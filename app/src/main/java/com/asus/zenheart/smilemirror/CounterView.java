package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class CounterView extends TextView {
    private static final String LOG_TAG = "CounterView";
    private static final int DELAY_TIME_MILLS = 1000;
    private static final int INDEX_COMPOUND_TOP = 1;

    private boolean mIsCounting;
    private long mCountingTime;
    private GravityCompoundDrawable mGravityDrawable;

    private CountingRunnable mRunnable = new CountingRunnable();

    public CounterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCounterView();
    }

    public CounterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCounterView();
    }

    public CounterView(Context context) {
        super(context);
        initCounterView();
    }

    private void initCounterView() {
        mCountingTime = 0;
        final Drawable innerDrawable = getCompoundDrawables()[INDEX_COMPOUND_TOP];
        if (innerDrawable == null) {
            Log.w(LOG_TAG, "drawable is null");
            return;
        }
        mGravityDrawable = alignDrawableToLeftTop(innerDrawable);
        setCompoundDrawables(null, mGravityDrawable, null, null);
        setText(getTimeText());
    }

    private GravityCompoundDrawable alignDrawableToLeftTop(Drawable innerDrawable) {
        GravityCompoundDrawable gravityDrawable = new GravityCompoundDrawable(innerDrawable);
        innerDrawable.setBounds(0, 0, innerDrawable.getIntrinsicWidth(),
                innerDrawable.getIntrinsicHeight());
        gravityDrawable.setBounds(0, 0, innerDrawable.getIntrinsicWidth(),
                innerDrawable.getIntrinsicHeight());
        return gravityDrawable;
    }

    private String getTimeText() {
        final int sec = (int) mCountingTime % 60;//sec per min
        final int min = (int) mCountingTime / 60 % 60;//sec per hour
        final int hour = (int) mCountingTime / 3600 % 24;//sec per day
        String numberText = String.format("%02d:%02d:%02d", hour, min, sec);
        return numberText;
    }

    public void starCount() {
        if (!mIsCounting) {
            mIsCounting = true;
            mCountingTime = 0;
            setText(getTimeText());
            postDelayed(mRunnable, DELAY_TIME_MILLS);
        }
    }

    public void stopCount() {
        Log.d(LOG_TAG, "stopCount");
        mIsCounting = false;
        removeCallbacks(mRunnable);
    }

    private class CountingRunnable implements Runnable {
        @Override
        public void run() {
            mCountingTime++;
            if (mCountingTime % 2 == 0) {
                setCompoundDrawables(null, null, null, null);
            } else {
                setCompoundDrawables(null, mGravityDrawable, null, null);
            }
            setText(getTimeText());
            postDelayed(this, DELAY_TIME_MILLS);
        }
    }

    private static class GravityCompoundDrawable extends Drawable {

        // inner Drawable
        private final Drawable mDrawable;

        public GravityCompoundDrawable(Drawable drawable) {
            mDrawable = drawable;
        }

        @Override
        public int getIntrinsicWidth() {
            return mDrawable.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mDrawable.getIntrinsicHeight();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int halfCanvas = canvas.getWidth() / 2;
            int halfDrawable = mDrawable.getIntrinsicWidth() / 2;

            // align to left
            canvas.save();
            canvas.translate(-halfCanvas + halfDrawable, 0);
            mDrawable.draw(canvas);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }
}
