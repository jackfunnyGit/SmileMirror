package com.asus.zenheart.smilemirror.Util;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.ui.camera.CameraSource;

//TODO rename and refactor in the future, factory should not be used like this
public class AnimationUtil {
    private static final String LOG_TAG = "AnimationUtil";
    private static final int TOAST_ANIMATION_TIME_MILL = 2000;
    private static final int TOAST_REPEATED_TIME = 1;
    private static final int BLINK_ANIMATION_TIME_MILL = 500;
    private static final int ROTATION_ANIMATION_TIME_MILL = 100;

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

    /**
     * Helper method to retrieve a String value from {@link TextView}.
     *
     * @param toastView A {@link TextView} object.
     * @param stringId The text view string
     * @param drawableId If the toast shown with the icon, please enter the drawableId.If not, please enter value 0.
     */
    public static void showToast(@NonNull TextView toastView, int stringId, int drawableId) {
        toastView.setVisibility(View.VISIBLE);
        toastView.setText(stringId);
        if (drawableId != 0) {
            toastView.setCompoundDrawablesWithIntrinsicBounds(0, drawableId, 0, 0);
        }
        toastAnimation(toastView);
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
    public static void rotateAnimation(View view,float rotation){
        view.animate().rotation(rotation).setDuration(ROTATION_ANIMATION_TIME_MILL).start();
    }

}
