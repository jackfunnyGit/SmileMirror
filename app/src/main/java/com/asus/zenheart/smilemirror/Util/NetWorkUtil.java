package com.asus.zenheart.smilemirror.Util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

public class NetWorkUtil {
    private static final String LOG_TAG = "NetWorkUtil";

    public static boolean hasInternet(@NonNull Context context) {
        ConnectivityManager connManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            LogUtils.d(LOG_TAG, "NetWork is Connected");
            return true;
        } else {
            LogUtils.d(LOG_TAG, "NetWork is not Connected");
            return false;
        }
    }
}
