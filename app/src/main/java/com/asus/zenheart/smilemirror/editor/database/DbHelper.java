package com.asus.zenheart.smilemirror.editor.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.asus.zenheart.smilemirror.R;

class DbHelper extends SQLiteOpenHelper {
    private static final long DEFAULT_DATE = 0;
    private Context mContext;

    DbHelper(Context context) {
        super(context, "dbNotes", null, 1);
        mContext = context;
    }

    /**
     * Store conversation scripts in this database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + SpeechContract.SPEECH_TABLE + " (" +
                SpeechContract._ID + " INTEGER NOT NULL PRIMARY KEY, " +
                SpeechContract.TYPE + " INTEGER, " +
                SpeechContract.DATE + " LONG NOT NULL, " +
                SpeechContract.TITLE + " TEXT NOT NULL, " +
                SpeechContract.CONTENT + " TEXT)");

        db.execSQL("INSERT INTO " + SpeechContract.SPEECH_TABLE + " VALUES(" +
                "1, 1, '" + DEFAULT_DATE + "', '" +
                mContext.getString(R.string.editor_example_one_title) + "', '" +
                "1" + "')");

        db.execSQL("INSERT INTO " + SpeechContract.SPEECH_TABLE + " VALUES(" +
                "2, 2, '" + DEFAULT_DATE + "', '" +
                mContext.getString(R.string.editor_example_two_title) + "', '" +
                "2" + "')");

        db.execSQL("INSERT INTO " + SpeechContract.SPEECH_TABLE + " VALUES(" +
                "3, 3, '" + DEFAULT_DATE + "', '" +
                mContext.getString(R.string.editor_example_three_title) + "', '" +
                "3" + "')");

        db.execSQL("INSERT INTO " + SpeechContract.SPEECH_TABLE + " VALUES(" +
                "4, 4, '" + DEFAULT_DATE + "', '" +
                mContext.getString(R.string.editor_example_four_title) + "', '" +
                "4" + "')");
    }

    /**
     * Upgrade the database version and the table
     * into the database.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Kills the table and existing data
        db.execSQL("DROP TABLE IF EXISTS Speech");
        // Recreates the database with a new version
        onCreate(db);
    }
}
