/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asus.zenheart.smilemirror.ui.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.CameraInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.Util.GalleryUtil;
import com.asus.zenheart.smilemirror.Util.LogUtils;
import com.asus.zenheart.smilemirror.Util.PermissionUtil;
import com.asus.zenheart.smilemirror.Util.PrefsUtils;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Thread.State;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Note: This requires Google Play Services 8.1 or higher, due to using indirect byte buffers for
// storing images.

/**
 * Manages the camera in conjunction with an underlying
 * {@link com.google.android.gms.vision.Detector}.  This receives preview frames from the camera at
 * a specified rate, sending those frames to the detector as fast as it is able to process those
 * frames.
 * The following Android permission is required to use the camera:
 * <ul>
 * <li>android.permissions.CAMERA</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public class CameraSource {
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;
    @SuppressLint("InlinedApi")
    public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;

    private static final String TAG = "CameraSource";

    @IntDef({Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ORIENTATION {
    }

    private Context mContext;
    private final Object mCameraLock = new Object();

    /**
     * Rotation of the device, and thus the associated preview images captured from the device.
     * See {@link Frame.Metadata#getRotation()}.
     */
    private int mRotation;
    @ORIENTATION
    private int mScreenRotation;

    // These values may be requested by the caller.  Due to hardware limitations, we may need to
    // select close, but not exactly the same values for these.
    private int mRequestedPreviewWidth = 640;
    private int mRequestedPreviewHeight = 480;
    private int mFacing = CAMERA_FACING_BACK;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    //TODO ++ JACk  ------------
    /**
     * members for Camera2 API
     */
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray DETECTOR_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    static {
        DETECTOR_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        DETECTOR_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DETECTOR_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        DETECTOR_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // Guarded by mCameraLock
    private CameraDevice mCameraDevice;

    private boolean mIsOpened;
    private MediaRecorder mMediaRecorder;
    private ImageReader mImageReader;
    private TextureView mTextureView;
    private CameraCaptureSession mCaptureSession;
    private Size mVideoSize;
    private Size mImagePreviewSize;

    private Integer mSensorOrientation;
    private CaptureRequest.Builder mPreviewBuilder;
    private Surface mRecorderSurface;
    private String mNextVideoAbsolutePath;

    //TODO watch out if it is necessary to use mHandler
    private Handler mHandler;

    //TODO --jack
    //==============================================================================================
    // Builder
    //==============================================================================================

    /**
     * Builder for configuring and creating an associated camera source.
     */
    public static class Builder {
        private final Detector<?> mDetector;
        private CameraSource mCameraSource = new CameraSource();

        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder(Context context, Detector<?> detector) {
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            mDetector = detector;
            mCameraSource.mContext = context;
        }

        /**
         * Sets the desired width and height of the camera frames in pixels.  If the exact desired
         * values are not available options, the best matching available options are selected.
         * Also, we try to select a preview size which corresponds to the aspect ratio of an
         * associated full picture size, if applicable.  Default: 1024x768.
         */
        public Builder setRequestedPreviewSize(int width, int height) {
            // Restrict the requested range to something within the realm of possibility.  The
            // choice of 1000000 is a bit arbitrary -- intended to be well beyond resolutions that
            // devices can support.  We bound this to avoid int overflow in the code later.
            final int MAX = 1000000;
            if ((width <= 0) || (width > MAX) || (height <= 0) || (height > MAX)) {
                throw new IllegalArgumentException("Invalid preview size: " + width + "x" + height);
            }
            mCameraSource.mRequestedPreviewWidth = width;
            mCameraSource.mRequestedPreviewHeight = height;
            return this;
        }

        /**
         * Sets the camera to use (either {@link #CAMERA_FACING_BACK} or
         * {@link #CAMERA_FACING_FRONT}). Default: back facing.
         */
        public Builder setFacing(int facing) {
            if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
                throw new IllegalArgumentException("Invalid camera: " + facing);
            }
            mCameraSource.mFacing = facing;
            return this;
        }

        /**
         * Creates an instance of the camera source.
         */
        public CameraSource build() {
            mCameraSource.mFrameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
            return mCameraSource;
        }
    }

    //==============================================================================================
    // Public
    //==============================================================================================

    //TODO Jack +++

    /**
     * Opens the camera with Camera2 and starts sending preview frames to the underlying detector
     * The preview  frames are displayed,but not necessary.
     *
     * @throws IOException if the camera's preview texture or display could not be initialized
     */

    public CameraSource start(TextureView textureView) throws IOException {
        //TODO watch out mCameraLock
        synchronized (mCameraLock) {
            if (mCameraDevice != null) {
                return this;
            }
            mTextureView = textureView;
            openCamera2();

            mProcessingThread = new Thread(mFrameProcessor);
            mFrameProcessor.setActive(true);
            mProcessingThread.start();
        }
        return this;
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     * <p/>
     * This camera source may be restarted again by calling {@link #start(TextureView)}.
     * <p/>
     * Call {@link #release()} instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */

    public void stop() {
        synchronized (mCameraLock) {
            mFrameProcessor.setActive(false);
            if (mProcessingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time (i.e., which would happen if we called start too
                    // quickly after stop).
                    mProcessingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                mProcessingThread = null;
            }

            // Jack +++
            mIsOpened = false;
            closeCamera();
            // Jack ---

        }
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    public void release() {
        synchronized (mCameraLock) {
            stop();
            mFrameProcessor.release();
        }
    }

    /**
     * Returns the preview size that is currently in use by the underlying camera.
     */
    public android.util.Size getVideoSize() {
        return mVideoSize;
    }

    /**
     * Returns the selected camera; one of {@link #CAMERA_FACING_BACK} or
     * {@link #CAMERA_FACING_FRONT}.
     */
    public int getCameraFacing() {
        return mFacing;
    }

    //==============================================================================================
    // Private
    //==============================================================================================

    /**
     * Only allow creation via the builder class.
     */
    private CameraSource() {
    }

    /**
     * {@link #mRotation }should be initialized as 270 because of the screenOrientation in Manifest
     * is set "portrait".
     * {@link #mRotation } will be change by the method {@link #setScreenRotation(int)} invoked by
     * sensorListener callback when screen is rotated
     */
    private void setRotation() {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        final int rotation = windowManager.getDefaultDisplay().getRotation();
        mRotation = DETECTOR_ORIENTATIONS.get(rotation) / 90;
        mScreenRotation = rotation;
    }

//==============================================================================================
// Frame processing
//==============================================================================================

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera.  This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     * <p/>
     * While detection is running on a frame, new frames may be received from the camera.  As these
     * frames come in, the most recent frame is held onto as pending.  As soon as detection and its
     * associated processing are done for the previous frame, detection on the mostly recently
     * received frame will immediately start on the same thread.
     */
    private class FrameProcessingRunnable implements Runnable {
        private FaceDetector mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private ByteBuffer mPendingFrameData;

        //Jack +++
        private Detector<?> mDetectorNew;
        private boolean mDetectorChanged;
        private final Object mDetectorLock = new Object();

        //Jack ---
        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = (FaceDetector) detector;
        }

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread.getState() == State.TERMINATED);
            if (mDetector != null) {
                mDetector.release();
            }
            if (mDetectorNew != null) {
                mDetectorNew.release();
            }
            mDetector = null;
            mDetectorNew = null;
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }

        /**
         * Sets the frame data received from the camera given by imageReader.
         * (if present) back to the camera, and keeps a pending reference to the frame data for
         * future use.
         */

        void setNextFrame(Image image) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    mPendingFrameData = null;
                }
                final byte[] bytes = convertYUV420ToNV21(image);

                // Timestamp and frame ID are maintained here, which will give downstream code some
                // idea of the timing of frames received and when frames were dropped along the way.
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = ByteBuffer.wrap(bytes);

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll();
            }
        }

        /**
         * Reset Detector for different orientation
         */
        //TODO: consider race condition when detector receive frame
        void setDetector(FaceDetector detector) {
            synchronized (mDetectorLock) {
                mDetectorNew = detector;
                mDetectorChanged = true;
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames
         * continuously.  The next pending frame is either immediately available or hasn't been
         * received yet.  Once it is available, we transfer the frame info to local variables and
         * run detection on that frame.  It immediately loops back for the next frame without
         * pausing.
         * <p/>
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context
         * switching or frame acquisition time latency.
         * <p/>
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @Override
        public void run() {
            Frame outputFrame;
            LogUtils.d(TAG, "thread is = " + Thread.currentThread().getId());
            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }
                    outputFrame = new Frame.Builder()
                            .setImageData(mPendingFrameData, mVideoSize.getWidth(),
                                    mVideoSize.getHeight(), ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(mRotation)
                            .build();

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear mPendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.

                    mPendingFrameData = null;
                }

                synchronized (mDetectorLock) {
                    if (mDetectorChanged) {
                        if (mDetector != null) {
                            mDetector.release();
                        }
                        mDetector = (FaceDetector) mDetectorNew;
                        mDetectorChanged = false;
                        continue;
                    }
                }
                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.
                try {
                    mDetector.receiveFrame(outputFrame);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                }
            }
        }

    }

    // Jack +++

    /**
     * Open Camera by Camera2API.
     * startPreview once manager.openCamera succeed
     */
    @SuppressWarnings({"MissingPermission"})
    private void openCamera2() {
        if (mIsOpened) {
            Log.e(TAG, "Camera is opened,it should not be opened again");
            return;
        }
        if (!PermissionUtil.hasPermissions(mContext, PermissionUtil.VIDEO_PERMISSIONS)) {
            Log.w(TAG, "Runtime Permission denied ...");
            return;
        }

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = getFrontFacingCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                Log.e(TAG, "configuration map should not be null");
                return;
            }
            mMediaRecorder = new MediaRecorder();
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mVideoSize = selectSize(map.getOutputSizes(MediaRecorder.class),
                    mRequestedPreviewWidth, mRequestedPreviewHeight);
            mImagePreviewSize = selectSize(map.getOutputSizes(SurfaceTexture.class),
                    mRequestedPreviewWidth, mRequestedPreviewHeight);
            //TODO: delete logs below in the future
            final Activity activity = (FaceTrackerActivity) mContext;
            LogUtils.d(TAG,
                    "rotation = " + activity.getWindowManager().getDefaultDisplay().getRotation());
            LogUtils.d(TAG, "SensorOrientation = " + mSensorOrientation);
            LogUtils.d(TAG, "videoSize = " + mVideoSize
                    .getWidth() + " videoSize = " + mVideoSize.getHeight());
            LogUtils.d(TAG, "image width = " + mImagePreviewSize.getWidth() + " image height = " +
                    mImagePreviewSize.getHeight());

            //configureTransform(mTextureView.getWidth(), mTextureView.getHeight(), activity);
            setRotation();
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.i(TAG, "Camera Device is onOpened");
                    mIsOpened = true;
                    mCameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.i(TAG, "Camera Device is onDisconnected");
                    mIsOpened = false;
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "onError -> " + error);
                    camera.close();
                }
            }, null);
            Log.i(TAG, "open Camera id = " + cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void startPreview() {
        try {
            if (mCameraDevice == null) {
                Log.w(TAG, "updatePreview error ..... return");
                return;
            }
            closePreviewSession();
            setUpImageReader();
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //SurfaceTexture section
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mImagePreviewSize.getWidth(),
                    mImagePreviewSize.getHeight());
            Surface textureSurface = new Surface(texture);
            mPreviewBuilder.addTarget(textureSurface);
            //imageReader section
            Surface imageSurface = mImageReader.getSurface();
            mPreviewBuilder.addTarget(imageSurface);

            List<Surface> surfaceList = Arrays.asList(textureSurface, imageSurface);
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback
                    () {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Camera Device Configuration failed");
                }
            }, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * It is used to set up recorder spec. setVideoEncodingBitRate and setVideoSize is necessary
     */
    private void setUpMediaRecorder() {
        final int ENCODING_BIT_RATE = 3000000;
        final int ENCODING_FRAME_RATE = 15;//frame per minutes

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mNextVideoAbsolutePath = GalleryUtil.getVideoFileName();
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(ENCODING_BIT_RATE);
        mMediaRecorder.setVideoFrameRate(ENCODING_FRAME_RATE);
        mMediaRecorder.setOrientationHint(mSensorOrientation);
        //do nothing without below
        //However camera1 does not need below and need mediaRecorder.setCamera
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * It is used to set up ImageReader for read preview frame.
     * Note that not all format is supported,like ImageFormat.NV21,which is needed by faceDetector
     * Therefore,image's format transformation is needed before fed into faceDetector
     */
    private void setUpImageReader() {
        final int MAX_IMAGES = 10;
        mImageReader = ImageReader.newInstance(mImagePreviewSize.getWidth(),
                mImagePreviewSize.getHeight(), ImageFormat.YUV_420_888, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                if (image != null) {
                    //TODO implement the face detection related code
                    mFrameProcessor.setNextFrame(image);
                    image.close();
                }
            }
        }, mHandler);
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {

        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void closePreviewSession() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    private void startRecordingVideo() {
        if (mCameraDevice == null || !mTextureView.isAvailable() || mImagePreviewSize == null) {
            Log.e(TAG, "CameraDevice = " + mCameraDevice + " or TextureView  = " + mTextureView
                    .isAvailable() + " or ImagePreview = " + mImagePreviewSize + " is not ready !!!");
            return;
        }
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            setUpImageReader();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mImagePreviewSize.getWidth(),
                    mImagePreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            mRecorderSurface = mMediaRecorder.getSurface();
            surfaces.add(mRecorderSurface);
            mPreviewBuilder.addTarget(mRecorderSurface);

            // Set up Surface for the MediaRecorder
            Surface imageSurface = mImageReader.getSurface();
            surfaces.add(imageSurface);
            mPreviewBuilder.addTarget(imageSurface);

            // Start a capture session
            // Once the session starts, we can  start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    updatePreview();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "CameraDevice Configuration failed");
                }
            }, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void stopRecordingVideo() {
        if (mCameraDevice == null || !mTextureView.isAvailable() || mImagePreviewSize == null) {
            Log.e(TAG,
                    "can't stop Recording with CameraDevice,TextureView,or ImagePreview is not ready");
            return;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        GalleryUtil.sendMediaScanIntent(mContext, mNextVideoAbsolutePath);
        startPreview();
    }

    public String getNextVideoName() {
        String[] nextVideoAbsolutePath = mNextVideoAbsolutePath.split("/");
        return nextVideoAbsolutePath[7];
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (mCameraDevice == null) {
            // The camera is already closed
            Log.w("Camera2", "CameraDevice is already closed .... return ");
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    //TODO: fix in the future ,delete if not used anymore
    private void configureTransform(int viewWidth, int viewHeight, Activity activity) {
        if (null == mTextureView || null == mVideoSize || null == activity) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mVideoSize.getHeight(), mVideoSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mVideoSize.getHeight(),
                    (float) viewWidth / mVideoSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    //==============================================================================================
    // static method
    //==============================================================================================



    /**
     * This method used to find front-camera id
     *
     * @param cManager CameraManager get by system service
     * @return cameraId corresponding to front-camera
     * @throws CameraAccessException
     */
    private static String getFrontFacingCameraId(@NonNull CameraManager cManager)
            throws CameraAccessException {
        for (final String cameraId : cManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                return cameraId;
        }
        throw new CameraAccessException(CameraAccessException.CAMERA_ERROR,
                "can not get the front-camera Id");
    }

    /**
     * This method used to choose a appropriate videoSize,which is 4 : 3
     * It is expected that videoSize is the same as preview size as 640* 480
     *
     * @param choices consists of different preview sizes
     * @return an appropriate size,expected as 640* 480
     */
    private static Size selectSize(Size[] choices, int desiredWidth,
            int desiredHeight) {
        if (choices == null) {
            Log.e(TAG, "camera supported preview size could not be null!!!");
            return null;
        }
        for (Size size : choices) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width == height * 4 / 3 && width <= desiredWidth && height <= desiredHeight) {
                return size;
            }
        }
        Log.w(TAG, "Couldn't find any suitable videoSize... return default size");
        return choices[0];
    }

    /**
     * This method used to convert YUV420 to NV21 because imageReader could only read image
     * data in YUV420 but faceDetector can only receive data in NV21
     *
     * @param imgYUV420 image data organized in YUV420
     * @return byte data organized in NV21
     */
    private static byte[] convertYUV420ToNV21(Image imgYUV420) {

        byte[] rez;

        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer1 = imgYUV420.getPlanes()[1].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();

        final int buffer0_size = buffer0.remaining();
        final int buffer1_size = buffer1.remaining();
        final int buffer2_size = buffer2.remaining();

        byte[] buffer0_byte = new byte[buffer0_size];
        byte[] buffer1_byte = new byte[buffer1_size];
        byte[] buffer2_byte = new byte[buffer2_size];

        buffer0.get(buffer0_byte, 0, buffer0_size);
        buffer1.get(buffer1_byte, 0, buffer1_size);
        buffer2.get(buffer2_byte, 0, buffer2_size);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(buffer0_byte);
            outputStream.write(buffer1_byte);
            outputStream.write(buffer2_byte);
        } catch (IOException e) {
            e.printStackTrace();
        }

        rez = outputStream.toByteArray();

        return rez;
    }

    //==============================================================================================
    // Public method
    //==============================================================================================
    public void startRecord() {
        if (!PrefsUtils.getBooleanPreference(mContext, PrefsUtils.PREFS_AUTO_RECORDING, true)) {
            return;
        }
        startRecordingVideo();
    }

    public void stopRecord() {
        if (!PrefsUtils.getBooleanPreference(mContext, PrefsUtils.PREFS_AUTO_RECORDING, true)) {
            return;
        }
        stopRecordingVideo();
    }

    public void setScreenRotation(@ORIENTATION int rotation) {
        mRotation = DETECTOR_ORIENTATIONS.get(rotation) / 90;
        mScreenRotation = rotation;
        Log.d(TAG, "setRotation ... mRotation = " + mRotation);
    }

    @ORIENTATION
    public int getScreenRotation() {
        return mScreenRotation;
    }

    public void setDetector(FaceDetector detector) {
        mFrameProcessor.setDetector(detector);
    }
}
