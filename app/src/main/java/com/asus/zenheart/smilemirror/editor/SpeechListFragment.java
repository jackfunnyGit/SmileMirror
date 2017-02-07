package com.asus.zenheart.smilemirror.editor;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.asus.zenheart.smilemirror.FaceTrackerActivity;
import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import java.util.List;

public class SpeechListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback {

    private SpeechCursorAdapter mAdapter;
    private boolean mMenuIsSelected;
    private FloatingActionButton mFloatingActionButton;
    private ActionMode mActionMode;
    private FragmentActivity mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_list_main, container, false);

        // use getActivity instead of getContext because it need to setSupportActionBar
        mContext = getActivity();
        mMenuIsSelected = false;

        setHasOptionsMenu(true);

        initView(view);
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        backToTheMirror();
                    }
                    return true;
                }
                return false;
            }
        });

        getLoaderManager().initLoader(0, null, this);
        return view;
    }

    private void initView(View view) {
        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fabAdd);
        Toolbar mToolbar = (Toolbar) view.findViewById(R.id.editor_list_toolbar);
        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpeechBrowsePageFragment speechBrowsePageFragment = new SpeechBrowsePageFragment();
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
                fragmentTransaction.commit();
            }
        });

        ((AppCompatActivity) mContext).setSupportActionBar(mToolbar);
        mContext.setTitle(R.string.conversation_scripts);

        mToolbar.setTitleTextColor(mContext.getColor(R.color.smile_text_color));
        mToolbar.setNavigationIcon(R.drawable.back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToTheMirror();
            }
        });
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SpeechCursorAdapter(new SpeechCursorAdapter.itemClick() {
            @Override
            public void itemClick(Cursor cursor) {
                if (mActionMode != null) {
                    return;
                }
                long id = cursor.getLong(cursor.getColumnIndex(SpeechContract._ID));
                SpeechBrowsePageFragment speechBrowsePageFragment = SpeechBrowsePageFragment
                        .newInstance(id);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment, speechBrowsePageFragment);
                fragmentTransaction.commit();
            }
        }, new SpeechCursorAdapter.itemLongClick() {

            @Override
            public void itemLongClick(Cursor cursor) {
                if (mActionMode != null) {
                    return;
                }
                mActionMode = mContext.startActionMode(SpeechListFragment.this);
            }
        });
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void backToTheMirror() {
        Intent intent = new Intent(mContext,
                FaceTrackerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    private void removeData(int position) {
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);
        final long id = cursor.getLong(cursor.getColumnIndex(SpeechContract._ID));
        mContext.getContentResolver().delete(
                Uri.withAppendedPath(SpeechContract.SPEECH_URI, String.valueOf(id)),
                null, null);
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
                SpeechContract.TITLE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.setCursor(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.editor_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.editor_list_menu_setting) {
            mActionMode = mContext.startActionMode(SpeechListFragment.this);
        }
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.editor_list_action_mode, menu);
        actionMode.setTitle(R.string.editor_delete_list_script);
        mFloatingActionButton.setVisibility(View.GONE);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.editor_action_menu_select:
                // TODO: Rename file feature
                mAdapter.clearSelections();
                if (!mMenuIsSelected) {
                    for (int i = mAdapter.getItemCount() - 1; i >= 0; i--) {
                        mAdapter.toggleSelection(i);
                    }
                    mMenuIsSelected = true;
                } else {
                    mMenuIsSelected = false;
                }
            case R.id.editor_action_menu_delete:
                showDeleteCheckDialog(mAdapter.getSelectedItem());
                actionMode.finish();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.mActionMode = null;
        mAdapter.clearSelections();
        mFloatingActionButton.setVisibility(View.VISIBLE);
    }

    private void showDeleteCheckDialog(final List<Integer> selectedItemPositions) {
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
