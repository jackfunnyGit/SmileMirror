package com.asus.zenheart.smilemirror.GUIView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.Nullable;
import android.util.AttributeSet;


public class ShiningImageView extends android.support.v7.widget.AppCompatImageView {
    private static final int[] sShiningEffectColor = new int[]{
            0x01FFFFFF, Color.WHITE, 0x01FFFFFF};

    private boolean mShiningEffectRunning = false;
    private ValueAnimator mShiningAnimator;
    private Bitmap mShiningShape;
    private Paint mShiningPaint;

    public ShiningImageView(Context context) {
        super(context);
    }

    public ShiningImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ShiningImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mShiningEffectRunning) {
            canvas.drawBitmap(mShiningShape, 0, 0, mShiningPaint);
        }
    }

    public void playShiningEffect(long duration) {
        if (mShiningEffectRunning) {
            return;
        }
        prepareShiningEffect();
        startShiningEffect(duration);
        releaseShiningEffect(duration);
    }

    private void prepareShiningEffect() {
        mShiningEffectRunning = true;
        mShiningPaint = new Paint();
        Bitmap temp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mShiningShape = temp.extractAlpha();
        temp.recycle();
        Canvas canvas = new Canvas(mShiningShape);
        draw(canvas);
        canvas.setBitmap(null);
        invalidate();
    }

    private void startShiningEffect(long duration) {
        mShiningAnimator = ValueAnimator.ofFloat(0.0f, 1.4f);
        mShiningAnimator.setDuration(duration);
        mShiningAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float) updatedAnimation.getAnimatedValue();
                int size = Math.max(getWidth(), getHeight());
                LinearGradient gradient = new LinearGradient(0, 0, size, size, sShiningEffectColor,
                        new float[]{animatedValue - 0.4f, animatedValue, animatedValue},
                        Shader.TileMode.CLAMP);
                if (mShiningPaint == null) {
                    mShiningPaint = new Paint();
                }
                mShiningPaint.setShader(gradient);
                invalidate();
            }
        });
        mShiningAnimator.start();
    }

    private void releaseShiningEffect(long duration) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mShiningEffectRunning = false;
                mShiningPaint = null;
                mShiningShape = null;
                mShiningAnimator.removeAllListeners();
                invalidate();
            }
        }, duration);
    }
}
