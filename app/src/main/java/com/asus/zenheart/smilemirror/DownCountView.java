package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class DownCountView extends View {

    private static final String LOG_TAG = "DownCountView";

    private static final int COUNTDOWN_TIME_MILLISECOND = 3000;
    private static final int COUNTDOWN_INTERVAL_MILLISECOND = 1000;
    private static final int COUNTDOWN_NUMBER = COUNTDOWN_TIME_MILLISECOND / COUNTDOWN_INTERVAL_MILLISECOND;

    private Context mContext;
    private int mCount;
    private Paint mPaint;
    private Rect mBound;

    private OnFinishedLister mOnFinishedLister;
    private CountDownRunnable mRunnable = new CountDownRunnable();

    public DownCountView(Context context) {
        super(context);
        initDownCountView(context);
    }

    public DownCountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDownCountView(context);
    }

    public DownCountView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initDownCountView(context);
    }

    private void initDownCountView(Context context) {
        mContext = context;
        mPaint = new Paint();
        mPaint.setTextSize(mContext.getResources().getDimension(R.dimen.countdown_text_size));
        mPaint.setColor(mContext.getColor(R.color.countdown_text_color));
        mPaint.setTextAlign(Paint.Align.CENTER);
        mCount = COUNTDOWN_NUMBER;
        mBound = new Rect();
        setBackgroundResource(R.drawable.circle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        String text = String.format("%d", mCount);
        mPaint.getTextBounds(text, 0, text.length(), mBound);
        canvas.drawText(text, getWidth() / 2, getHeight() / 2 + mBound.height() / 2, mPaint);
    }

    public void startCount() {
        for (int i = 0; i < COUNTDOWN_TIME_MILLISECOND / COUNTDOWN_INTERVAL_MILLISECOND; i++) {
            postDelayed(mRunnable, (i + 1) * COUNTDOWN_INTERVAL_MILLISECOND);
        }

    }

    public void stopCount() {
        mCount = COUNTDOWN_NUMBER;
        removeCallbacks(mRunnable);
    }

    /**
     * Register a callback to be invoked when a downCount time is up.
     *
     * @param listener the listener to attach to this view
     */
    public void setOnFinishedListener(OnFinishedLister listener) {
        mOnFinishedLister = listener;
    }

    /**
     * Interface definition for a callback to be invoked when a downCount time is up
     */
    public interface OnFinishedLister {
        /**
         * Called when the downCount time is up
         */
        void onFinished();
    }

    private class CountDownRunnable implements Runnable {
        @Override
        public void run() {
            mCount--;
            invalidate();
            if (mCount <= 0) {
                stopCount();
                if (mOnFinishedLister != null) {
                    mOnFinishedLister.onFinished();
                }
            }

        }
    }

}

