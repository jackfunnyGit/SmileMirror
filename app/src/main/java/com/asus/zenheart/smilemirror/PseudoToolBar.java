package com.asus.zenheart.smilemirror;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class PseudoToolBar extends RelativeLayout {
    private boolean mInterceptEnable;

    public PseudoToolBar(Context context) {
        super(context);
    }

    public PseudoToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInterceptTouchEvent(boolean enabled) {
        mInterceptEnable = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mInterceptEnable || super.onInterceptTouchEvent(ev);
    }
}
