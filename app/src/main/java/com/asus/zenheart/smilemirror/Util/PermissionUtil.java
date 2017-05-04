package com.asus.zenheart.smilemirror.Util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.R;

public class PermissionUtil {
    private static final String LOG_TAG = "PermissionUtil";
    // permission request codes need to be < 256
    public static final int RC_HANDLE_VIDEO_PERM = 2;
    public static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    /**
     * Used for {@link #showAppInfoPage(Context)}
     */
    public static final String ARG_PACKAGE_NAME = "package";
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String APP_INFO_CLASS_NAME = "com.android.settings.applications.InstalledAppDetails";
    public static final String EXTRA_KEY_RIPPLE_PREF = ":settings:fragment_args_key";
    public static final String EXTRA_KEY_RIPPLE_TIMES = ":settings:fragment_args_key_highlight_times";
    public static final String SETTINGS_PERMISSION_KEY = "permission_settings";

    /**
     * Check if the activity or application has the permissions granted by users
     *
     * @param context     Context referenced to Activity
     * @param permissions RunTime permissions
     * @return false when all of the permissions is not granted
     */
    public static boolean hasPermissions(@NonNull Context context, @NonNull String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check the grantResults given by the user
     *
     * @param grantResults The RunTime permission results given by the user
     * @return false when all of the permissions is not granted
     */
    public static boolean checkPermissions(@NonNull int grantResults[]) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void requestPermission(@NonNull Context context, @NonNull String[] permissions,
            @IntRange(from = 0) int requestCode) {
        Activity activity;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            LogUtils.e(LOG_TAG, "context should be instance of Activity !!!");
            return;
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    //TODO: delete in the future if not used when the UX flow is determined
    public static boolean shouldShowRequestPermissionsRationale(@NonNull Context context,
            @NonNull String[] permissions) {
        Activity activity;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            LogUtils.e(LOG_TAG, "context should be instance of Activity !!!");
            return true;
        }
        LogUtils.i(LOG_TAG, " permissions = " + permissions.length);
        for (String permission : permissions) {
            LogUtils.i(LOG_TAG, " per  = " + ActivityCompat
                    .shouldShowRequestPermissionRationale(activity, permission));
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                //return false;

            }
        }
        return true;
    }

    /**
     * Add permission page to guide user to the setting page to give authority
     *
     * @param context       The reference to the activity or application
     * @param container     The container on which the permission will be showed
     * @param titleTextId   The string id of the page title
     * @param contentTextId The string id of the page content
     * @param layoutId      The layout id of the permission xml
     * @return The permission page view
     */
    public static View addPermissionPage(@NonNull final Context context,
            @NonNull final ViewGroup container, @StringRes int titleTextId,
            @StringRes int contentTextId,
            @LayoutRes int layoutId) {
        Resources resources = context.getResources();
        final View psView = LayoutInflater.from(context).inflate(layoutId, container, false);
        TextView titleView = (TextView) psView.findViewById(R.id.permission_agreement_title);
        titleView.setText(resources.getText(titleTextId));
        TextView contentView = (TextView) psView.findViewById(R.id.permission_agreement_content);
        contentView.setText(resources.getText(contentTextId));
        Button button = (Button) psView.findViewById(R.id.permission_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ifNeverSayAgain(context)) {
                    showAppInfoPage(context);
                } else {
                    requestPermission(context, VIDEO_PERMISSIONS, RC_HANDLE_VIDEO_PERM);
                }
            }
        });
        psView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //to absorb the touch event to intercept dispatch
                return true;
            }
        });
        container.addView(psView);
        return psView;
    }

    /**
     * Show AppInfoPage in setting
     *
     * @param context The reference to the activity or application
     */
    public static void showAppInfoPage(@NonNull Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(SETTINGS_PACKAGE_NAME, APP_INFO_CLASS_NAME);
        intent.putExtra(ARG_PACKAGE_NAME, context.getPackageName());
        intent.putExtra(EXTRA_KEY_RIPPLE_PREF, SETTINGS_PERMISSION_KEY);
        intent.putExtra(EXTRA_KEY_RIPPLE_TIMES, 3);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean ifNeverSayAgain(@NonNull Context context) {
        return PrefsUtils.getBooleanPreference(context, PrefsUtils.PREFS_IF_NEVER_SAY_AGAIN, false);
    }

    public static void setIfNeverSayAgain(@NonNull Context context,boolean bool) {
        PrefsUtils.setBooleanPreference(context, PrefsUtils.PREFS_IF_NEVER_SAY_AGAIN, bool);
    }
}
