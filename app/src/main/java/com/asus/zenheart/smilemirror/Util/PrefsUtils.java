package com.asus.zenheart.smilemirror.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class PrefsUtils {

    public static final String SHARED_PREFERENCE_NAME = "SmileMirrorPreference";
    public static final String PREFS_SHOW_MAIN_TUTORIAL = "showMainTutorial";
    public static final String PREFS_SHOW_COACH_TUTORIAL = "showCoachTutorial";
    public static final String PREFS_SPEECH_SCROLLING_SPEED = "scrollingSpeed";
    public static final String PREFS_SPEECH_TEXT_SIZE = "textSize";
    public static final String PREFS_SPEECH_ID = "speechId";
    public static final String PREFS_AUTO_RECORDING = "autoRecording";

    /**
     * Helper method to retrieve a String value from {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return The value from shared preferences, or null if the value could not be read.
     */
    public static String getStringPreference(Context context, String key) {
        String value = null;
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getString(key, "");
        }
        return value;
    }

    /**
     * Helper method to write a String value to {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return true if the new value was successfully written to persistent storage.
     */
    public static boolean setStringPreference(Context context, String key, String value) {
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null && !TextUtils.isEmpty(key)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            return editor.commit();
        }
        return false;
    }

    /**
     * Helper method to retrieve a float value from {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @param defaultValue A default to return if the value could not be read.
     * @return The value from shared preferences, or the provided default.
     */
    public static float getFloatPreference(Context context, String key, float defaultValue) {
        float value = defaultValue;
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getFloat(key, defaultValue);
        }
        return value;
    }

    /**
     * Helper method to write a float value to {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return true if the new value was successfully written to persistent storage.
     */
    public static boolean setFloatPreference(Context context, String key, float value) {
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putFloat(key, value);
            return editor.commit();
        }
        return false;
    }

    /**
     * Helper method to retrieve a long value from {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @param defaultValue A default to return if the value could not be read.
     * @return The value from shared preferences, or the provided default.
     */
    public static long getLongPreference(Context context, String key, long defaultValue) {
        long value = defaultValue;
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getLong(key, defaultValue);
        }
        return value;
    }

    /**
     * Helper method to write a long value to {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return true if the new value was successfully written to persistent storage.
     */
    public static boolean setLongPreference(Context context, String key, long value) {
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(key, value);
            return editor.commit();
        }
        return false;
    }

    /**
     * Helper method to retrieve an integer value from {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @param defaultValue A default to return if the value could not be read.
     * @return The value from shared preferences, or the provided default.
     */
    public static int getIntegerPreference(Context context, String key, int defaultValue) {
        int value = defaultValue;
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getInt(key, defaultValue);
        }
        return value;
    }

    /**
     * Helper method to write an integer value to {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return true if the new value was successfully written to persistent storage.
     */
    public static boolean setIntegerPreference(Context context, String key, int value) {
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, value);
            return editor.commit();
        }
        return false;
    }

    /**
     * Helper method to retrieve a boolean value from {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @param defaultValue A default to return if the value could not be read.
     * @return The value from shared preferences, or the provided default.
     */
    public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
        boolean value = defaultValue;
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            value = preferences.getBoolean(key, defaultValue);
        }
        return value;
    }

    /**
     * Helper method to write a boolean value to {@link SharedPreferences}.
     *
     * @param context a {@link Context} object.
     * @return true if the new value was successfully written to persistent storage.
     */
    public static boolean setBooleanPreference(Context context, String key, boolean value) {
        SharedPreferences preferences = context
                .getSharedPreferences(SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key, value);
            return editor.commit();
        }
        return false;
    }
}
