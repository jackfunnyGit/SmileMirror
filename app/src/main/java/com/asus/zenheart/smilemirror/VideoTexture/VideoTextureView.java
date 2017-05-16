package com.asus.zenheart.smilemirror.VideoTexture;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;


/**
 *  VideoTextureView can used to play the video in the texture view.
 *  You just need to create and setResourceId.
 *
 */

public class VideoTextureView extends TextureView {
    private static final int VIDEO_EFFECT_WIDTH = 1080;
    private static final int VIDEO_EFFECT_HEIGHT = 1200;
    private MediaPlayer mMediaPlayer;
    private TextureSurfaceRenderer mVideoRenderer;
    private int mEffectId;
    private int mShaderId;
    private VideoCompletionCallback mCallback;
    public void setCompletionListener(VideoCompletionCallback callback) {
        mCallback = callback;
    }

    public VideoTextureView(Context context) {
        super(context);
        setOpaque(false);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOpaque(false);
    }

    public VideoTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOpaque(false);
    }

    /**
     * Set the video which we want to play. If  we do not call this function, it will play the default video.
     * This function is very important, if you don't call it and your application will crash.
     *
     */
    public void setResourceId(int effectId, int shaderId) {
        mEffectId = effectId;
        mShaderId = shaderId;
    }

    public interface VideoCompletionCallback {
        void onCompletion();
    }

    /**
     * Helper function to create and play the effect.
     */
    public void initMediaPlayer() {
        if (mVideoRenderer == null) {
            mVideoRenderer = new VideoTextureSurfaceRenderer(getContext(), getSurfaceTexture(),
                    getWidth(), getHeight(), mShaderId);
            mVideoRenderer.setVideoSize(VIDEO_EFFECT_WIDTH, VIDEO_EFFECT_HEIGHT);
        }

        mMediaPlayer = MediaPlayer.create(getContext(), mEffectId);
        mMediaPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
        mMediaPlayer.setLooping(false);
        while (mVideoRenderer.getVideoTexture() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Surface videoSurface = new Surface(mVideoRenderer.getVideoTexture());
        mMediaPlayer.setSurface(videoSurface);
        videoSurface.release();
    }

    /**
     * Play the media player which includes the video data.
     *
     */
    public void playMediaPlayer() {
        if (isAvailable()) {
            initMediaPlayer();
        }
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        }
    }

    /**
     * Stop the media player which includes the video data and also stop renderer.
     */
    public void stopMediaPlayerAndRenderer() {
        stopMediaPlayer();
        if (mVideoRenderer != null) {
            mVideoRenderer.onStop();
            mVideoRenderer = null;
        }
    }

    /**
     * Stop the media player only, usually want to cache renderer and repeat video condition.
     * Use this method can help reduce initial media player time, please sure to call
     * {@link VideoTextureView#stopMediaPlayerAndRenderer()} before create another new one.
     */
    public void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Media player is playing or not.
     *
     */
    public boolean isPlayingMediaPlayer() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    /**
     * Media player completion call back.
     * If the video is play finished, we should clear the view and init the media player.
     */
    private class MediaPlayerCompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            if (mCallback != null) {
                mCallback.onCompletion();
            }
        }
    }
}
