package com.asus.zenheart.smilemirror.VideoTexture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.util.Log;

import com.asus.zenheart.smilemirror.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

class VideoTextureSurfaceRenderer extends TextureSurfaceRenderer implements
        SurfaceTexture.OnFrameAvailableListener {

    private static final float SQUARE_SIZE = 1.0f;
    private static final float SQUARE_COORDINATES[] = {
            -SQUARE_SIZE, SQUARE_SIZE,   // top left
            -SQUARE_SIZE, -SQUARE_SIZE,   // bottom left
            SQUARE_SIZE, -SQUARE_SIZE,    // bottom right
            SQUARE_SIZE, SQUARE_SIZE}; // top right

    private static final short DRAW_ORDER[] = {
            0, 1, 2,
            0, 2, 3};

    // Texture to be shown in background
    private FloatBuffer mTextureBuffer;
    // Texture coordinate
    private float mTextureCoordinates[] = {
            0.0f, 1.0f, 0.0f, 1.0f,  // top left
            0.0f, 0.0f, 0.0f, 1.0f,  // bottom left
            1.0f, 0.0f, 0.0f, 1.0f,  // bottom right
            1.0f, 1.0f, 0.0f, 1.0f};  // top right

    private int[] mTextures = new int[1];

    private int mShaderProgram = -1;
    private FloatBuffer mVertexBuffer;
    private ShortBuffer mDrawListBuffer;

    private float[] mVideoTextureTransform;
    private AtomicBoolean mFrameAvailable = new AtomicBoolean(false);
    private Context mContext;
    private SurfaceTexture mVideoTexture;

    private int mFragmentShaderResId = R.raw.fragment_sharder;
    private int mTextureParamHandle;
    private int mTextureCoordinateHandle;
    private int mPositionHandle;
    private int mTextureTransformHandle;

    private int mWidth;
    private int mHeight;
    private int mVideoWidth;
    private int mVideoHeight;

    /***
     * in this attribute, draw the view for the surface extends {@link TextureSurfaceRenderer},
     * it implement {@link SurfaceTexture.OnFrameAvailableListener}
     */
    VideoTextureSurfaceRenderer(@NonNull Context context, SurfaceTexture texture, int width,
            int height, @RawRes int fragmentShaderResId) {
        super(texture, width, height);
        mContext = context;
        mWidth = width;
        mHeight = height;
        mVideoTextureTransform = new float[16];
        mFragmentShaderResId = fragmentShaderResId;
    }

    /**
     * Used to set the video width and height before draw the video in OpenGL.
     *
     * @param width The width of the video.
     * @param height The height of the video.
     */
    public void setVideoSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
    }

    private void setupGraphics() {
        final String vertexShader = RawResourceReader(mContext, R.raw.vertex_sharder);
        final String fragmentShader = RawResourceReader(mContext, mFragmentShaderResId);

        final int vertexShaderHandle = ShaderHelper
                .compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper
                .compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        if (mShaderProgram == -1) {
            mShaderProgram = ShaderHelper
                    .createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                            new String[]{"texture", "vPosition", "vTexCoordinate",
                                    "textureTransform"});
        }

        GLES20.glUseProgram(mShaderProgram);
        mTextureParamHandle = GLES20.glGetUniformLocation(mShaderProgram, "texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mShaderProgram, "vTexCoordinate");
        mPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        mTextureTransformHandle = GLES20.glGetUniformLocation(mShaderProgram, "textureTransform");
    }

    private String RawResourceReader(@NonNull final Context context, @RawRes final int resourceId) {
        final StringBuilder body = new StringBuilder();

        try (final InputStream inputStream = context.getResources().openRawResource(resourceId);
             final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String nextLine;
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return body.toString();
    }

    private void setupVertexBuffer() {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(DRAW_ORDER.length * 2);
        dlb.order(ByteOrder.nativeOrder()); // Modifies this buffer's byte order
        mDrawListBuffer = dlb.asShortBuffer(); // create this view's short buffer
        mDrawListBuffer.put(DRAW_ORDER);
        mDrawListBuffer.position(0); //to the next buffer, start with position 0

        // Initialize the texture holder
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SQUARE_COORDINATES.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        mVertexBuffer = byteBuffer.asFloatBuffer();
        mVertexBuffer.put(SQUARE_COORDINATES);
        mVertexBuffer.position(0);
    }

    private void setupTexture() {
        ByteBuffer textureByteBuffer = ByteBuffer.allocateDirect(mTextureCoordinates.length * 4);
        textureByteBuffer.order(ByteOrder.nativeOrder());

        mTextureBuffer = textureByteBuffer.asFloatBuffer();
        mTextureBuffer.put(mTextureCoordinates);
        mTextureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, mTextures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        checkGlError("Texture bind");

        mVideoTexture = new SurfaceTexture(mTextures[0]);
        mVideoTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected boolean draw() {
        if (mFrameAvailable.compareAndSet(true, false)) {
            mVideoTexture.updateTexImage();
            mVideoTexture.getTransformMatrix(mVideoTextureTransform);
        } else {
            return false;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glViewport(0, mHeight - mVideoHeight, mVideoWidth, mVideoHeight);
        drawTexture();

        return true;
    }

    @Override
    public void initGLComponents() {
        setupVertexBuffer();
        setupTexture();
        setupGraphics();
    }

    @Override
    protected void releaseGLComponents() {
        GLES20.glDeleteTextures(1, mTextures, 0);
        GLES20.glDeleteProgram(mShaderProgram);
        mVideoTexture.release();
        mVideoTexture.setOnFrameAvailableListener(null);
    }

    public SurfaceTexture getVideoTexture() {
        return mVideoTexture;
    }

    @Override
    public void clearSurface() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] attributeList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attributeList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                new int[]{
                        12440, 2, EGL10.EGL_NONE
                });
        EGLSurface eglSurface = egl.eglCreateWindowSurface(display, config, mVideoTexture,
                new int[]{
                        EGL10.EGL_NONE
                });

        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }

    private void drawTexture() {
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(mTextureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0,
                mTextureBuffer);

        GLES20.glUniformMatrix4fv(mTextureTransformHandle, 1, false, mVideoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, DRAW_ORDER.length,
                GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mFrameAvailable.set(true);
    }
}
