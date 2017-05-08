package com.asus.zenheart.smilemirror.GUIView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
        final ValueAnimator shiningAnimator = ValueAnimator.ofFloat(0.0f, 1.4f);
        shiningAnimator.setDuration(duration);
        shiningAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float) updatedAnimation.getAnimatedValue();
                int size = Math.max(getWidth(), getHeight());
                LinearGradient gradient = new LinearGradient(0, 0, size, size, sShiningEffectColor,
                        new float[]{animatedValue - 0.4f, animatedValue, animatedValue},
                        Shader.TileMode.CLAMP);
                mShiningPaint.setShader(gradient);
                invalidate();
            }
        });
        shiningAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mShiningEffectRunning = true;
                mShiningPaint = new Paint();
                mShiningShape = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config
                        .ARGB_8888).extractAlpha();
                Canvas canvas = new Canvas(mShiningShape);
                draw(canvas);
                canvas.setBitmap(null);
                invalidate();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mShiningEffectRunning = false;
                mShiningPaint = null;
                mShiningShape = null;
                shiningAnimator.removeAllListeners();
                invalidate();
            }
        });
        shiningAnimator.start();
        invalidate();
    }
}
