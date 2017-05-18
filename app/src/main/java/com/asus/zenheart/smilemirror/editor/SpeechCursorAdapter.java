package com.asus.zenheart.smilemirror.editor;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.zenheart.smilemirror.R;
import com.asus.zenheart.smilemirror.editor.database.SpeechContract;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The adapter is using to let recycler view like list view, and it can insert, update and delete data from SQLite
 *
 */
class SpeechCursorAdapter extends RecyclerView.Adapter<SpeechCursorAdapter.ViewHolder> {

    private Cursor mCursor;
    private itemClick mListener;
    private itemLongClick mLongListener;
    private SparseBooleanArray mSelectedItem;
    private Context mContext;

    SpeechCursorAdapter(Context context, itemClick listener, itemLongClick longListener)
    {   mContext = context;
        mListener = listener;
        mLongListener = longListener;
        mSelectedItem = new SparseBooleanArray();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.editor_list_fragment, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int position = viewHolder.getAdapterPosition();
                toggleSelection(position);
                mCursor.moveToPosition(position);
                if (mListener != null) mListener.itemClick(mCursor);
            }
        });
        view.setLongClickable(true);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int position = viewHolder.getAdapterPosition();
                toggleSelection(position);
                mCursor.moveToPosition(position);
                if (mLongListener != null) mLongListener.itemLongClick(mCursor);
                return true;
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        StringBuilder timeFormat = new StringBuilder(getDate(mCursor.getLong(
                mCursor.getColumnIndex(SpeechContract.DATE)))).append(" ").append(
                        getTime(mCursor.getLong(mCursor.getColumnIndex(SpeechContract.DATE))));
        int type = mCursor.getInt(mCursor.getColumnIndex(SpeechContract.TYPE));

        holder.itemView.setActivated(mSelectedItem.get(position, false));

        if (holder.itemView.isActivated()) {
            holder.mTypeView.setImageResource(R.drawable.check);
            if (type == 0) {
                holder.mTitle.setText(mCursor.
                        getString(mCursor.getColumnIndex(SpeechContract.TITLE)));
            } else {
                holder.mTitle.setText(setSampleSpeechTitle(type));
            }
        } else {
            if (type == 0) {
                holder.mTypeView.setImageResource(R.drawable.inputtext_add);
                holder.mTitle.setText(mCursor.
                        getString(mCursor.getColumnIndex(SpeechContract.TITLE)));
            } else {
                holder.mTypeView.setImageResource(R.drawable.inputtext_default);
                holder.mTitle.setText(setSampleSpeechTitle(type));
            }
        }
        holder.mDate.setText(timeFormat);
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

    private String getDate(long time) {
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(mContext);
        if (time <= 0) {
            return mContext.getString(R.string.editor_default_date);
        }
        return dateFormat.format(time);
    }

    private String getTime(long time) {
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(mContext);
        if (time <= 0) {
            return "";
        }
        return timeFormat.format(time);
    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                int id = mCursor.getColumnIndex(SpeechContract._ID);
                return mCursor.getLong(id);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    Cursor getCursor() {
        return mCursor;
    }

    void setCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    interface itemClick {
        void itemClick(Cursor cursor);
    }

    interface itemLongClick {
        void itemLongClick(Cursor cursor);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mTypeView;
        TextView mTitle;
        TextView mDate;

        ViewHolder(View v) {
            super(v);
            mTypeView = (ImageView) v.findViewById(R.id.editor_list_type_img);
            mTitle = (TextView) v.findViewById(R.id.editor_list_title);
            mDate = (TextView) v.findViewById(R.id.editor_list_date);

        }
    }

    void clearSelections() {
        mSelectedItem.clear();
        notifyDataSetChanged();
    }

    private void toggleSelection(int pos) {
        if (mSelectedItem.get(pos)) {
            mSelectedItem.delete(pos);
        } else {
            mSelectedItem.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    int getSelectedItemCount() {
        return mSelectedItem.size();
    }

    List<Integer> getSelectedItem() {
        List<Integer> items = new ArrayList<>(mSelectedItem.size());
        for (int i = 0; i < mSelectedItem.size(); i++) {
            items.add(mSelectedItem.keyAt(i));
        }
        return items;
    }

}