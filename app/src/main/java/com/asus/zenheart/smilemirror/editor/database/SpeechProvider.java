package com.asus.zenheart.smilemirror.editor.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

public class SpeechProvider extends ContentProvider {

    public static final int SPEECH = 1;
    public static final int SPEECH_ID = 2;
    private static final String UNIMPLEMENTED_DESCRIPTION = "Not yet implemented";
    private static final String INVALID_URI_DESCRIPTION = "Not yet implemented";

    private UriMatcher mUriMatcher;
    private DbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(SpeechContract.AUTHORITY, "msgs", SPEECH);
        mUriMatcher.addURI(SpeechContract.AUTHORITY, "msgs/#", SPEECH_ID);

        mDbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException(UNIMPLEMENTED_DESCRIPTION);
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (mUriMatcher.match(uri) == SPEECH) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            long id = db.insert(SpeechContract.SPEECH_TABLE, null, values);
            Uri insertUri = Uri.withAppendedPath(SpeechContract.BASE_URI, String.valueOf(id));
            db.close();
            notifyChanges(uri);
            return insertUri;
        } else {
            throw new UnsupportedOperationException(UNIMPLEMENTED_DESCRIPTION);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (mUriMatcher.match(uri) == SPEECH_ID) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            int modifiedLines = db.delete(SpeechContract.SPEECH_TABLE,
                    SpeechContract._ID + " = ?",
                    new String[]{uri.getLastPathSegment()});
            db.close();
            notifyChanges(uri);
            return modifiedLines;

        } else {
            throw new UnsupportedOperationException(INVALID_URI_DESCRIPTION);
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        if (mUriMatcher.match(uri) == SPEECH_ID) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            int modifiedLines = db.update(SpeechContract.SPEECH_TABLE,
                    values,
                    SpeechContract._ID + " = ?",
                    new String[]{uri.getLastPathSegment()});
            db.close();
            notifyChanges(uri);
            return modifiedLines;

        } else {
            throw new UnsupportedOperationException("Uri \n" + INVALID_URI_DESCRIPTION);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (mUriMatcher.match(uri) == SPEECH) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cursor = db.query(SpeechContract.SPEECH_TABLE,
                    projection, selection, selectionArgs, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;

        } else if (mUriMatcher.match(uri) == SPEECH_ID) {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            Cursor cursor = db.query(SpeechContract.SPEECH_TABLE,
                    projection,
                    SpeechContract._ID + " = ?",
                    new String[]{uri.getLastPathSegment()}, null, null, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;

        } else {
            throw new UnsupportedOperationException(UNIMPLEMENTED_DESCRIPTION);
        }
    }

    private void notifyChanges(Uri uri) {
        if (getContext() != null && getContext().getContentResolver() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }
}