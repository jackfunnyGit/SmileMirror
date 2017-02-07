package com.asus.zenheart.smilemirror.editor;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.nfc.NfcAdapter.EXTRA_ID;

public class SpeechEditPageFragment extends Fragment {

    private static final String TIME_FORMAT = "yyyy/MMM/DD a hh:mm";
    // type value 1 means user speech, 0 means default speech.
    private static final int INPUT_TYPE = 1;

    private EditText mEditText;
    private Context mContext;
    private long mId;
    private String mTitle;

    /**
     * Get the speech data from SpeechListFragment
     *
     * @param id the number which used to get the data from database.
     */
    public static SpeechEditPageFragment newInstance(long id) {
        Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_ID, id);

        SpeechEditPageFragment speechEditPageFragment = new SpeechEditPageFragment();
        speechEditPageFragment.setArguments(bundle);
        return speechEditPageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_edit_page_fragment, container, false);
        mContext = getContext();
        setHasOptionsMenu(true);

        initView(view);

        if (getArguments() != null && getArguments().getLong(EXTRA_ID) != 0) {
            mId = getArguments().getLong(EXTRA_ID);
            Uri uri = Uri.withAppendedPath(
                    SpeechContract.SPEECH_URI, String.valueOf(mId));

            Cursor cursor = mContext.getContentResolver()
                    .query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                mTitle = cursor.getString(cursor.getColumnIndex(SpeechContract.TITLE));
                mEditText.setText(cursor.getString(
                        cursor.getColumnIndex(SpeechContract.CONTENT)));
                cursor.close();
            }
        }
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        SpeechBrowsePageFragment speechBrowsePageFragment = SpeechBrowsePageFragment
                                .newInstance(mId);
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager
                                .beginTransaction();
                        fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
                        fragmentTransaction.commit();
                    }
                    return true;
                }
                return false;
            }
        });
        return view;
    }

    private void initView(View view) {
        mEditText = (EditText) view.findViewById(R.id.editor_edit_page_editor);
        Toolbar mToolBar = (Toolbar) view.findViewById(R.id.editor_edit_page_toolbar);
        ((AppCompatActivity) mContext).setSupportActionBar(mToolBar);
        mToolBar.setTitle(mContext.getString(R.string.editor_edit_script));
        mToolBar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        mToolBar.setNavigationIcon(R.drawable.back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeechBrowsePageFragment speechBrowsePageFragment = SpeechBrowsePageFragment
                        .newInstance(mId);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
                fragmentTransaction.commit();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editor_edit_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.editor_edit_menu_save) {
            saveSpeechDialog();
        }
        return true;
    }

    private String getDate() {
        DateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    private void saveSpeechDialog() {
        final Resources resources = mContext.getResources();
        final Dialog dialog = new Dialog(mContext);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.editor_fragment_check_dialog);
        dialog.show();

        dialog.setOnKeyListener(new Dialog.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                // TODO Need the better method to handle the key back event.
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                }
                return true;
            }
        });

        final EditText titleEditText = (EditText) dialog
                .findViewById(R.id.editor_dialog_enter_title_edit);
        TextView title = (TextView) dialog.findViewById(R.id.editor_save_dialog_title);
        TextView cancel = (TextView) dialog.findViewById(R.id.editor_dialog_cancel_text);
        TextView save = (TextView) dialog.findViewById(R.id.editor_dialog_save_text);

        titleEditText.setText(mTitle);
        title.setText(resources.getString(R.string.editor_save_new_file_title));
        cancel.setText(resources.getString(R.string.editor_check_cancel));
        save.setText(resources.getString(R.string.editor_save_file_title));

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mTitle = titleEditText.getText().toString();
                String mContent = mEditText.getText().toString();
                ContentValues values = new ContentValues();

                if (!mTitle.isEmpty()) {
                    values.put(SpeechContract.TYPE, INPUT_TYPE);
                    values.put(SpeechContract.DATE, getDate());
                    values.put(SpeechContract.TITLE, mTitle);
                    values.put(SpeechContract.CONTENT, mContent);
                    if (mId < 0) {
                        Uri uri = Uri
                                .withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(mId));
                        mContext.getContentResolver().update(uri, values, null, null);
                    } else {
                        // New file
                        mContext.getContentResolver().insert(SpeechContract.SPEECH_URI, values);
                    }
                    SpeechListFragment speechListPageFragment = new SpeechListFragment();
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.fragment, speechListPageFragment);
                    fragmentTransaction.commit();
                } else {
                    Toast toast = Toast.makeText(mContext,
                            resources.getString(R.string.editor_save_file_hint),
                            Toast.LENGTH_LONG);
                    toast.show();
                }
                dialog.dismiss();
            }
        });
    }
}

