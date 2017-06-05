package com.asus.zenheart.smilemirror.VideoTexture;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLUtils;

import com.asus.zenheart.smilemirror.Util.LogUtils;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

abstract class TextureSurfaceRenderer implements Runnable {
    private static final String TAG = "TextureSurfaceRenderer";
    private final SurfaceTexture mSurfaceTexture;
    private final int[] mVersion = new int[2];
    private final int mWidth;
    private final int mHeight;

    private EGL10 mEgl;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private boolean mIsRunning = false;
    private RendererReadyCallback mCallback;

    TextureSurfaceRenderer(SurfaceTexture surfaceTexture, int width, int height) {
        mSurfaceTexture = surfaceTexture;
        mWidth = width;
        mHeight = height;
        mIsRunning = true;
        Thread thread = new Thread(this);
        thread.start();
    }
    void setRendererReadyListener(RendererReadyCallback callback) {
        mCallback = callback;
    }

    /**
     * Helper function to init the Egl, including  EglDisplay, EglSurface and EglContext.
     *
     */
    private void initEGL() {
        mEgl = (EGL10) EGLContext.getEGL();
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        mEgl.eglInitialize(mEglDisplay, mVersion);

        EGLConfig eglConfig = chooseEglConfig();
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, eglConfig, mSurfaceTexture, null);

        mEglContext = createContext(mEgl, mEglDisplay, eglConfig);

        try {
            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
                throw new RuntimeException(
                        "GL error:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                throw new RuntimeException(
                        "GL make current error" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            initEGL();
            initGLComponents();
            if (mCallback != null) {
                mCallback.onGLComponentsReady();
            }
        } catch (RuntimeException e) {
            LogUtils.e(TAG, "Init GL fail", e);
        }
        while (mIsRunning) {
            if (draw()) {
                mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
            }
        }
        releaseGLComponents();
        releaseEGL();
    }

    /**
     * Recycle the Egl resource.
     *
     */
    private void releaseEGL() {
        mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
        mEgl.eglDestroyContext(mEglDisplay, mEglContext);
        mEgl.eglTerminate(mEglDisplay);
    }

    protected abstract boolean draw();

    protected abstract void initGLComponents();

    protected abstract void releaseGLComponents();

    public abstract SurfaceTexture getVideoTexture();

    public abstract void clearSurface();

    public abstract void setVideoSize(int width, int height);

    interface RendererReadyCallback {
        void onGLComponentsReady();
    }

    /**
     * Create a context for the drawing api.
     *
     * @return a handle to the context
     */
    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrs);
    }

    /**
     * Create a context for the drawing api.
     *
     * @return a most  meets requirement config for your Egl spec.
     */
    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] attributes = getAttributes();
        int confSize = 1;

        if (!mEgl.eglChooseConfig(mEglDisplay, attributes, configs, confSize, configsCount)) {
            throw new IllegalArgumentException(
                    "Failed to choose config:" + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        } else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }

    private int[] getAttributes() {
        return new int[]{
                EGL10.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE      // always end in EGL10.EGL_NONE
        };
    }

    /**
     * Call when activity pauses. This stops the rendering thread and release OpenGL.
     */
    void onStop() {
        mIsRunning = false;
    }
}
