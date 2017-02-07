package com.asus.zenheart.smilemirror.editor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DbHelper extends SQLiteOpenHelper {

    DbHelper(Context context) {
        super(context, "dbNotes", null, 1);
    }

    /**
     * Store conversation scripts in this database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SpeechContract.SPEECH_TABLE + " (" +
                SpeechContract._ID + " INTEGER NOT NULL PRIMARY KEY, " +
                SpeechContract.TYPE + " INTEGER, " +
                SpeechContract.DATE + " TEXT NOT NULL, " +
                SpeechContract.TITLE + " TEXT NOT NULL, " +
                SpeechContract.CONTENT + " TEXT)");
    }

    /**
     * Upgrade the database version and the table
     * into the database.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Kills the table and existing data
        db.execSQL("DROP TABLE IF EXISTS notes");
        // Recreates the database with a new version
        onCreate(db);
    }
}
