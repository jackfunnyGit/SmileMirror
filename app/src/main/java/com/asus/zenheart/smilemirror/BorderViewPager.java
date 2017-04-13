package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BorderViewPager extends ViewPager {
    private boolean mSwipeEnabled;
    private OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //always return false because touch event is expected to dispatch to viewPager
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setBorderLineEnabled(true);
                    return false;
                case MotionEvent.ACTION_MOVE:
                    return false;
                case MotionEvent.ACTION_UP:
                    setBorderLineEnabled(false);
                    return false;
            }
            return false;
        }
    };

    public BorderViewPager(Context context) {
        super(context);
        initBorderViewPager();
    }

    public BorderViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBorderViewPager();
    }

    private void initBorderViewPager() {
        mSwipeEnabled = true;
        setOnTouchListener(mOnTouchListener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mSwipeEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mSwipeEnabled && super.onTouchEvent(ev);
    }

    /**
     * Draw the borderLine of each childView
     *
     * @param enabled Flag to draw borderLine or not
     */
    private void setBorderLineEnabled(boolean enabled) {

        if (enabled) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).findViewById(R.id.border_line)
                        .setBackgroundResource(R.drawable.border_line_background);
            }
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).findViewById(R.id.border_line).setBackground(null);
            }
        }

    }

    /**
     * Enable/Disable ViewPager to scroll
     *
     * @param enabled Flag to scroll viewPager
     */
    public void setSwipeEnabled(boolean enabled) {
        setOnTouchListener(enabled ? mOnTouchListener : null);
        mSwipeEnabled = enabled;
    }
}
