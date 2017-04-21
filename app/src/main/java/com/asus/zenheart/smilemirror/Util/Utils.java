package com.asus.zenheart.smilemirror.Util;

import android.os.Build;
import android.util.Log;

public class Utils {
    public static boolean isRunningOnSpecifiedVersionOrHigher(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}
