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

package com.asus.zenheart.smilemirror;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.ui.camera.CameraSource;
import com.asus.zenheart.smilemirror.ui.camera.CameraSourcePreview;
import com.asus.zenheart.smilemirror.ui.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity implements
        ModePagerAdapter.ActivityCallback {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constant field
    private static final int[] INDEX_LAYOUT_OF_MODE = {R.layout.smile_mode, R.layout.coach_mode};
    private static final int MOVING_AVERAGE_NUMBER = 10;//it should be removed in the future
    public static final int MODE_SMILE = 0;
    public static final int MODE_COUCH = 1;
    // member field
    private Context mContext;
    private ViewPager mViewpager;
    private ModePagerAdapter mPagerAdapter;
    private HistogramChart mHistogramChart;
    private SmileIndicatorView mSmileIndicatorView;
    private TextView mToastTextView;
    //TODO change to local variable with callback in the future
    public static int sIfDraw = 0;

    // Show image_tutorial
    //TODO it should be change to preference in the future
    private static boolean sShowTutorial = true;


    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.mirror_main);
        //Jack ++
        initView();
        //TODO adjust in the future,set N directly in the AverageUtil
        AverageUtil.setMovingWindowSize(MOVING_AVERAGE_NUMBER);
        //Jack --

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    // Jack: init the ui object in the main activity.
    private void initView() {
        mContext = getApplicationContext();
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.face_overlay);
        mViewpager = (ViewPager) findViewById(R.id.viewpager);
        mToastTextView = (TextView) findViewById(R.id.toast_text);
        mHistogramChart = (HistogramChart) findViewById(R.id.panel_pie_chart);
        mHistogramChart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideHistogramChart();
                return true;
            }
        });
        mSmileIndicatorView = (SmileIndicatorView) findViewById(R.id.smile_indicator);
        mPagerAdapter = new ModePagerAdapter(this, INDEX_LAYOUT_OF_MODE, mHistogramChart);
        mViewpager.setAdapter(mPagerAdapter);
        mViewpager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    //  show the smile mode toast
                    mToastTextView.setVisibility(View.VISIBLE);
                    mToastTextView.setText(R.string.smile_mode);
                    mToastTextView.setCompoundDrawablesWithIntrinsicBounds(0,
                            R.drawable.smile_mode, 0, 0);
                    AnimationUtil.toastAnimation(mToastTextView);
                } else {
                    //  show the coach mode toast
                    mToastTextView.setVisibility(View.VISIBLE);
                    mToastTextView.setText(R.string.coach_mode);
                    mToastTextView.setCompoundDrawablesWithIntrinsicBounds(0,
                            R.drawable.conversation_mode, 0, 0);
                    AnimationUtil.toastAnimation(mToastTextView);

                }

            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                //when Viewpager scrolled to another page, onPageScrolled(position = 1,offset=0,
                // offsetPixels =0) will be invoked,which is supposed to be 100.It could be
                // tolerated to abandoned offset = 0
                if (positionOffset == 0) {
                    return;
                }
                //when ViewPager is scrolled a little, the face window is not drawn.
                //Therefore,small shift as 0.1 is set
                if (positionOffset > 0.1) {
                    sIfDraw = MODE_COUCH;
                } else {
                    sIfDraw = MODE_SMILE;
                }
                mSmileIndicatorView.setCirclePosition((int) (positionOffset * 100));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        addTutorialView();

    }
    // Jack +++

    /**
     * It depends on the preferences if show OOBE/help to the users at the first time
     */
    private void addTutorialView() {
        //TODO sShowTutorial should be change to preference in the future
        if (sShowTutorial) {
            final ImageView imageTutorial = new ImageView(mContext);
            imageTutorial.setImageResource(R.drawable.tutorial_page);
            final FrameLayout mainLayout = (FrameLayout) findViewById(R.id.mirror_framelayout);
            mainLayout.addView(imageTutorial);
            final Configuration configuration = mContext.getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                imageTutorial.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        v.setVisibility(View.GONE);
                        return true;
                    }
                });
                sShowTutorial = false;
            } else {
                imageTutorial.setVisibility(View.GONE);
            }
        }

    }
    // Jack ---

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(mContext, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        //Jack +++
        AnimationUtil.toastAnimation(mToastTextView);//mode Toast
        //Jack ---
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                mContext);
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay, mContext);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay, Context mContext) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, mContext);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


    // ++Jack: Show or hide pie chart.
    @Override
    public void hideHistogramChart() {
        mHistogramChart.setVisibility(View.INVISIBLE);
        mSmileIndicatorView.setVisibility(View.VISIBLE);
        final ImageView close = (ImageView) findViewById(R.id.image_close);
        close.setVisibility(View.VISIBLE);
        mViewpager.setVisibility(View.VISIBLE);
    }

    @Override
    public void showHistogramChart() {
        mHistogramChart.setVisibility(View.VISIBLE);
        mSmileIndicatorView.setVisibility(View.INVISIBLE);
        final ImageView close = (ImageView) findViewById(R.id.image_close);
        close.setVisibility(View.INVISIBLE);
        mViewpager.setVisibility(View.INVISIBLE);
    }

    // --Jack


}

