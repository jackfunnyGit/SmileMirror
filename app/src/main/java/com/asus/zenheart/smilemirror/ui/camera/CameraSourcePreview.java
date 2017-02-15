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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.ViewGroup;

import java.io.IOException;

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private Context mContext;
    private TextureView mTextureView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    private GraphicOverlay mOverlay;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;//be careful of if surfaceTexture is ready

    }

    public void start(CameraSource cameraSource) throws IOException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;

        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException {
        mOverlay = overlay;
        start(cameraSource);
    }

    //Jack +++
    public void start(CameraSource cameraSource, GraphicOverlay overlay, TextureView textureView)
            throws IOException {
        Log.i(TAG, "startCameraSource .....CameraPreview.start");
        mTextureView = textureView;
        if (mTextureView.isAvailable()) {
            mSurfaceAvailable = true;
        }
        mTextureView.setSurfaceTextureListener(new SurfaceTextureCallback());
        start(cameraSource, overlay);
    }

    //Jack ---
    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    private void startIfReady() throws IOException {
        Log.i(TAG, "startIfReady ..........");
        Log.i(TAG,
                "mStartRequested = " + mStartRequested + " SurfaceAvailable = " + mSurfaceAvailable);
        if (mStartRequested && mSurfaceAvailable) {
            mCameraSource.start(mTextureView);
            if (mOverlay != null) {
                android.util.Size size = mCameraSource.getVideoSize();
                Log.i(TAG, "startIfReady");
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                if (isPortraitMode()) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees
                    //TODO fix CameraFacing ... getCameraFacing
                    mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                } else {
                    mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                }
                mOverlay.clear();
            }
            mStartRequested = false;
        }
    }

    private class SurfaceTextureCallback implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            //Log.i(TAG, "onSurfaceTextureAvailable...");
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            //Log.i(TAG, "onSurfaceTextureDestroyed...");
            mSurfaceAvailable = false;
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.i(TAG, "onSurfaceTextureSizeChanged...");
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.i(TAG, "onSurfaceTextureUpdated...");
        }

    }

    /*unused*/
    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if (mCameraSource != null) {
            Size size = mCameraSource.getVideoSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
                Log.i("CameraSource", "video width = " + width + " video height = " + height);
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;
        Log.i("CameraSource", "layoutWidth = " + layoutWidth + " layoutHeight = " + layoutHeight);
        // strategy is to fit the taller side
        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int) (((float) layoutWidth / (float) width) * height);

        //Jack +++
        // If height is too short using fit width, does fit height instead.
        if (childHeight < layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int) (((float) layoutHeight / (float) height) * width);
        }
        Log.i("CameraSource", "childWidth = " + childWidth + " childHeight  = " + childHeight);
        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout((layoutWidth - childWidth) / 2, (layoutHeight - childHeight) / 2,
                    (layoutWidth + childWidth) / 2, (layoutHeight + childHeight) / 2);
        }
        //Jack ---

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }

}
