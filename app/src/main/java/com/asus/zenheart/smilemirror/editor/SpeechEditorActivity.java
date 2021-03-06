package com.asus.zenheart.smilemirror.editor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.R;

import java.util.Calendar;

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

    public long getTime() {
        return Calendar.getInstance().getTime().getTime();
    }

    public void hideKeyboard() {
        View focusViw = getCurrentFocus();
        try {
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            if ((focusViw != null) && (focusViw.getWindowToken() != null)) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(focusViw.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showKeyboard() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .toggleSoftInput(InputMethodManager.SHOW_FORCED,
                        InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard();
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, FaceTrackerActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void backToSpeechListFragment() {
        SpeechListFragment speechListPageFragment = new SpeechListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechListPageFragment);
        fragmentTransaction.commit();
    }

    public void backToSpeechBrowseFragment(long id) {
        SpeechBrowsePageFragment speechBrowsePageFragment = SpeechBrowsePageFragment.newInstance(id);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
        fragmentTransaction.commit();
    }
}
