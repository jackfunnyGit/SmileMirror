package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerticalScrollTextView extends TextView implements View.OnTouchListener {
    /**
     * Scrolling speed constant concern with four speed level ,slow,normal,fast and superFast
     */
    public interface TEXT_SPEED {
        int SLOW = 1;
        int NORMAL = 5;
        int FAST = 10;
        int TURBO = 20;
    }

    /**
     * Three options of size measured in dp to set text size
     */
    public interface TEXT_SIZE {
        float SMALL = 16;
        float DEFAULT = 18;
        float BIG = 22;
    }

    /**
     * Used for the first postDelay time
     * @see #start()
     * @see #start(int)
     */
    public static final int FIRST_DELAY_TIME_MILLS = 5000;

    //TODO remove all the log in the future
    private static final String LOG_TAG = "VerticalScrollTextView";
    private static final boolean LOG_FLAG = true;
    private static final float STROKE_WIDTH = 1f;//for bottom line painting

    /**
     * Used to control teleprompter scrolling speed
     */
    private static float TEXT_ROW_SPACE = 1.2f;
    private static final int TEXT_INTERVAL_TIME_MILL = 40;
    private int mTextStep = 1;//scrolled toast_text speed

    private Paint mPaint;
    private DisplayMetrics mDisplayMetrics;
    private int mPaddingShift;
    private String mText;
    private int mFontHeight;
    private float mWidth;
    private float mHeight;
    private List<String> mTextList = new ArrayList<>();
    private AnimRunnable mRunnable = new AnimRunnable();

    private boolean mIsScrolling;
    private float mFirstLineY;
    //TODO above will be delete in the future, it is for demo
    private static int I;
    private static final float TextSize[] = new float[]{
            TEXT_SIZE.BIG, TEXT_SIZE.DEFAULT, TEXT_SIZE.SMALL
    };

    public VerticalScrollTextView(Context context) {
        super(context);
        initVerticalScrollTextView(context);
    }

    public VerticalScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVerticalScrollTextView(context);
    }

    private void initVerticalScrollTextView(Context context) {
        final Resources resources = context.getResources();
        mDisplayMetrics = resources.getDisplayMetrics();
        mPaddingShift = (int) resources.getDimension(R.dimen.vertical_text_view_width_padding);
        mPaint = getPaint();
        //TODO: read textContent,textSize,textSpeed from the preference
        mText = (String) getText();
        setTextSize(TEXT_SIZE.SMALL);
        //TODO: delete in the future, it is for demo
        setOnTouchListener(this);
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
        Log.i(LOG_TAG, "onMeasure mHeight = " + mHeight + " mWidth = " + mWidth);
        initTextList();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTextList.size() == 0) {
            Log.w(LOG_TAG, "toast_text content should not be empty");
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
            textSpeed = TEXT_SPEED.SLOW;
        }
        mTextStep = textSpeed;
    }

    /**
     * Customized textView does not need to call super method
     * This TextView will never change toast_text size with the system toast_text setting
     *
     * @param size the textSize measured in DP instead of SP
     */
    @Override
    public void setTextSize(float size) {
        final float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size,
                mDisplayMetrics);
        mPaint.setTextSize(px);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mFontHeight = getFontHeight(mPaint);
        mFirstLineY = mFontHeight;
        initTextList();
        invalidate();
    }

    /**
     * Sets the string value of the TextView
     *
     * @param text The new text to place in the text view.
     */
    public void setText(String text) {
        mText = text;
        initTextList();
        super.setText(text);
    }

    public void setTextRowSpace(float rowSpace) {
        TEXT_ROW_SPACE = rowSpace;
        invalidate();
    }

    /**
     * split toast_text content into String List to init {@link #mTextList}
     */
    private void initTextList() {
        String newText = autoSplitText(mText, mWidth, mPaint);
        if (!TextUtils.isEmpty(newText)) {
            stringToList(newText, mTextList);
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
            Log.e(LOG_TAG, "toast_text row width should not be less than or equal to zero !!!");
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

    /**
     * start toast_text auto scrolled after interval time
     */
    public void start() {
        if (!mIsScrolling) {
            mIsScrolling = true;
            postDelayed(mRunnable, TEXT_INTERVAL_TIME_MILL);
        }
    }

    /**
     * start toast_text auto scrolled after delay milli seconds
     *
     */
    public void start(int delayMillis) {
        if (!mIsScrolling) {
            mIsScrolling = true;
            postDelayed(mRunnable, delayMillis);
        }
    }

    /**
     * stop toast_text auto scrolled
     */
    public void stop() {
        removeCallbacks(mRunnable);
        mFirstLineY = mFontHeight;
        mIsScrolling = false;
        invalidate();
    }

    /*unused*/
    //TODO do not delete until you figure out the textSize,it will remove in the future
    private void showValue(Paint.FontMetrics fm) {
        if (LOG_FLAG)
            Log.i(LOG_TAG, "ascent = " + fm.ascent + " descent " + fm.descent);
        if (LOG_FLAG)
            Log.i(LOG_TAG, "top = " + fm.top + " bottom =  " + fm.bottom);
        if (LOG_FLAG)
            Log.i(LOG_TAG, "leading = " + fm.leading);// always be zero
    }

    /*unused*/
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e(LOG_TAG, "is Start = " + mIsScrolling);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                if (mIsScrolling) {
//                    stop();
//                } else {
//                    start();
//                }
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                setTextSize(TextSize[I]);
                I++;
                if (I > 2) {
                    I = I % 3;
                }

                return true;
        }
        return true;
    }

    /**
     * to measure the height of a word with this method
     *
     * @param paint the textSize
     * @return an integer corresponding to row space
     */
    private static int getFontHeight(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        Log.i(LOG_TAG, "text height = " + (int) Math.ceil(fm.bottom - fm.top));
        return (int) Math.ceil(fm.bottom - fm.top);
    }

    private class AnimRunnable implements Runnable {
        @Override
        public void run() {
            mFirstLineY = mFirstLineY - mTextStep;
            if ((mFirstLineY + TEXT_ROW_SPACE * mTextList.size() * mFontHeight) < 0) {
                //start from bottom
                mFirstLineY = mHeight;
            }
            invalidate();
            postDelayed(this, TEXT_INTERVAL_TIME_MILL);
        }
    }
}

