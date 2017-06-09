package com.asus.zenheart.smilemirror.service;

import android.app.IntentService;
import android.content.Intent;

import com.asus.zenheart.smilemirror.Util.LogUtils;
import com.google.android.gms.vision.face.FaceDetector;

public class FaceDownloadService extends IntentService {
    private static final String LOG_TAG = "FaceDownloadService";

    public FaceDownloadService() {
        super(FaceDownloadService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        LogUtils.d(LOG_TAG,"trigger downloading library");
        triggerDownload();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(LOG_TAG,"FaceDownloadService is onDestroy");
        LogUtils.d(LOG_TAG,"FaceDownloadService kill self ... pid is "+android.os.Process.myPid());
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * This method is only used to trigger download.Therefore,we don't care about the isOperational
     * results
     */
    private void triggerDownload() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        detector.isOperational();
        detector.release();
    }
}
