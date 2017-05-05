package com.asus.zenheart.smilemirror.editor;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import static android.nfc.NfcAdapter.EXTRA_ID;

public class SpeechEditPageFragment extends Fragment {
    // type value 0 means user speech, others mean default speeches.
    private static final int USER_INPUT_TYPE = 0;

    private EditText mEditText;
    private Context mContext;
    private long mId;
    private String mTitle;
    private Bundle mBundle;
    private SpeechEditorActivity mActivity;

    private boolean mEditTextChanged = false;

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
        mBundle = getArguments();
        if (mContext instanceof SpeechEditorActivity) {
            mActivity = (SpeechEditorActivity) getActivity();
        }
        setHasOptionsMenu(true);

        initView(view);
        mActivity.showKeyboard();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mEditText.clearFocus();

                    if(getView() != null) {
                        View view = getView();
                        view.setFocusableInTouchMode(true);
                        view.requestFocus();
                        view.setOnKeyListener(new View.OnKeyListener() {
                            @Override
                            public boolean onKey(View v, int keyCode, KeyEvent event) {
                                // only need to get the KEYCODE_BACK.
                                if (keyCode == KeyEvent.KEYCODE_BACK &&
                                        event.getAction() == KeyEvent.ACTION_UP) {
                                    mActivity.backToSpeechListFragment();
                                    return true;
                                }
                                return false;
                            }
                        });
                    }
                }
                return false;
            }
        });
    }

    private void initView(@NonNull View view) {
        mEditText = (EditText) view.findViewById(R.id.editor_edit_page_editor);
        mEditText.requestFocus();
        if (mBundle != null && mBundle.getLong(EXTRA_ID) != 0) {
            mId = mBundle.getLong(EXTRA_ID);
            Uri uri = Uri.withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(mId));

            try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null) {
                    cursor.moveToFirst();
                    int type = cursor.getInt(cursor.getColumnIndex(SpeechContract.TYPE));
                    if (type ==0) {
                        mTitle = cursor.getString(cursor.getColumnIndex(SpeechContract.TITLE));
                        mEditText.setText(
                                cursor.getString(cursor.getColumnIndex(SpeechContract.CONTENT)));
                    } else {
                        mTitle = setSampleSpeechTitle(type);
                        mEditText.setText(setSampleSpeechContent(type));
                    }
                    mEditText.setSelection(mEditText.getText().length());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mEditText.getText().length() == 0) {
            mEditText.setHint(R.string.editor_content_hint);
            mEditText.setSelection(0);
        }
        final Toolbar toolBar = (Toolbar) view.findViewById(R.id.editor_edit_page_toolbar);
        ((AppCompatActivity) mContext).setSupportActionBar(toolBar);
        toolBar.setTitle(mContext.getString(R.string.editor_edit_script));
        toolBar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        toolBar.setNavigationIcon(R.drawable.back);
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditTextChanged) {
                    saveCheckDialog();
                } else {
                    mActivity.hideKeyboard();
                    mActivity.backToSpeechListFragment();
                }
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (toolBar.getMenu().findItem(R.id.editor_edit_menu_save) != null) {
                    toolBar.getMenu().findItem(R.id.editor_edit_menu_save).setIcon(R.drawable.save);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                mEditTextChanged = s.length() != 0;
            }
        });
    }

    private String setSampleSpeechTitle(int type) {
        if (type == 1) {
            return mContext.getString(R.string.editor_example_one_title);
        } else if (type == 2) {
            return mContext.getString(R.string.editor_example_two_title);
        } else if (type == 3) {
            return mContext.getString(R.string.editor_example_three_title);
        } else {
            return mContext.getString(R.string.editor_example_four_title);
        }
    }

    private String setSampleSpeechContent(int type) {
        if (type == 1) {
            return mContext.getString(R.string.editor_example_one_content);
        } else if (type == 2) {
            return mContext.getString(R.string.editor_example_two_content);
        } else if (type == 3) {
            return mContext.getString(R.string.editor_example_three_content);
        } else {
            return mContext.getString(R.string.editor_example_four_content);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editor_edit_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.editor_edit_menu_save) {
            if (mEditTextChanged && mEditText.getText().length() > 0) {
                showSaveFileDialog();
            }
        }
        return true;
    }

    private void showSaveFileDialog() {
        final EditText titleEditText = new EditText(mContext);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        titleEditText.setBackgroundResource(R.drawable.asusres_edit_text_holo_dark);
        titleEditText.setTextColor(Color.WHITE);
        if (mTitle != null) {
            titleEditText.setText(mTitle);
            titleEditText.setSelection(mTitle.length());
            titleEditText.setSelectAllOnFocus(true);
        } else {
            titleEditText.setText(R.string.editor_title_hint);
            titleEditText.setSelection(titleEditText.getText().length());
            titleEditText.setSelectAllOnFocus(true);
        }
        builder.setView(titleEditText);
        builder.setTitle(R.string.editor_save_new_file_title);
        builder.setPositiveButton(R.string.editor_save_file_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mTitle = titleEditText.getText().toString();
                        String mContent = mEditText.getText().toString();
                        ContentValues values = new ContentValues();
                        Uri uri;
                        if (!mTitle.isEmpty()) {
                            values.put(SpeechContract.TYPE, USER_INPUT_TYPE);
                            values.put(SpeechContract.DATE,
                                    mActivity.getTime());
                            values.put(SpeechContract.TITLE, mTitle);
                            values.put(SpeechContract.CONTENT, mContent);
                            if (mId > 0) {
                                uri = Uri.withAppendedPath(SpeechContract.SPEECH_URI,
                                        String.valueOf(mId));
                                mContext.getContentResolver().update(uri, values, null, null);
                            } else {
                                // New file
                                uri = mContext.getContentResolver()
                                        .insert(SpeechContract.SPEECH_URI, values);
                            }
                            int id = Integer.valueOf(uri != null ? uri.getLastPathSegment() : null);
                            mActivity.hideKeyboard();
                            mActivity.backToSpeechBrowseFragment(id);
                        } else {
                            Toast toast = Toast.makeText(mContext,
                                    mContext.getResources()
                                            .getString(R.string.editor_save_file_hint),
                                    Toast.LENGTH_LONG);
                            toast.show();
                        }
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(R.string.editor_check_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        Window alertWindow = alertDialog.getWindow();
        if (alertWindow != null) {
            alertWindow.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    private void saveCheckDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.editor_modify_file_title);
        builder.setMessage(R.string.editor_save_file_description);
        builder.setPositiveButton(R.string.editor_save_file_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showSaveFileDialog();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(R.string.editor_check_discard,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.hideKeyboard();
                        SpeechListFragment speechListPageFragment = new SpeechListFragment();
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager
                                .beginTransaction();
                        fragmentTransaction.replace(R.id.fragment, speechListPageFragment);
                        fragmentTransaction.commit();
                        dialog.dismiss();
                    }
                });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}

