package com.asus.zenheart.smilemirror.editor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.VerticalScrollTextView;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import static android.content.Context.MODE_PRIVATE;

public class SpeechBrowsePageFragment extends Fragment {

    private static final String EXTRA_ID = "id";
    private static final SparseIntArray MARQUEE_ARRAY = new SparseIntArray() {
        {
            append(0, VerticalScrollTextView.TEXT_SPEED.SLOW);
            append(1, VerticalScrollTextView.TEXT_SPEED.SLOW);
            append(2, VerticalScrollTextView.TEXT_SPEED.NORMAL);
            append(3, VerticalScrollTextView.TEXT_SPEED.FAST);
            append(4, VerticalScrollTextView.TEXT_SPEED.TURBO);
        }
    };
    // TODO: SparseArray is not a better solution for searching float value, but hash map can not find the index of the value.
    private final SparseIntArray FONT_SIZE_ARRAY = new SparseIntArray() {
        {
            append(0, (int)VerticalScrollTextView.TEXT_SIZE.BIG);
            append(1, (int)VerticalScrollTextView.TEXT_SIZE.DEFAULT);
            append(2, (int)VerticalScrollTextView.TEXT_SIZE.SMALL);
        }
    };
    private VerticalScrollTextView mPresentText;
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private Bundle mBundle;
    private AppCompatActivity mActivity;
    private long mItemId;
    private float mTextSize;

    /**
     * Get the speech data from SpeechListFragment
     *
     * @param id the number which used to get the data from database.
     */
    public static SpeechBrowsePageFragment newInstance(long id) {
        Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_ID, id);

        SpeechBrowsePageFragment speechBrowsePageFragment = new SpeechBrowsePageFragment();
        speechBrowsePageFragment.setArguments(bundle);
        return speechBrowsePageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_browse_page_fragment, container, false);
        mContext = getContext();
        mBundle = getArguments();
        mSharedPreferences = mContext
                .getSharedPreferences(SpeechContract.SPEECH_SHARED_PREFERENCE_NAME
                        , MODE_PRIVATE);
        if (mContext instanceof AppCompatActivity) {
            mActivity = ((AppCompatActivity) mContext);
        }
        // setHasOptionsMenu support to create the tool bar.
        setHasOptionsMenu(true);
        initView(view);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPresentText != null) {
            mPresentText.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPresentText != null) {
            mPresentText.start();
        }
    }

    private void initView(@NonNull View view) {
        mPresentText = (VerticalScrollTextView) view.findViewById(R.id.presetText);

        Toolbar toolBar = (Toolbar) view.findViewById(R.id.editor_browse_page_toolbar);
        mActivity.setSupportActionBar(toolBar);
        toolBar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        toolBar.setNavigationIcon(R.drawable.back);
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToSpeechListFragment();
            }
        });

        ImageView editTextSizeView = (ImageView) view.findViewById(R.id.seekBarTextSize);
        editTextSizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextSizeRadioDialog();
            }
        });

        SeekBar marqueeSeekBar = (SeekBar) view.findViewById(R.id.marqueeSeekBar);
        marqueeSeekBar.setProgress(MARQUEE_ARRAY.indexOfValue(
                mSharedPreferences.getInt(SpeechContract.MARQUEE_SCROLLING_SPEED,
                        VerticalScrollTextView.TEXT_SPEED.NORMAL)));
        marqueeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int speed = MARQUEE_ARRAY.get(progress);
                mPresentText.setTextScrollSpeed(speed);
                mSharedPreferences.edit().putInt(
                        SpeechContract.MARQUEE_SCROLLING_SPEED, speed).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mPresentText.stop();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPresentText.start();
            }
        });

        // KeyCode Back action in this fragment
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // only need to get the KEYCODE_BACK.
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        backToSpeechListFragment();
                    }
                    return true;
                }
                return false;
            }
        });
        mItemId = mBundle.getLong(EXTRA_ID);
        if (mBundle != null && mItemId != 0) {
            mTextSize = mSharedPreferences.getFloat(SpeechContract.CONTENT_TEXT_SIZE,
                    VerticalScrollTextView.TEXT_SIZE.DEFAULT);

            Uri uri = Uri.withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(mItemId));
            try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        toolBar.setTitle(
                                cursor.getString(cursor.getColumnIndex(SpeechContract.TITLE)));
                        mPresentText.setText(cursor.getString(
                                cursor.getColumnIndex(SpeechContract.CONTENT)));
                        mPresentText.setTextSize(mTextSize);
                    }
                    cursor.close();
                }
                mPresentText.setTextScrollSpeed(
                        mSharedPreferences.getInt(SpeechContract.MARQUEE_SCROLLING_SPEED,
                                VerticalScrollTextView.TEXT_SPEED.NORMAL));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void backToSpeechListFragment() {
        SpeechListFragment speechListPageFragment = new SpeechListFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechListPageFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editor_browse_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.editor_browse_menu_edit_text) {
            SpeechEditPageFragment speechEditPageFragment = SpeechEditPageFragment.
                    newInstance(mItemId);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment, speechEditPageFragment);
            fragmentTransaction.commit();
        } else if (itemId == R.id.editor_browse_menu_next) {
            mSharedPreferences.edit().putLong(SpeechContract.SPEECH_ID, mItemId).apply();
            Intent intent = new Intent(mContext, FaceTrackerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return true;
    }

    private void showTextSizeRadioDialog() {
        final int preferenceFontSize = (int)mSharedPreferences.getFloat(
                SpeechContract.CONTENT_TEXT_SIZE, VerticalScrollTextView.TEXT_SIZE.DEFAULT);

        CharSequence[] fontSizeText = {mContext.getString(R.string.font_size_large),
                mContext.getString(R.string.font_size_default), mContext.getString(
                R.string.font_size_small)};
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.font_size);
        builder.setSingleChoiceItems(fontSizeText, FONT_SIZE_ARRAY.indexOfValue(preferenceFontSize),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int itemPosition) {
                        mTextSize = FONT_SIZE_ARRAY.get(itemPosition);
                        mPresentText.setTextSize(mTextSize);
                        mPresentText.invalidate();
                        mSharedPreferences.edit()
                                .putFloat(SpeechContract.CONTENT_TEXT_SIZE, mTextSize).apply();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton(R.string.editor_check_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }
}
