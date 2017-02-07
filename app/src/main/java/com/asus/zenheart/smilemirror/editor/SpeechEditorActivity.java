package com.asus.zenheart.smilemirror.editor;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.asus.zenheart.smilemirror.R;

public class SpeechEditorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor_main);
        // The main fragment view.
        SpeechListFragment speechListFragment = new SpeechListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechListFragment);
        fragmentTransaction.commit();
    }
}
