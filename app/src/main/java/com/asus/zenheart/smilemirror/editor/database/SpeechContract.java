package com.asus.zenheart.smilemirror.editor.database;

import android.net.Uri;
import android.provider.BaseColumns;

public interface SpeechContract extends BaseColumns {

    String AUTHORITY = "com.asus.zenheart.smilemirror";
    Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
    Uri SPEECH_URI = Uri.withAppendedPath(BASE_URI, "msgs");

    /**
     * SPEECH_TABLE is the table name of the speech.
     * TYPE used to distinguish the speech is default or input, default speech is 0, input is 1.
     * DATE is the speech create time.
     * TITLE is the speech's title.
     * CONTENT is th speech's content
     */
    String SPEECH_TABLE = "Speech";
    String TYPE = "type";
    String DATE = "date";
    String TITLE = "title";
    String CONTENT = "content";


}
