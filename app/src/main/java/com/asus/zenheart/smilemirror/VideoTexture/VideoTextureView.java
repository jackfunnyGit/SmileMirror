package com.asus.zenheart.smilemirror.VideoTexture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;


/**
 *  VideoTextureView can used to play the video in the texture view.
 *  You just need to create and setResourceId.
 *
 */

public class VideoTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private MediaPlayer mMediaPlayer;
    private SurfaceTexture mSurfaceTexture;
    private TextureSurfaceRenderer mVideoRenderer;
    private Context mContext;
    private int mEffectId;
    private int mShaderId;
    private VideoCompletionCallback mCallback;
    public void setCompletionListener(VideoCompletionCallback callback) {
        mCallback = callback;
    }

    public VideoTextureView(Context context) {
        super(context);
        mContext = context;
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    public VideoTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setSurfaceTextureListener(this);
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

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mSurfaceTexture = surface;
        initVideoRenderer();
        initMediaPlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        mSurfaceTexture = surface;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    /**
     * Clear the content in the video renderer.
     *
     */
    public void clearTextureSurface() {
        if (mVideoRenderer != null) {
            mVideoRenderer.clearSurface();
        }
    }

    public void clearRenderer() {
        if (mVideoRenderer != null) {
            mVideoRenderer.onStop();
            mVideoRenderer = null;
        }
    }

    private void initVideoRenderer() {
        if (mVideoRenderer == null) {
            // Care the renderer can not repeatedly create, or it will crash.
            mVideoRenderer = new VideoTextureSurfaceRenderer(mContext, mSurfaceTexture,
                    mSurfaceWidth, mSurfaceHeight, mShaderId);
        }
    }

    /**
     * Helper function to create and play the effect.
     */
    public void initMediaPlayer() {
        try {
            // If you don not set the resource and shader, you will crash in here.
            mMediaPlayer = MediaPlayer.create(mContext, mEffectId);
            mMediaPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
            mMediaPlayer.setLooping(false);
            Surface videoSurface = new Surface(mVideoRenderer.getVideoTexture());
            mMediaPlayer.setSurface(videoSurface);
            videoSurface.release();
        } catch (IllegalArgumentException | IllegalStateException |
        SecurityException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Play the media player which includes the video data.
     *
     */
    public void playMediaPlayer() {
        try {
            if (mMediaPlayer != null) {
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Pause the media player which includes the video data.
     *
     */
    public void pauseMediaPlayer() {
        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop the media player which includes the video data.
     *
     */
    public void stopMediaPlayer() {
        try {
            pauseMediaPlayer();
            releaseMediaPlayer();
            clearTextureSurface();
            clearRenderer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Release exist media player which includes the video data.
     *
     */
    public void releaseMediaPlayer() {
        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                } else {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void setVideoSize(int width, int height) {
        mVideoRenderer.setVideoSize(width, height);
    }
}
