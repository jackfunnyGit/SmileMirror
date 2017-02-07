package com.asus.zenheart.smilemirror.Util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

//TODO rename in the future, factory should not be used like this
public class AnimationUtil {
    private static final String LOG_TAG = "AnimationUtil";
    private static final int TOAST_ANIMATION_TIME_MILL = 2000;
    private static final int TOAST_REPEATED_TIME = 1;
    private static final int BLINK_ANIMATION_TIME_MILL = 500;

    private static Animation toastFactory(@NonNull final View callbackView) {
        return toastFactory(callbackView, TOAST_ANIMATION_TIME_MILL);
    }

    private static Animation toastFactory(@NonNull final View callbackView, int toastShowTime) {
        final Animation toastAnimation = new AlphaAnimation(1, 0);
        toastAnimation.setDuration(toastShowTime);
        toastAnimation.setRepeatMode(TOAST_REPEATED_TIME);
        toastAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (callbackView == null) {
                    return;
                }
                callbackView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        return toastAnimation;
    }

    public static void toastAnimation(View targetView) {
        final Animation toastAnimation = AnimationUtil.toastFactory(targetView);
        targetView.setAnimation(toastAnimation);
        targetView.getAnimation().start();
    }

    public static Animation blinkFactory() {
        return blinkFactory(BLINK_ANIMATION_TIME_MILL);
    }

    private static Animation blinkFactory(int intervalTime) {
        // Change alpha from fully visible to invisible
        final Animation blinkAnimation = new AlphaAnimation(1, 0);
        // duration - half a second
        blinkAnimation.setDuration(intervalTime);
        // do not alter animation rate
        blinkAnimation.setInterpolator(new LinearInterpolator());
        // Repeat animation infinitely
        blinkAnimation.setRepeatCount(Animation.INFINITE);
        // Reverse animation at the end so the button will fade back in
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        return blinkAnimation;
    }

    //TODO: getWindow background animation
    public static Animation loadingFactory(int intervalTime) {

        Animation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setDuration(intervalTime);

        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }
}
