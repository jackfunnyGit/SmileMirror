package com.asus.zenheart.smilemirror.GUIView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.Util.PrefsUtils;

import java.util.Locale;

public class CounterView extends TextView {
    private static final String LOG_TAG = "CounterView";
    private static final int DELAY_TIME_MILLS = 1000;
    private static final int INDEX_COMPOUND_START = 0;

    private boolean mIsCounting;
    private boolean mIsRecording;
    private long mCountingTime;
    private Drawable mInnerDrawable;
    private SpaceCompoundDrawable mSpaceDrawable;

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
        mInnerDrawable = getCompoundDrawablesRelative()[INDEX_COMPOUND_START];
        if (mInnerDrawable == null) {
            Log.w(LOG_TAG, "drawable is null");
        }
        mSpaceDrawable = new SpaceCompoundDrawable(mInnerDrawable);
        setText(getTimeText());
    }

    public String getTimeText() {
        final int sec = (int) mCountingTime % 60;//sec per min
        final int min = (int) mCountingTime / 60 % 60;//sec per hour
        final int hour = (int) mCountingTime / 3600 % 24;//sec per day
        return hour > 0
                ? String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec)
                : String.format(Locale.US, "%02d:%02d", min, sec);

    }

    public void starCount() {
        mIsRecording = PrefsUtils.getBooleanPreference(getContext(),
                PrefsUtils.PREFS_AUTO_RECORDING, true);
        if (!mIsCounting) {
            Log.d(LOG_TAG, "startCount");
            mIsCounting = true;
            mCountingTime = 0;
            setText(getTimeText());
            setCompoundDrawables(mSpaceDrawable, null, null, null);
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
            if (mIsRecording) {
                if (mCountingTime % 2 == 0) {
                    setCompoundDrawables(mSpaceDrawable, null, null, null);
                } else {
                    setCompoundDrawables(mInnerDrawable, null, null, null);
                }
            }
            setText(getTimeText());
            postDelayed(this, DELAY_TIME_MILLS);
        }
    }

    /**
     * This drawable class is used for draw "empty",whose bounds are corresponding to inner drawable
     */
    private static class SpaceCompoundDrawable extends Drawable {

        // inner Drawable
        private final Drawable mDrawable;

        public SpaceCompoundDrawable(Drawable drawable) {
            mDrawable = drawable;
            setBounds(0, 0, getIntrinsicWidth(), getIntrinsicHeight());
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
            //do nothing because it is a space drawable
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
