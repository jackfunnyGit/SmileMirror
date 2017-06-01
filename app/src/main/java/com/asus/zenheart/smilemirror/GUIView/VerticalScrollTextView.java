package com.asus.zenheart.smilemirror.GUIView;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.Util.AnimationUtil;
import com.asus.zenheart.smilemirror.Util.LogUtils;
import com.asus.zenheart.smilemirror.Util.PrefsUtils;

public class VerticalScrollTextView extends ScrollView implements View.OnTouchListener {

    private static final String LOG_TAG = "VerticalScrollTextView";

    /**
     * Scrolling speed constant concerned with five speed level
     */
    public @interface TEXT_SPEED {
        int LAZY = 5;
        int SLOW = 4;
        int NORMAL = 3;
        int FAST = 2;
        int TURBO = 1;
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

    /**
     * Used to control teleprompter scrolling speed
     */
    private static final int SCROLLING_PX_UNIT = 20;
    private static final int TEXT_INTERVAL_TIME_MILL = 80;
    private static final int TEXT_LINES = 100;
    private static final float LINE_SPACING_MULTIPLIER = 1.4f;

    private Context mContext;
    private TextView mTextView;
    private Runnable mRunnable;
    /**
     * True when the height of the textSpeech is shorter than the VerticalTextView
     */
    private boolean mNeedScrolling;
    private boolean mIsScrolling;
    private boolean mRepeating;

    private int mScrollingSpeed = 1;
    private int mScrollingShift;
    private int mSpeechHeight;
    private float mHeight;
    private ObjectAnimator mObjectAnimator;
    private AnimatorListener mListener = new AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            LogUtils.v(LOG_TAG, "onAnimationStart ");
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            LogUtils.v(LOG_TAG, "onAnimationEnd");
            if (!mIsScrolling) {
                return;
            }
            post(new ScrollRepeatRunnable());
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            LogUtils.v(LOG_TAG, "onAnimationCancel");
        }

        @Override
        public synchronized void onAnimationRepeat(Animator animation) {
            LogUtils.v(LOG_TAG, "onAnimationRepeat ");

        }
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
        mContext = context;
        //only initialize mObjectAnimator here because mSpeechHeight may return 0
        mTextView = initTextView();
        mObjectAnimator = AnimationUtil.getObjectAnimator(mTextView, "scrollY", 0, 0, 1, 0);
        updateTextPref();
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
        checkIfNeedScrolling();
    }

    private TextView initTextView() {
        TextView textView = new TextView(mContext);
        textView.setLineSpacing(0, LINE_SPACING_MULTIPLIER);
        textView.setTextColor(Color.WHITE);
        textView.setLines(TEXT_LINES);
        addView(textView);
        return textView;
    }

    /**
     * getLineCount may return 0 when the view is initialized. Therefore,only checkIfNeedScrolling
     * in {@link #onMeasure(int, int)}.Watch out it will scrolling if repeatMode is set true
     *
     * @see #setRepeatMode(boolean)
     */
    private void checkIfNeedScrolling() {
        if (mTextView == null) {
            return;
        }
        LogUtils.d(LOG_TAG, "ViewHeight = " + mHeight);
        LogUtils.d(LOG_TAG, "LineHeight() = " + mTextView.getLineHeight());
        LogUtils.d(LOG_TAG, "LineCount() = " + mTextView.getLineCount());
        mSpeechHeight = mTextView.getLineHeight() * mTextView.getLineCount();
        mScrollingShift = (int) mHeight - mSpeechHeight;
        mNeedScrolling = mScrollingShift < 0;
        mScrollingShift = Math.abs(mScrollingShift);
    }

    private Runnable createRunnable() {
        if (mRepeating) {
            return new ScrollToBottomRunnable();
        } else if (mNeedScrolling) {
            return new ScrollToLastRunnable();
        } else {
            LogUtils.w(LOG_TAG, "createRunnable returning false is unexpected");
            return null;
        }
    }

    /**
     * Control the text scrolling speed
     *
     * @param textSpeed The text scrolling speed,supposed to fed by values in {@link TEXT_SPEED}
     */
    public void setTextScrollSpeed(@TEXT_SPEED int textSpeed) {
        if (textSpeed < 0) {
            textSpeed = TEXT_SPEED.NORMAL;
        }
        mScrollingSpeed = textSpeed * SCROLLING_PX_UNIT;
    }

    /**
     * Customized textView does not need to call super method
     * This TextView will never change toast_text size with the system toast_text setting
     *
     * @param size The textSize measured in SP
     */

    public void setTextSize(float size) {
        if (size < 0) {
            size = TEXT_SIZE.NORMAL;
        }
        mTextView.setTextSize(size);
    }

    public void setText(CharSequence text) {
        mTextView.setText(text);
    }

    /**
     * Read textSize and textSpeed from the preference
     */
    public void updateTextPref() {
        setTextScrollSpeed(PrefsUtils.getIntegerPreference(
                mContext, PrefsUtils.PREFS_SPEECH_SCROLLING_SPEED, TEXT_SPEED.NORMAL));
        setTextSize(PrefsUtils.getFloatPreference(
                mContext, PrefsUtils.PREFS_SPEECH_TEXT_SIZE, TEXT_SIZE.NORMAL));
    }

    /**
     * start toast_text auto scrolled after interval time
     */
    public void start() {
        start(TEXT_INTERVAL_TIME_MILL);
    }

    /**
     * start toast_text auto scrolled after delay milli seconds. {@link #mRunnable} is only created once
     * because scrolling strategy will not change once mRunnable is created
     */
    public void start(int delayMillis) {
        LogUtils.d(LOG_TAG,
                "start... needScrolling is " + mNeedScrolling + " repeatMode is " + mRepeating);
        if (!mIsScrolling && (mRepeating || mNeedScrolling)) {
            mIsScrolling = true;
            LogUtils.d(LOG_TAG, "Runnable = " + mRunnable);
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
        mIsScrolling = false;
        if (mObjectAnimator != null) {
            mObjectAnimator.cancel();
            // Reset position to 0 immediately and do it only once
            mObjectAnimator.setIntValues(0);
            mObjectAnimator.setDuration(0);
            mObjectAnimator.setRepeatCount(0);
            mObjectAnimator.removeAllListeners();
            mObjectAnimator.start();
        }
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //do nothing to disable scrolling by touching
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return true;
    }

    private class ScrollRepeatRunnable implements Runnable {
        @Override
        public void run() {
            final int height = getHeight();
            final int scrollingHeight = mSpeechHeight + height;
            mObjectAnimator.setIntValues(-height, mSpeechHeight);
            mObjectAnimator.setDuration(mScrollingSpeed * scrollingHeight);
            mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mObjectAnimator.removeListener(mListener);
            mObjectAnimator.start();
        }
    }

    private class ScrollToBottomRunnable implements Runnable {
        @Override
        public void run() {
            mObjectAnimator.setIntValues(0, mSpeechHeight);
            mObjectAnimator.setDuration(mScrollingSpeed * mSpeechHeight);
            mObjectAnimator.setRepeatCount(0);
            mObjectAnimator.addListener(mListener);
            mObjectAnimator.start();
        }
    }

    /**
     * Stop scrolling as soon as the last text shows up
     */
    private class ScrollToLastRunnable implements Runnable {
        @Override
        public void run() {
            mObjectAnimator.setIntValues(0, mScrollingShift);
            mObjectAnimator.setDuration(mScrollingSpeed * mScrollingShift);
            mObjectAnimator.start();
        }
    }

}

