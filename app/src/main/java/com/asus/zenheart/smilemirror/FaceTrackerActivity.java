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

import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.zenheart.smilemirror.GUIView.HistogramChartView;
import com.asus.zenheart.smilemirror.GUIView.ShiningImageView;
import com.asus.zenheart.smilemirror.GUIView.SmileIndicatorView;
import com.asus.zenheart.smilemirror.Util.AnimationUtil;
import com.asus.zenheart.smilemirror.Util.GalleryUtil;
import com.asus.zenheart.smilemirror.Util.LogUtils;
import com.asus.zenheart.smilemirror.Util.NetWorkUtil;
import com.asus.zenheart.smilemirror.Util.PermissionUtil;
import com.asus.zenheart.smilemirror.Util.PrefsUtils;
import com.asus.zenheart.smilemirror.VideoTexture.SmileVideoTextureView;
import com.asus.zenheart.smilemirror.service.FaceDownloadService;
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
import java.util.Locale;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity implements
        ModePagerAdapter.ActivityCallback, SensorEventListener {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_VIDEO_PERM = 2;

    // constant field
    private static final int PAGE_INDEX_SMILE_MODE = 0;
    private static final int PAGE_INDEX_COACH_MODE = 1;
    private static final int[] INDEX_LAYOUT_OF_MODE = {R.layout.smile_mode, R.layout.coach_mode};
    private static final float POSITION_OFFSET_NOT_DRAW = 0.01f;

    // Exit animation duration
    private static final int SHINING_ANIMATION_DURATION = 600;
    // download page progress bar time
    private static final int PROGRESSBAR_UPDATE_TIME = 2000;

    // member field
    private Context mContext;
    private BorderViewPager mViewpager;
    private ModePagerAdapter mPagerAdapter;
    private SmileIndicatorView mSmileIndicatorView;
    private TextView mToastTextView;
    private ViewGroup mContainer;
    private View mChartPage;
    private View mPermissionPage;
    private View mTutorialPage;
    private View mDownloadPage;

    private SensorManager mSensorManager;
    // Smile video TextureView
    private SmileVideoTextureView mVideoView;
    private ShiningImageView mShiningImageViews;
    private boolean mClearEffect = false;

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
        Log.d(TAG, "MainActivity is onCreate.....");
        setContentView(R.layout.mirror_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initView();
        checkFaceDetectorAvailable();

    }

    // Jack: init the ui object in the main activity.
    private void initView() {
        mContext = getApplicationContext();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mContainer = (ViewGroup) findViewById(android.R.id.content);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.face_overlay);
        // ShihJie: init the texture view which play the effect video.
        mVideoView = (SmileVideoTextureView) findViewById(R.id.video_view);
        mVideoView.setResourceId(mVideoView.getRandomEffect(), mVideoView.getCorrectShader());
        mViewpager = (BorderViewPager) findViewById(R.id.viewpager);
        mToastTextView = (TextView) findViewById(R.id.toast_text);
        mSmileIndicatorView = (SmileIndicatorView) findViewById(R.id.smile_indicator);
        mShiningImageViews = (ShiningImageView) findViewById(R.id.shining_view);

        mPagerAdapter = new ModePagerAdapter(this, INDEX_LAYOUT_OF_MODE, mContainer);
        mViewpager.setAdapter(mPagerAdapter);
        mViewpager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == PAGE_INDEX_SMILE_MODE) {
                    //  show the smile mode toast
                    if (!PrefsUtils.getBooleanPreference(mContext,
                            PrefsUtils.PREFS_SHOW_MAIN_TUTORIAL, true)) {
                        AnimationUtil.showToast(mToastTextView,
                                R.string.smile_mode, R.drawable.smile_mode);
                    }
                } else if (position == PAGE_INDEX_COACH_MODE) {
                    //  show the coach mode toast
                    if (!PrefsUtils.getBooleanPreference(mContext,
                            PrefsUtils.PREFS_SHOW_COACH_TUTORIAL, true)) {
                        AnimationUtil.showToast(mToastTextView,
                                R.string.coach_mode, R.drawable.conversation_mode);
                    }
                    addTutorialView(PrefsUtils.PREFS_SHOW_COACH_TUTORIAL,
                            R.layout.tutorial_coach_page);
                }

            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {
                // when Viewpager scrolled to another page, onPageScrolled(position = 1,offset=0,
                // offsetPixels =0) will be invoked,which is supposed to be 100.It could be
                // tolerated to abandoned offset = 0
                if (positionOffset == 0) {
                    return;
                }
                // when ViewPager is scrolled a little, the face window is expected to be not drawn.
                // Therefore,small shift is set by POSITION_OFFSET_NOT_DRAW valued at 0.01
                if (positionOffset > POSITION_OFFSET_NOT_DRAW) {
                    mGraphicOverlay.setMode(GraphicOverlay.Mode.CONVERSATION);
                } else {
                    mGraphicOverlay.setMode(GraphicOverlay.Mode.SMILE);
                }
                mSmileIndicatorView.setCirclePosition((int) (positionOffset * 100));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

                //TODO: for special UX flow,is there any better way to implement it ?
                if (mGraphicOverlay.getMode() == GraphicOverlay.Mode.CONVERSATION) {
                    if (state == 1) {
                        mViewpager.getChildAt(1).findViewById(R.id.video_image_view)
                                .setVisibility(View.INVISIBLE);
                    } else if (state == 0) {
                        mViewpager.getChildAt(1).findViewById(R.id.video_image_view)
                                .setVisibility(View.VISIBLE);
                    }
                }

            }
        });

        addTutorialView(PrefsUtils.PREFS_SHOW_MAIN_TUTORIAL, R.layout.tutorial_main_page);

    }

    // Jack +++

    /**
     * It depends on the preferences if show OOBE/help to the users at the first time
     */
    private void addTutorialView(final String prefKey, final int layoutId) {

        if (!PrefsUtils.getBooleanPreference(mContext, prefKey, true)) {
            return;
        }
        mTutorialPage = LayoutInflater.from(mContext)
                .inflate(layoutId, mContainer, false);
        ImageView imageView = (ImageView) mTutorialPage.findViewById(R.id.tutorial_close_image);
        if (imageView == null) {
            return;
        }
        if (layoutId == R.layout.tutorial_main_page) {
            hideGuiElement();
        } else {
            mSmileIndicatorView.setVisibility(View.INVISIBLE);
            addPrefixNumber(R.id.tutorial_text_script_list, 1, mTutorialPage);
            addPrefixNumber(R.id.tutorial_text_start_practice, 2, mTutorialPage);
            addPrefixNumber(R.id.tutorial_text_review_video, 3, mTutorialPage);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefsUtils.setBooleanPreference(mContext, prefKey, false);
                mContainer.removeView(mTutorialPage);
                mTutorialPage = null;
                if (layoutId == R.layout.tutorial_main_page) {
                    showGuiElement();
                    showTitleToast();
                    AnimationUtil.showToast(mToastTextView,
                            R.string.coach_mode, R.drawable.conversation_mode);
                } else if (layoutId == R.layout.tutorial_coach_page) {
                    mSmileIndicatorView.setVisibility(View.VISIBLE);
                    AnimationUtil.showToast(mToastTextView,
                            R.string.coach_mode, R.drawable.conversation_mode);
                }
            }

        });
        mTutorialPage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //absorb user's touch event to prevent unexpected touch
                return true;
            }
        });
        mContainer.addView(mTutorialPage);
    }

    private void addPrefixNumber(int textId, int prefixNumber, View view) {
        final TextView textView = (TextView) view.findViewById(textId);
        textView.setText(String.format(Locale.US, "%d.%s", prefixNumber, textView.getText()));
    }

    /**
     * add chart page to show the statistics of smile value
     *
     * @param smileDegreeCounter The counter containing the statistical data
     */
    private void addChartPageView(SmileDegreeCounter smileDegreeCounter, String timeText) {
        mChartPage = LayoutInflater.from(mContext).inflate(R.layout.chart_page, mContainer, false);
        ImageView closeView = (ImageView) mChartPage.findViewById(R.id.chart_close_view);
        HistogramChartView chartView = (HistogramChartView) mChartPage
                .findViewById(R.id.histogram_chart);
        TextView textView = (TextView) mChartPage.findViewById(R.id.duration_text_view);
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideChartPage();
            }
        });
        chartView.setData(smileDegreeCounter.getSmileCountsPercent());
        textView.setText(
                String.format("%s%s", mContext.getString(R.string.chart_page_smile_duration),
                        timeText));
        // Open Video file
        ImageView videoIntentView = (ImageView) mChartPage.findViewById(R.id.video_intent_view);
        if (PrefsUtils.getBooleanPreference(mContext, PrefsUtils.PREFS_AUTO_RECORDING, true)) {
            Bitmap videoThumbnail = GalleryUtil.createVideoThumbnail(mContext);
            videoIntentView.setImageBitmap(videoThumbnail);
            videoIntentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GalleryUtil.mediaScan(mContext, GalleryUtil.getLastVideoPath());
                }
            });
        }
        mContainer.addView(mChartPage);
    }

    /**
     * add a download page to tell the user to turn on the internet when the face library is
     * not downloaded yet
     */
    private void addDownloadPage() {
        mDownloadPage = LayoutInflater.from(mContext)
                .inflate(R.layout.download_page, mContainer, false);
        Button button = (Button) mDownloadPage.findViewById(R.id.restart_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetWorkUtil.hasInternet(mContext)) {
                    startService(new Intent(mContext, FaceDownloadService.class));
                    final ProgressDialog progressDialog = ProgressDialog
                            .show(FaceTrackerActivity.this, null,
                                    mContext.getString(R.string.sm_progressbar_text));
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            if (mDownloadPage != null) {
                                mContainer.removeView(mDownloadPage);
                                mDownloadPage = null;
                            }
                        }
                    }, PROGRESSBAR_UPDATE_TIME);
                } else {
                    Toast toast = Toast.makeText(mContext,
                            mContext.getString(R.string.sm_need_network_toast), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        mDownloadPage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mContainer.addView(mDownloadPage);
    }

    /**
     * remove chartPage from the root container after user touch the close view
     */
    private void removeChartPageView() {
        if (mChartPage != null) {
            mContainer.removeView(mChartPage);
            mChartPage = null;
        }
    }

    private void hideGuiElement() {
        mSmileIndicatorView.setVisibility(View.INVISIBLE);
        mViewpager.setVisibility(View.INVISIBLE);
        mGraphicOverlay.setVisibility(View.INVISIBLE);
    }

    private void hideGuiElementInCoach() {
        mSmileIndicatorView.setVisibility(View.INVISIBLE);
        mPagerAdapter.hideGuiElementInCoach();
    }

    private void showGuiElement() {
        mSmileIndicatorView.setVisibility(View.VISIBLE);
        mViewpager.setVisibility(View.VISIBLE);
        mGraphicOverlay.setVisibility(View.VISIBLE);
        mPagerAdapter.showGuiElement();
    }

    private void refreshViewContent() {
        mPagerAdapter.refreshViewContent();
    }

    private void resetGuiElementState() {
        if (mTutorialPage != null) {
            //tutorial page is covered so resetGUi is not needed
            return;
        }
        if (mChartPage != null) {
            //chart page is covered so resetGUi is not needed
            return;
        }
        mPagerAdapter.resetGuiElementState();
        mViewpager.setSwipeEnabled(true);
        showGuiElement();
    }

    /**
     * Check if faceDetector is available. If isOperational return true,do nothing.
     * If not,wait for library downloading with network when user uses smileMirror.
     * Otherwise,show Download page.
     */
    private void checkFaceDetectorAvailable() {
        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        LogUtils.d(TAG, "faceDetector is isOperational =" + detector.isOperational()
                + " hasInternet " + NetWorkUtil.hasInternet(mContext));
        if (detector.isOperational() || NetWorkUtil.hasInternet(mContext)) {
            detector.release();
            return;
        }
        //faceDetector download is not ready and network is not working
        addDownloadPage();
    }
    // Jack ---

    /**
     * Handles the requesting of the camera,storage and microphone permission.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    //In reality,this app is wrapped into zenheart.Therefore,it must have permission when
    //user enters the app.This section should not be entered.
    private void requestPermissions() {
        Log.w(TAG, "Video permission is not granted. Show permission page to request permissions");
        if (mPermissionPage == null) {
            mPermissionPage = PermissionUtil.addPermissionPage(this, mContainer,
                    R.string.sm_permission_title,
                    R.string.sm_permission_content,
                    R.layout.permission);
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        if (mCameraSource != null) {
            return;
        }
        FaceDetector detector = createFaceDetector();
        //TODO it should be modified because CameraSource is not needed  for CameraAPI
        mCameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .build();
    }

    private FaceDetector createFaceDetector() {
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
            // TODO:should show dialog to tell user to update the google service to the latest
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }
        return detector;
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "mainActivity onResume ......");
        //Jack +++
        //check permission here in case that user granted permission in settings and
        //return back smileMirror
        if (PermissionUtil.hasPermissions(this, PermissionUtil.VIDEO_PERMISSIONS)) {
            if (mPermissionPage != null) {
                mContainer.removeView(mPermissionPage);
                //user granted all the needed permission,so reset pref_NeverSayAgain to false
                PermissionUtil.setIfNeverSayAgain(mContext, false);
            }
            mPermissionPage = null;
            createCameraSource();
        } else {
            requestPermissions();
        }
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        AnimationUtil.toastAnimation(mToastTextView);//mode Toast
        //Jack ---
        startCameraSource();

        if (mChartPage != null) {
            if (GalleryUtil.getLastVideoName() != null && mCameraSource
                    .getNextVideoPath() != null) {
                if (!mCameraSource.getNextVideoPath().contains(GalleryUtil.getLastVideoName())
                        || GalleryUtil.getVideoFileNumbers() == 0) {
                    mChartPage.findViewById(R.id.video_intent_view).setVisibility(View.INVISIBLE);
                }
            } else {
                mChartPage.findViewById(R.id.video_intent_view).setVisibility(View.INVISIBLE);
            }
        }

        if (mClearEffect) {
            if (mVideoView != null && mVideoView.isAvailable()) {
                mVideoView.setResourceId(mVideoView.getBlackBufferEffect(),
                        mVideoView.getCorrectShader());
                mVideoView.initMediaPlayer();
                mVideoView.playMediaPlayer();
                mClearEffect = false;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        refreshViewContent();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "mainActivity is onPause..........");
        resetGuiElementState();
        mSensorManager.unregisterListener(this);
        mPreview.stop();
        if (mVideoView != null) {
            mVideoView.stopMediaPlayerAndRenderer();
            mClearEffect = true;
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreview.release();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode != RC_HANDLE_VIDEO_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        for (int i = 0, len = permissions.length; i < len; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                // user rejected the permission
                boolean showRationale = shouldShowRequestPermissionRationale(permission);
                if (!showRationale) {
                    // user also CHECKED "never ask again",so set pref_NeverSayAgain to true
                    PermissionUtil.setIfNeverSayAgain(mContext, true);
                }
            }
        }
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
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }
        final TextureView textureView = (TextureView) findViewById(R.id.texture_view);

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay, textureView);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        } else {
            Log.d(TAG, "mCameraSource = null");
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
            mFaceGraphic = new FaceGraphic(overlay, mContext, mPreview, mVideoView);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
            mFaceGraphic.updateTextureView(mVideoView);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
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

    @Override
    public void hideChartPage() {
        showGuiElement();
        removeChartPageView();
    }

    @Override
    public void showChartPage(String timeText) {
        hideGuiElement();
        final GraphicOverlay.Graphic graphic = mGraphicOverlay.getGraphic();
        if (graphic == null) {
            addChartPageView(new SmileDegreeCounter.Builder().create(), timeText);
        } else {
            addChartPageView(graphic.getSmileDegreeCounter(), timeText);
        }
    }

    @Override
    public void startRecord() {
        hideGuiElementInCoach();
        mViewpager.setSwipeEnabled(false);
        mGraphicOverlay.setRecordingState(true);
        mCameraSource.startRecord();
    }

    @Override
    public void stopRecord() {
        mViewpager.setSwipeEnabled(true);
        mGraphicOverlay.setRecordingState(false);
        mCameraSource.stopRecord();
    }

    @Override
    public void finishActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, SHINING_ANIMATION_DURATION);
        mShiningImageViews.setVisibility(View.VISIBLE);
        mShiningImageViews.playShiningEffect(SHINING_ANIMATION_DURATION);
    }

    @Override
    public void showTitleToast() {
        mPagerAdapter.showTitleToast();
    }

    @Override
    public void onBackPressed() {
        if (mChartPage != null) {
            hideChartPage();
        } else {
            finishActivity();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final int screenRotation = getScreenRotation(event);
        if (mPreview != null && mPreview.getScreenRotation() != screenRotation) {
            Log.i(TAG, "screen is rotated to  rotation =  " + screenRotation);
            mPreview.setScreenRotation(screenRotation);
            if (mCameraSource == null) {
                return;
            }
            mCameraSource.setScreenRotation(screenRotation);
            mCameraSource.setDetector(createFaceDetector());
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @CameraSource.ORIENTATION
    private int getScreenRotation(SensorEvent event) {
        final float xGravity = event.values[0];
        final float yGravity = event.values[1];
        //TODO: those may be refactored into effective way
        if (Math.abs(xGravity) < Math.abs(yGravity)) {
            //portrait mode
            if (yGravity > 0) {
                //Rotation = 0
                return Surface.ROTATION_0;
            } else {
                //Rotation = 180
                return Surface.ROTATION_180;
            }
        } else {
            //landscape mode
            if (xGravity > 0) {
                //Rotation = 90
                return Surface.ROTATION_90;
            } else {
                return Surface.ROTATION_270;
            }
        }

    }

}

