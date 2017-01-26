package com.asus.zenheart.smilemirror;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


public class SmileIndicatorView extends View implements View.OnTouchListener {
    private static final String LOG_TAG = "SmileIndicator";

    private static final int ANGLE_DEGREE_MAX = 135;
    private static final int ANGLE_DEGREE_MIN = 45;
    private static final int DEGREE_OF_QUADRANT = 90;
    private static final float RECIPROCAL_SQUARE_ROOT_OF_TWO = 0.707f;
    private float mCircleRadius;

    private Bitmap mBitmap;
    private int mCirclePositionX;
    private int mCirclePositionY;
    //it will be used in the future
    /*unused*/
    private int mXPositionDown;
    private int mXMove;
    private int mXScroll;

    public SmileIndicatorView(Context context) {
        super(context);
        initSmileIndicator(context);
    }

    public SmileIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSmileIndicator(context);
    }

    public SmileIndicatorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSmileIndicator(context);
    }

    private void initSmileIndicator(Context context) {
        mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.circle_solid);
        setBackgroundResource(R.drawable.curve);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap == null) {
            Log.e(LOG_TAG, "bitmap is null, which is not expected !!!");
            return;
        }
        if (mCirclePositionX == 0 || mCirclePositionY == 0) {
            calculateCirclePosition(0, getWidth());
        }
        canvas.drawBitmap(mBitmap, mCirclePositionX - mBitmap.getWidth() / 2,
                mCirclePositionY - mBitmap.getHeight() / 2, null);
    }

    //it will be used in the future
    /*unused*/
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mXPositionDown = (int) event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                mXMove = (int) event.getX();
                mXScroll = mXMove - mXPositionDown;
                final int width = v.getWidth();
                int percent = covertPositionToPercent(width, mXScroll);
                calculateCirclePosition(percent, width);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return false;
    }

    //it will be used in the future
    /*unused*/
    private int covertPositionToPercent(int parentWidth, int position) {
        int percent = 100 * position / parentWidth;
        //for backward direction
        if (percent < 0) {
            percent = percent + 100;
        }
        return percent;
    }

    /**
     * calculate  position with the circle formula ,which is centered at (k,h) with radius r
     * x = r*cos()+k , k = viewWidth/2
     * y = r*sin()+h , h = viewWidth/2
     *
     * @param anglePercent valued from 0% to 100%
     * @param viewWidth    the width of the view
     */
    private void calculateCirclePosition(int anglePercent, int viewWidth) {
        final double radian = angleToRadian(
                ANGLE_DEGREE_MAX - DEGREE_OF_QUADRANT * anglePercent / 100);
        final int bitmapWidth = mBitmap.getWidth();
        final int shift = bitmapWidth / 2;
        //to fix the actual width
        viewWidth = viewWidth - bitmapWidth;
        final int halfViewWidth = viewWidth / 2;

        final int circleCenterX = halfViewWidth;
        final int circleCenterY = -halfViewWidth;
        mCircleRadius = viewWidth * RECIPROCAL_SQUARE_ROOT_OF_TWO;

        mCirclePositionX = (int) (mCircleRadius * Math.cos(radian)) + circleCenterX + shift;
        mCirclePositionY = (int) (mCircleRadius * Math.sin(radian)) + circleCenterY + shift;
    }

    /**
     * @param angle should value from degrees of 135 to 45 corresponding to anglePercent 0% to 100%
     * @return radian value
     */
    private double angleToRadian(int angle) {
        if (angle > ANGLE_DEGREE_MAX) {
            angle = ANGLE_DEGREE_MAX;
        } else if (angle < ANGLE_DEGREE_MIN) {
            angle = ANGLE_DEGREE_MIN;
        }
        return Math.toRadians((angle));
    }


    /**
     * to control the position of the circle
     * this method may be called outsides or onTouchEvent
     *
     * @param anglePercent valued from 0%~100%
     */
    public void setCirclePosition(int anglePercent) {
        calculateCirclePosition(anglePercent, getWidth());
        invalidate();
    }

}
