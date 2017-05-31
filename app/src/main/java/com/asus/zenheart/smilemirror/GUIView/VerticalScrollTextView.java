package com.asus.zenheart.smilemirror.GUIView;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.Util.LogUtils;
import com.asus.zenheart.smilemirror.Util.PrefsUtils;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerticalScrollTextView extends TextView implements View.OnTouchListener {

    /**
     * Scrolling speed constant concerned with five speed level
     */
    public @interface TEXT_SPEED {
        int LAZY = 1;
        int SLOW = 2;
        int NORMAL = 3;
        int FAST = 4;
        int TURBO = 5;
    }

    /**
     * Three options of size measured in sp to set text size
     */

    public @interface TEXT_SIZE {
        float SMALL = 13;
        float NORMAL = 15;
        float BIG = 17;
    }

    /**
     * Used for the first postDelay time
     *
     * @see #start()
     * @see #start(int)
     */
    public static final int FIRST_DELAY_TIME_MILLS = 5000;

    private static final String LOG_TAG = "VerticalScrollTextView";
    private static final float STROKE_WIDTH = 1f;//for bottom line painting

    /**
     * Used to control teleprompter scrolling speed
     */
    private static final float TEXT_ROW_SPACE = 1.2f;
    private static final int TEXT_INTERVAL_TIME_MILL = 80;
    private int mTextStep = 1;//scrolled toast_text speed

    private Context mContext;
    private Paint mPaint;
    private DisplayMetrics mDisplayMetrics;
    private int mPaddingShift;
    private String mText;
    private int mFontHeight;
    private int mFontBottomHeight;
    private float mWidth;
    private float mHeight;
    private List<String> mTextList = new ArrayList<>();
    private AnimRunnable mRunnable;
    /**
     * True when the height of the textSpeech is shorter than the VerticalTextView
     */
    private boolean mNeedScrolling;
    private boolean mIsScrolling;
    private boolean mRepeating;
    private float mFirstLineY;

    public VerticalScrollTextView(Context context) {
        super(context);
        initVerticalScrollTextView(context);
    }

    public VerticalScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVerticalScrollTextView(context);
    }

    private void initVerticalScrollTextView(Context context) {
        mContext = context;
        final Resources resources = mContext.getResources();
        mDisplayMetrics = resources.getDisplayMetrics();
        mPaddingShift = (int) resources.getDimension(R.dimen.sm_vertical_text_view_width_padding);
        mPaint = getPaint();
        mText = (String) getText();
        updateTextStyle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ScrollLayout only can run at EXACTLY mode !");
        }
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mWidth = mWidth - mPaddingShift * 2;//fix viewWidth to actual toast_text row width
        initTextList();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTextList.size() == 0) {
            Log.i(LOG_TAG, "toast_text content is empty");
            return;
        }
        for (int i = 0; i < mTextList.size(); i++) {
            float y = mFirstLineY + TEXT_ROW_SPACE * i * mFontHeight;
            canvas.drawText(mTextList.get(i), mPaddingShift, y, mPaint);
        }
        //draw a line at the bottom of the view
        canvas.drawLine(mPaddingShift, mHeight - 1, mWidth + mPaddingShift, mHeight - 1, mPaint);

    }

    /**
     * Control the text scrolling speed
     *
     * @param textSpeed The text scrolling speed,supposed to fed by values in {@link TEXT_SPEED}
     */
    public void setTextScrollSpeed(int textSpeed) {
        if (textSpeed < 0) {
            textSpeed = TEXT_SPEED.NORMAL;
        }
        mTextStep = textSpeed;
    }

    /**
     * Customized textView does not need to call super method
     * This TextView will never change toast_text size with the system toast_text setting
     *
     * @param size The textSize measured in SP
     */
    @Override
    public void setTextSize(float size) {
        if (size < 0) {
            size = TEXT_SIZE.NORMAL;
        }
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size,
                mDisplayMetrics);
        mPaint.setTextSize(px);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mFontHeight = getFontHeight(mPaint);
        mFontBottomHeight = getFontBottomHeight(mPaint);
        mFirstLineY = mFontHeight * TEXT_ROW_SPACE;
        initTextList();
        invalidate();
    }

    /**
     * Sets the string value of the TextView
     *
     * @param text The new text to place in the text view.
     */
    public void setText(@NonNull String text) {
        mText = text;
        initTextList();
        super.setText(text);
    }

    /**
     * Read textSize and textSpeed from the preference
     */
    public void updateTextStyle() {
        setTextScrollSpeed(PrefsUtils.getIntegerPreference(
                mContext, PrefsUtils.PREFS_SPEECH_SCROLLING_SPEED, TEXT_SPEED.NORMAL));
        setTextSize(PrefsUtils.getFloatPreference(
                mContext, PrefsUtils.PREFS_SPEECH_TEXT_SIZE, TEXT_SIZE.NORMAL));
    }

    /**
     * Load textContent by {@link LoadContentTask}
     */
    public void loadContentToView(long contentId) {
        final LoadContentTask task = new LoadContentTask(this, contentId);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * split toast_text content into String List to init {@link #mTextList}
     */
    private void initTextList() {
        String newText = autoSplitText(mText, mWidth, mPaint);
        if (!TextUtils.isEmpty(newText)) {
            stringToList(newText, mTextList);
            checkIfNeedScrolling();
        }
    }

    /**
     * split toast_text in every width of this view measured by textSize and combine to a string
     *
     * @param text  toast_text content
     * @param width the width of toast_text row
     * @param paint to give the textSize
     * @return a string composed of @param toast_text  split with "\n" in every width of TextView
     * measured by @param paint textSize
     */
    private String autoSplitText(@NonNull String text, float width, @NonNull Paint paint) {
        if (width <= 0) {
            Log.w(LOG_TAG, "toast_text row width should not be less than or equal to zero !!!");
            return null;
        }
        //replace "\r" with "" and split by "\n"
        String[] rawTextLines = text.replaceAll("\r", "").split("\n");
        StringBuilder sbNewText = new StringBuilder();
        for (String rawTextLine : rawTextLines) {
            if (paint.measureText(rawTextLine) <= width) {
                //if the length of the content toast_text is shorter than the view width,do nothing
                sbNewText.append(rawTextLine);
            } else {
                //if the length of the content toast_text is longer than the view width,
                // the content toast_text is measured with word by word and
                // append "\n" before exceeding available width
                float lineWidth = 0;
                for (int cnt = 0; cnt != rawTextLine.length(); ++cnt) {
                    char ch = rawTextLine.charAt(cnt);
                    lineWidth += paint.measureText(String.valueOf(ch));
                    if (lineWidth <= width) {
                        sbNewText.append(ch);
                    } else {
                        sbNewText.append("\n");
                        lineWidth = 0;
                        cnt--;
                    }
                }
            }
            sbNewText.append("\n");
        }
        //erase extra "\n" in the end of sbNewText
        if (!text.endsWith("\n")) {
            sbNewText.deleteCharAt(sbNewText.length() - 1);
        }
        return sbNewText.toString();
    }

    /**
     * this method  should be called after {@link #autoSplitText}
     *
     * @param text     the processed toast_text content,supposed to be returned by autoSplitText
     * @param textList the textList to store the split toast_text content
     * @return a string list split from a string toast_text with "\n"
     */
    private List<String> stringToList(@NonNull String text, @NonNull List<String> textList) {
        String[] rawTextLines = text.split("\n");
        textList.clear();
        Collections.addAll(textList, rawTextLines);
        return textList;
    }

    private void checkIfNeedScrolling() {
        mNeedScrolling = mHeight < TEXT_ROW_SPACE * mTextList.size() * mFontHeight +
                getFontBottomHeight(mPaint);
    }

    private AnimRunnable createRunnable() {
        if (mRepeating) {
            return new ScrollRepeatRunnable();
        } else if (mNeedScrolling) {
            return new ScrollOnceRunnable();
        } else {
            LogUtils.w(LOG_TAG, "createRunnable returning false is unexpected");
            return null;
        }
    }

    /**
     * start toast_text auto scrolled after interval time
     */
    public void start() {
        start(TEXT_INTERVAL_TIME_MILL);
    }

    /**
     * start toast_text auto scrolled after delay milli seconds. mRunnable is only created once
     * because scrolling strategy will not change once mRunnable is created
     */
    public void start(int delayMillis) {
        if (!mIsScrolling && (mRepeating || mNeedScrolling)) {
            mIsScrolling = true;
            if (mRunnable == null) {
                mRunnable = createRunnable();
            }
            postDelayed(mRunnable, delayMillis);
        }
    }

    /**
     * stop toast_text auto scrolled. We don't have to check if mRunnable is null because
     * removeCallbacks has already checked
     *
     * @see #removeCallbacks(Runnable)
     */
    public void stop() {
        removeCallbacks(mRunnable);
        mFirstLineY = mFontHeight * TEXT_ROW_SPACE;
        mIsScrolling = false;
        invalidate();
    }

    /**
     * Set if repeating. If true,text will scrolling repeatedly.
     * Otherwise,text will scroll to the last line and stop
     *
     * @param repeat The repeat flag
     * @return This object, allowing calls to methods in this class to be chained.
     */
    public VerticalScrollTextView setRepeatMode(boolean repeat) {
        mRepeating = repeat;
        return this;
    }

    /*unused*/
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mIsScrolling) {
                    stop();
                } else {
                    start();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return true;
    }

    /**
     * To measure the height of a word
     *
     * @param paint the textSize
     * @return an integer corresponding to row space
     */
    private static int getFontHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.bottom - fm.top);
    }

    /**
     * To measure the bottom of a word
     *
     * @param paint the textSize
     * @return an integer corresponding to bottom height of the font
     */
    private static int getFontBottomHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.bottom);
    }

    private abstract class AnimRunnable implements Runnable {
        public abstract boolean scrollStrategy();

        @Override
        public void run() {
            mFirstLineY = mFirstLineY - mTextStep;
            if (scrollStrategy()) {
                return;
            }
            invalidate();
            postDelayed(this, TEXT_INTERVAL_TIME_MILL);
        }
    }

    private class ScrollRepeatRunnable extends AnimRunnable {

        @Override
        public boolean scrollStrategy() {
            if (mFirstLineY + TEXT_ROW_SPACE * (mTextList.size() - 1) * mFontHeight < 0) {
                //start from bottom
                mFirstLineY = mHeight;
            }
            return false;
        }
    }

    private class ScrollOnceRunnable extends AnimRunnable {
        @Override
        public boolean scrollStrategy() {
            return mFirstLineY + TEXT_ROW_SPACE * (mTextList.size() - 1) * mFontHeight +
                    mFontBottomHeight < mHeight;
        }
    }

    private static class LoadContentTask extends AsyncTask<Void, Void, String[]> {

        private static final String[] PROJECTION = new String[]{
                SpeechContract.CONTENT, SpeechContract.TITLE, SpeechContract._ID,
                SpeechContract.TYPE};

        private static final int INDEX_CONTENT = 0;
        private static final int INDEX_TITLE = 1;
        private static final int INDEX_ID = 2;
        private static final int INDEX_TYPE = 3;

        private final WeakReference<Context> mContextReference;
        private final WeakReference<VerticalScrollTextView> mTextViewReference;
        private final long mContentId;

        public LoadContentTask(@NonNull VerticalScrollTextView verticalScrollTextView, long id) {

            mContextReference = new WeakReference<>(verticalScrollTextView.getContext());
            mTextViewReference = new WeakReference<>(verticalScrollTextView);
            mContentId = id;
        }

        @Override
        @WorkerThread
        protected String[] doInBackground(Void... params) {
            final Context context = mContextReference.get();
            if (context == null) return null;

            ContentResolver resolver = context.getContentResolver();
            try (Cursor cursor = resolver
                    .query(SpeechContract.SPEECH_URI, PROJECTION, null, null, null)) {

                if (cursor == null) {
                    Log.w(LOG_TAG, "cursor is null,which is not expected error");
                    return null;
                }
                if (cursor.getCount() == 0) {
                    return new String[]{
                            context.getString(R.string.sm_speech_content_create_script), ""};
                }
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    if (mContentId == cursor.getInt(INDEX_ID)) {
                        int type = cursor.getInt(INDEX_TYPE);
                        if (type == 1) {
                            return new String[]{context.getString(
                                    R.string.editor_example_one_content),
                                    context.getString(R.string.editor_example_one_title)};
                        } else if (type == 2) {
                            return new String[]{context.getString(
                                    R.string.editor_example_two_content),
                                    context.getString(R.string.editor_example_two_title)};
                        } else if (type == 3) {
                            return new String[]{context.getString(
                                    R.string.editor_example_three_content),
                                    context.getString(R.string.editor_example_three_title)};
                        } else if (type == 4) {
                            return new String[]{context.getString(
                                    R.string.editor_example_four_content),
                                    context.getString(R.string.editor_example_four_title)};
                        } else {
                            return new String[]{cursor.getString(INDEX_CONTENT),
                                    cursor.getString(INDEX_TITLE)};
                        }
                    }
                }
                return new String[]{context.getString(
                        R.string.sm_speech_content_select_script), ""};
            }
        }

        @Override
        protected void onPostExecute(String[] content) {
            if (content == null) return;

            // Show loaded content on UI
            final VerticalScrollTextView scrollTextView = mTextViewReference.get();
            if (scrollTextView != null) {
                scrollTextView.setText(content[INDEX_CONTENT]);
                View viewParent = (View) scrollTextView.getParent();
                TextView titleView = (TextView) viewParent.findViewById(R.id.text_present_tittle);
                if (titleView != null) {
                    titleView.setText(content[INDEX_TITLE]);
                }
            }
            //TODO: Remove the task from active tasks set

        }
    }
}

