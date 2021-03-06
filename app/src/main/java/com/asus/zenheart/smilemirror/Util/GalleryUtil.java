package com.asus.zenheart.smilemirror.Util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.asus.zenheart.smilemirror.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GalleryUtil {
    private static final String LOG_TAG = "GalleryUtil";
    private static final String RECORDING_PATH = Environment.getExternalStorageDirectory().getPath()
            + "/ZenHeart/SmileMirror/AutoRecording/";
    private static final String ASUS_GALLERY_PACKAGE_NAME = "com.asus.gallery";
    private static final int THUMBNAIL_BROKE_LENGTH = 3;
    private static final int THUMBNAIL_CORNER_SIZE = 3;

    private static String[] getVideoFiles() {
        File folder = new File(RECORDING_PATH);
        String [] allFiles = folder.list();
        if (allFiles == null || allFiles.length == 0) {
            return null;
        }
        return allFiles;
    }

    public static int getVideoFileNumbers() {
        String[] allFiles = getVideoFiles();

        if (allFiles == null) {
            return 0;
        }
        return allFiles.length;
    }

    public static String getLastVideoName() {
        String[] allFiles = getVideoFiles();

        if (allFiles == null) {
            return null;
        }
        return allFiles[allFiles.length - 1];
    }

    public static String getLastVideoPath() {
        return RECORDING_PATH + getLastVideoName();
    }

    public static String getVideoFileName() {
        File file = new File(RECORDING_PATH);
        if (file.exists() || file.mkdirs()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("'CV'_yyyyMMdd_HHmmss", Locale.US);
            String date = dateFormat.format(new java.util.Date());
            return String.format("%s%s.mp4", RECORDING_PATH, date);
        } else {
            Log.e(LOG_TAG, "Fail to make dir at " + file + "... return default ExternalDirectory ");
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    private static String getVideoBucketId(@NonNull Context context) {
        final String fileName = "AutoRecording";
        return getBucketId(context, fileName);
    }

    private static String getBucketId(@NonNull Context context, @NonNull String fileName) {
        String bucketId = "";

        final String[] projection = new String[]{"DISTINCT " +
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME + ", " +
                MediaStore.Video.Media.BUCKET_ID};

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                final String bucketName = cursor.getString(
                        (cursor.getColumnIndex(
                                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME)));
                if (bucketName.equals(fileName)) {
                    bucketId = cursor.getString(
                            (cursor.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_ID)));
                    break;
                }
            }
        }
        return bucketId;
    }


    public static void sendMediaScanIntent(@NonNull Context context, String filePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(mediaScanIntent);
    }

    public static void intentToGallery(@NonNull Context context) {
        if (!pathIsExist(RECORDING_PATH) || getVideoFileNumbers() == 0) {
            showTheToast(context, context.getString(R.string.sm_recording_folder_miss_toast));
            return;
        }
        Uri mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String bucketId = getVideoBucketId(context);
        if (bucketId.length() > 0) {
            mediaUri = mediaUri.buildUpon()
                    .appendQueryParameter("bucketId", bucketId)
                    .build();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, mediaUri);
        if (appIsInstalled(context, ASUS_GALLERY_PACKAGE_NAME)) {
            intent.setPackage(ASUS_GALLERY_PACKAGE_NAME);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showTheToast(context, context.getString(R.string.sm_gallery_disabled_toast));
            e.printStackTrace();
        }
    }

    private static void showTheToast(@NonNull Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private static boolean appIsInstalled(@NonNull Context context, String pkgName) {
        try {
            context.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean pathIsExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void mediaScan(@NonNull final Context context, String path) {
        MediaScannerConnection.scanFile(
                context,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        if (uri != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(uri);
                            context.startActivity(intent);
                        }
                    }
                });
    }

    public static Bitmap createVideoThumbnail(@NonNull Context context) {
        Bitmap bitmap = ThumbnailUtils
                .createVideoThumbnail(getLastVideoPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
        if (bitmap == null) {
            return null;
        }
        final int color = context.getResources()
                .getColor(R.color.editor_video_thumbnail_color, null);

        return BitmapUtil.getRoundedCornerBitmap(bitmap,
                color,
                THUMBNAIL_CORNER_SIZE,
                THUMBNAIL_BROKE_LENGTH,
                context);
    }
}
