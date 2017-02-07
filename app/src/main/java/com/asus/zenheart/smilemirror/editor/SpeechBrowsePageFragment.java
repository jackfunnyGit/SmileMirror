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

    private VerticalScrollTextView mPresentText;
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private long mId;
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
        mSharedPreferences = mContext
                .getSharedPreferences(SpeechContract.SPEECH_SHARED_PREFERENCE_NAME
                        , MODE_PRIVATE);
        // setHasOptionsMenu support to create the tool bar.
        setHasOptionsMenu(true);
        initView(view);

        if (getArguments() != null && getArguments().getLong(EXTRA_ID) != 0) {
            mId = getArguments().getLong(EXTRA_ID);
            mTextSize = mSharedPreferences.getFloat(SpeechContract.CONTENT_TEXT_SIZE,
                    VerticalScrollTextView.TEXT_SIZE.DEFAULT);

            Uri uri = Uri.withAppendedPath(
                    SpeechContract.SPEECH_URI, String.valueOf(mId));
            Cursor cursor = mContext.getContentResolver()
                    .query(uri, null, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mPresentText.setText(cursor.getString(
                            cursor.getColumnIndex(SpeechContract.CONTENT)));
                    mPresentText.setTextSize(mTextSize);
                }
                cursor.close();
            }
            mPresentText.setTextScrollSpeed(getScrollingSpeed(mSharedPreferences.getInt(SpeechContract.MARQUEE_SCROLLING_SPEED,
                    VerticalScrollTextView.TEXT_SPEED.NORMAL)));
        }
        mPresentText.start();
        return view;
    }

    private void initView(@NonNull View view) {
        mPresentText = (VerticalScrollTextView) view.findViewById(R.id.presetText);

        Toolbar mToolBar = (Toolbar) view.findViewById(R.id.editor_browse_page_toolbar);
        ((AppCompatActivity) mContext).setSupportActionBar(mToolBar);
        mToolBar.setTitle(R.string.editor_edit_script);
        mToolBar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        mToolBar.setNavigationIcon(R.drawable.back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToSpeechListFragment();
            }
        });

        ImageView EditTextSizeView = (ImageView) view.findViewById(R.id.seekBarTextSize);
        EditTextSizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextSizeRadioDialog();
            }
        });
        SeekBar mMarqueeSeekBar = (SeekBar) view.findViewById(R.id.marqueeSeekBar);
        // 0 is the default value from SharedPreferences.getInt
        mMarqueeSeekBar
                .setProgress(mSharedPreferences.getInt(SpeechContract.MARQUEE_SCROLLING_SPEED, 0));
        mMarqueeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                mPresentText.setTextScrollSpeed(getScrollingSpeed(progress));
                mSharedPreferences.edit().putInt(SpeechContract.MARQUEE_SCROLLING_SPEED, progress)
                        .apply();
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
    }

    private int getScrollingSpeed(int progress) {
        int speed = VerticalScrollTextView.TEXT_SPEED.NORMAL;
        switch (progress) {
            case 0:
                speed = VerticalScrollTextView.TEXT_SPEED.SLOW;
                break;
            case 1:
                speed = VerticalScrollTextView.TEXT_SPEED.NORMAL;
                break;
            case 2:
                speed = VerticalScrollTextView.TEXT_SPEED.FAST;
                break;
            case 3:
                speed = VerticalScrollTextView.TEXT_SPEED.TURBO;
                break;
        }
        return speed;
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
        int i = item.getItemId();
        if (i == R.id.editor_browse_menu_edit_text) {
            SpeechEditPageFragment speechEditPageFragment = SpeechEditPageFragment.newInstance(mId);
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment, speechEditPageFragment);
            fragmentTransaction.commit();
        } else if (i == R.id.editor_browse_menu_next) {
            mSharedPreferences.edit().putLong(SpeechContract.SPEECH_ID, mId)
                    .apply();
            Intent intent = new Intent(mContext,
                    FaceTrackerActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getActivity().startActivity(intent);
        }
        return true;
    }

    private void showTextSizeRadioDialog() {
        CharSequence[] fontSizeText = {mContext.getString(R.string.font_size_large),
                mContext.getString(R.string.font_size_default), mContext.getString(
                R.string.font_size_small)};
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.font_size);

        int FontSizeItem;
        float preferenceFontSize = mSharedPreferences.getFloat(SpeechContract.CONTENT_TEXT_SIZE,
                VerticalScrollTextView.TEXT_SIZE.DEFAULT);

        if (preferenceFontSize == VerticalScrollTextView.TEXT_SIZE.BIG) {
            FontSizeItem = 0;
        } else if (preferenceFontSize == VerticalScrollTextView.TEXT_SIZE.DEFAULT) {
            FontSizeItem = 1;
        } else {
            FontSizeItem = 2;
        }

        builder.setSingleChoiceItems(fontSizeText, FontSizeItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                mTextSize = VerticalScrollTextView.TEXT_SIZE.BIG;
                                break;
                            case 1:
                                mTextSize = VerticalScrollTextView.TEXT_SIZE.DEFAULT;
                                break;
                            case 2:
                                mTextSize = VerticalScrollTextView.TEXT_SIZE.SMALL;
                                break;
                        }
                    }
                });
        builder.setPositiveButton(R.string.editor_save_file_title,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPresentText.setTextSize(mTextSize);
                        mPresentText.invalidate();
                        mSharedPreferences.edit()
                                .putFloat(SpeechContract.CONTENT_TEXT_SIZE, mTextSize).apply();
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
