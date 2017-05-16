package com.asus.zenheart.smilemirror.VideoTexture;

import android.content.Context;
import android.support.annotation.RawRes;
import android.util.AttributeSet;

import com.asus.zenheart.smilemirror.R;

/**
 * This view is extends {@link VideoTextureView}
 * It only used for Smile Mirror.
 */

public class SmileVideoTextureView extends VideoTextureView {

    public SmileVideoTextureView(Context context) {
        super(context);
    }

    public SmileVideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmileVideoTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private @interface FilterType {
        @RawRes
        int HEART_EFFECT = R.raw.effect_heartbling;
        @RawRes
        int STAR_EFFECT = R.raw.effect_star;
        @RawRes
        int BLING_EFFECT = R.raw.effect_bling;
    }

    private @interface ShaderType {
        @RawRes
        int DEFAULT_SHADER = R.raw.clear_color_fragment_sharder;
        @RawRes
        int SPECIAL_SHADER = R.raw.special_custom_fragment_sharder;
    }

    public int getRandomEffect() {
        int random = (int) (Math.random() * 3);
        switch (random) {
            case 0:
                return FilterType.HEART_EFFECT;
            case 1:
                return FilterType.STAR_EFFECT;
            case 2:
                return FilterType.BLING_EFFECT;
        }
        return FilterType.HEART_EFFECT;
    }

    public int getCorrectShader() {
        return ShaderType.SPECIAL_SHADER;
    }

}
