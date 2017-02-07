package com.asus.zenheart.smilemirror.editor;

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

import java.util.ArrayList;
import java.util.List;

class SpeechCursorAdapter extends RecyclerView.Adapter<SpeechCursorAdapter.ViewHolder> {

    private Cursor mCursor;
    private itemClick mListener;
    private itemLongClick mLongListener;
    private SparseBooleanArray mSelectedItem;

    SpeechCursorAdapter(itemClick listener, itemLongClick longListener) {
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
                int position = viewHolder.getAdapterPosition();
                mCursor.moveToPosition(position);
                if (mListener != null) mListener.itemClick(mCursor);
            }
        });
        view.setLongClickable(true);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = viewHolder.getAdapterPosition();
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

        int type = mCursor.getInt(mCursor.getColumnIndex(SpeechContract.TYPE));
        String date = mCursor.getString(mCursor.getColumnIndex(SpeechContract.DATE));
        String title = mCursor.getString(mCursor.getColumnIndex(SpeechContract.TITLE));

        switch (type) {
            case 0:
                holder.mTypeView.setImageResource(R.drawable.inputtext_default);
            case 1:
                holder.mTypeView.setImageResource(R.drawable.inputtext_add);
        }

        holder.mTitle.setText(title);
        holder.mDate.setText(date);
        holder.itemView.setActivated(mSelectedItem.get(position, false));
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

    void toggleSelection(int pos) {
        if (mSelectedItem.get(pos)) {
            mSelectedItem.delete(pos);
        } else {
            mSelectedItem.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    void clearSelections() {
        mSelectedItem.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
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