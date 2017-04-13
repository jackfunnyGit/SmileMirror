package com.asus.zenheart.smilemirror.Util;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public class LogUtils {

    private static final String DEFAULT_TAG = "SmileMirror";
    private static final int MAX_TAG_LENGTH = 23;
    private static final int NON_LOGGABLE = -1;
    /**
     * If set true,loggable is compared to {@link #LOCAL_LOG_LEVEL}.
     * If set false,loggable is returned by {@link Log#isLoggable(String, int)}.
     */
    private static final boolean LOCAL_LOGGABLE = true;
    private static final int LOCAL_LOG_LEVEL = LogLevel.VERBOSE;

    public @interface LogLevel {
        int VERBOSE = Log.VERBOSE;
        int DEBUG = Log.DEBUG;
        int INFO = Log.INFO;
        int WARN = Log.WARN;
        int ERROR = Log.ERROR;
        int ASSERT = Log.ASSERT;
    }

    public static int v(@Nullable final String tag, final String msg) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.VERBOSE)) {
            return Log.v(fixedTag, msg);
        }
        return NON_LOGGABLE;
    }

    public static int v(@Nullable final String tag, final String msg, final Throwable tr) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.VERBOSE)) {
            return Log.v(fixedTag, msg, tr);
        }
        return NON_LOGGABLE;
    }

    public static int d(@Nullable final String tag, final String msg) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.DEBUG)) {
            return Log.d(fixedTag, msg);
        }
        return NON_LOGGABLE;
    }

    public static int d(@Nullable final String tag, final String msg, final Throwable tr) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.DEBUG)) {
            return Log.d(fixedTag, msg, tr);
        }
        return NON_LOGGABLE;
    }

    public static int i(@Nullable final String tag, final String msg) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.INFO)) {
            return Log.i(fixedTag, msg);
        }
        return NON_LOGGABLE;
    }

    public static int i(@Nullable final String tag, final String msg, final Throwable tr) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.INFO)) {
            return Log.i(fixedTag, msg, tr);
        }
        return NON_LOGGABLE;
    }

    public static int w(@Nullable final String tag, final String msg) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.WARN)) {
            return Log.w(fixedTag, msg);
        }
        return NON_LOGGABLE;
    }

    public static int w(@Nullable final String tag, final String msg, final Throwable tr) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.WARN)) {
            return Log.w(fixedTag, msg, tr);
        }
        return NON_LOGGABLE;
    }

    public static int e(@Nullable final String tag, final String msg) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.ERROR)) {
            return Log.e(fixedTag, msg);
        }
        return NON_LOGGABLE;
    }

    public static int e(@Nullable final String tag, final String msg, final Throwable tr) {
        final String fixedTag = fixTag(tag);
        if (isLoggable(fixedTag, LogLevel.ERROR)) {
            return Log.e(fixedTag, msg, tr);
        }
        return NON_LOGGABLE;
    }

    public static boolean isLoggable(@Nullable final String tag, @LogLevel final int level) {
        if(LOCAL_LOGGABLE){
            return level > LOCAL_LOG_LEVEL;
        }
        return Log.isLoggable(fixTag(tag), level);
    }

    private static String fixTag(@Nullable final String tag) {
        if (TextUtils.isEmpty(tag)) {
            return DEFAULT_TAG;
        }
        // The logging tag can be at most 23 characters
        // Therefore, below statement will trim tag to proper length
        return tag.length() > MAX_TAG_LENGTH ? tag.substring(0, MAX_TAG_LENGTH) : tag;
    }
}