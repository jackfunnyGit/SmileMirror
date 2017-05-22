package com.asus.zenheart.smilemirror.editor;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import java.util.Formatter;
import java.util.List;

public class SpeechListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback {

    private static final int USER_INPUT_TYPE = 0;
    private SpeechCursorAdapter mAdapter;
    private FloatingActionButton mFloatingActionButton;
    private ActionMode mActionMode;
    private Context mContext;
    private AppCompatActivity mActivity;
    private String mTitle;
    private TextView mRemindText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_list_main, container, false);

        mContext = getContext();
        if (mContext instanceof AppCompatActivity) {
            mActivity = ((AppCompatActivity) mContext);
        }
        setHasOptionsMenu(true);
        initView(view);

        getLoaderManager().initLoader(0, null, this);
        return view;
    }

    private void initView(View view) {
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.editor_add_button);
        mRemindText = (TextView) view.findViewById(R.id.editor_add_item_remind);
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newSpeech();
            }
        });
        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.editor_list_toolbar);
        mActivity.setSupportActionBar(toolbar);
        getActivity().setTitle(R.string.editor_title_bar_text);

        toolbar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        toolbar.setNavigationIcon(R.drawable.back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, FaceTrackerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intent);
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new SpeechCursorAdapter(mContext, new SpeechCursorAdapter.itemClick() {
            @Override
            public void itemClick(Cursor cursor) {
                if (mActionMode != null) {
                    mActionMode.invalidate();
                    return;
                }
                mTitle = cursor.getString(cursor.getColumnIndex(SpeechContract.TITLE));
                goToSpeechBrowseFragment(cursor.getLong(cursor.getColumnIndex(SpeechContract._ID)));
            }
        }, new SpeechCursorAdapter.itemLongClick() {
            @Override
            public void itemLongClick(Cursor cursor) {
                mTitle = cursor.getString(cursor.getColumnIndex(SpeechContract.TITLE));
                if (mActionMode != null) {
                    mActionMode.invalidate();
                    return;
                }
                mActionMode = getActivity().startActionMode(SpeechListFragment.this);
            }
        });
        mAdapter.setHasStableIds(true);
        recyclerView.setAdapter(mAdapter);

    }

    private void goToSpeechBrowseFragment(long id) {
        SpeechBrowsePageFragment speechBrowsePageFragment = SpeechBrowsePageFragment.newInstance(id);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
        fragmentTransaction.commit();
    }

    private void newSpeech() {
        SpeechEditPageFragment speechEditPageFragment = new SpeechEditPageFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, speechEditPageFragment);
        fragmentTransaction.commit();
    }

    private void configureSwipe(RecyclerView mRecyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final int position = viewHolder.getLayoutPosition();
                removeData(position);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext, SpeechContract.SPEECH_URI, null, null, null,
                SpeechContract._ID);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.setCursor(data);

        // Need to decide add the remind text or not after load finished.
        if (mAdapter.getItemCount() == 0) {
            mRemindText.setVisibility(View.VISIBLE);
        } else {
            mRemindText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.setCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO: in this bar is empty, but maybe need the other features in the future.
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // TODO: in this action mode is empty, but maybe need the other features in the future.
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        mFloatingActionButton.setVisibility(View.GONE);
        MenuInflater inflater = actionMode.getMenuInflater();
        int selectedItem = mAdapter.getSelectedItemCount();
        if (selectedItem < 2) {
            menu.clear();
            inflater.inflate(R.menu.editor_list_action_mode, menu);
            if (selectedItem == 1) {
                menu.findItem(R.id.editor_action_menu_select_one).setVisible(true);
            }
            menu.findItem(R.id.editor_action_menu_select_more).setVisible(false);
        } else {
            menu.clear();
            inflater.inflate(R.menu.editor_list_action_mode, menu);
            menu.findItem(R.id.editor_action_menu_select_one).setVisible(false);
            menu.findItem(R.id.editor_action_menu_select_more).setVisible(true);
        }

        Formatter formatter = new Formatter();
        formatter.format(mContext.getString(R.string.editor_select_item), selectedItem);

        if (selectedItem == 0) {
            mActionMode.finish();
        } else {
            actionMode.setTitle(formatter.toString());
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        int selectedItem = mAdapter.getSelectedItemCount();
        if (itemId == R.id.editor_action_menu_select_more) {
            // do nothing
            return true;
        } else if (itemId == R.id.editor_action_menu_delete) {
            if (selectedItem == 0) {
                return true;
            }
            Formatter formatter = new Formatter().format("%d", selectedItem);
            showDeleteCheckDialog(mAdapter.getSelectedItem(), formatter.toString());
            return true;
        } else if (itemId == R.id.editor_action_menu_select_one) {
            showRenameCheckDialog(mAdapter.getSelectedItem());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.mActionMode = null;
        mAdapter.clearSelections();
        mFloatingActionButton.setVisibility(View.VISIBLE);
    }

    private void showRenameCheckDialog(final List<Integer> selectedItemPositions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final EditText titleEditText = new EditText(mContext);
        titleEditText.setBackgroundResource(R.drawable.asusres_edit_text_holo_dark);
        titleEditText.setTextColor(Color.WHITE);
        if (mTitle.length() == 0) {
            titleEditText.setHint(R.string.editor_title_hint);
            titleEditText.setSelection(0);
        } else {
            titleEditText.setText(mTitle);
            titleEditText.setSelection(0, mTitle.length());
        }
        builder.setView(titleEditText);
        builder.setTitle(R.string.editor_menu_title_rename);
        builder.setPositiveButton(R.string.editor_check_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int currentPosition;
                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                    if (!titleEditText.getText().toString().isEmpty()) {
                        currentPosition = selectedItemPositions.get(i);
                        renameData(currentPosition, titleEditText.getText().toString());
                    }
                }
                mActionMode.finish();
            }
        });
        builder.setNegativeButton(R.string.editor_check_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

    private void showDeleteCheckDialog(final List<Integer> selectedItemPositions,
            final String itemCount) {
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.editor_delete_script);
        builder.setMessage(R.string.delete_script_description);
        builder.setPositiveButton(R.string.editor_check_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int currentPosition;
                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                    currentPosition = selectedItemPositions.get(i);
                    removeData(currentPosition);
                }
                Toast toast = Toast.makeText(mContext,
                        String.format(mContext.getString(R.string.delete_script_toast),
                                itemCount), Toast.LENGTH_LONG);
                toast.show();
                mActionMode.finish();
            }
        });
        builder.setNegativeButton(R.string.editor_check_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActionMode.finish();
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void removeData(int position) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        mContext.getContentResolver().delete(
                Uri.withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(
                        cursor.getLong(cursor.getColumnIndex(SpeechContract._ID)))), null,
                null);
    }

    private void renameData(int position, String title) {
        if (!title.equals(mTitle)) {
            Cursor cursor = mAdapter.getCursor();
            cursor.moveToPosition(position);
            ContentValues updateValues = new ContentValues();
            updateValues.put(SpeechContract.TITLE, title);
            updateValues
                    .put(SpeechContract.DATE, ((SpeechEditorActivity) getActivity()).getTime());
            updateValues.put(SpeechContract.TYPE, USER_INPUT_TYPE);
            mContext.getContentResolver().update(
                    Uri.withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(
                            cursor.getLong(cursor.getColumnIndex(SpeechContract._ID)))),
                    updateValues, null, null);
        }
    }
}
