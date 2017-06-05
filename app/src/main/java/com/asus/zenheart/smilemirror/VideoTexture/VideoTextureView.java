package com.asus.zenheart.smilemirror.VideoTexture;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

/**
 * VideoTextureView can used to play the video in the texture view.
 * You just need to create and setResourceId.
 */

public class VideoTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final int VIDEO_EFFECT_WIDTH = 1080;
    private static final int VIDEO_EFFECT_HEIGHT = 1200;
    private MediaPlayer mMediaPlayer;
    private TextureSurfaceRenderer mVideoRenderer;
    private int mEffectId;
    private int mShaderId;
    private VideoCompletionCallback mCallback;
    private Context mContext;

    public void setCompletionListener(VideoCompletionCallback callback) {
        mCallback = callback;
    }

    public VideoTextureView(Context context) {
        super(context);
        mContext = getContext();
        initVideoTextureView();
    }

    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = getContext();
        initVideoTextureView();
    }

    public VideoTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = getContext();
        initVideoTextureView();
    }

    private void initVideoTextureView() {
        setOpaque(false);
    }

    /**
     * Set the video which we want to play. If  we do not call this function, it will play the default video.
     * This function is very important, if you don't call it and your application will crash.
     */
    public void setResourceId(int effectId, int shaderId) {
        mEffectId = effectId;
        mShaderId = shaderId;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initMediaPlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public interface VideoCompletionCallback {
        void onCompletion();
    }

    /**
     * Helper function to create renderer, media player and play the effect.
     */
    public void initMediaPlayer() {
        if (mVideoRenderer == null) {
            mVideoRenderer = new VideoTextureSurfaceRenderer(mContext, getSurfaceTexture(),
                    getWidth(), getHeight(), mShaderId);
            mVideoRenderer.setVideoSize(VIDEO_EFFECT_WIDTH, VIDEO_EFFECT_HEIGHT);
        }

        mVideoRenderer.setRendererReadyListener(new TextureSurfaceRenderer.RendererReadyCallback() {
            @Override
            public void onGLComponentsReady() {
                mMediaPlayer = MediaPlayer.create(mContext, mEffectId);
                mMediaPlayer.setOnCompletionListener(new MediaPlayerCompletionListener());
                mMediaPlayer.setLooping(false);
                Surface videoSurface = new Surface(mVideoRenderer.getVideoTexture());
                mMediaPlayer.setSurface(videoSurface);
                videoSurface.release();
                startMediaPlayer();
            }
        });
    }

    /**
     * Helper function to change the raw data, this function will be used in the future.
     */
    public void changeVideoData() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
        try (AssetFileDescriptor assetFileDescriptor =
                     mContext.getResources().openRawResourceFd(mEffectId)) {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
    }

    /**
     * Play the media player which includes the video data.
     */
    public void playMediaPlayer() {
        if (isAvailable()) {
            if (mMediaPlayer == null) {
                initMediaPlayer();
            }
        } else {
            setSurfaceTextureListener(this);
        }
    }

    private void startMediaPlayer() {
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
        stopMediaPlayerAndRelease();
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
    private void stopMediaPlayerAndRelease() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Media player is playing or not.
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
