package com.vimalcvs.pdfreader.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vimalcvs.pdfreader.R;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<File> items;

    private final SparseBooleanArray selected_items;
    private int current_selected_idx = -1;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, File value, int position);

        void onItemLongClick(View view, File obj, int pos);
    }

    public void setOnItemClickListener(OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public MainRecycleViewAdapter(List<File> items) {
        this.items = items;
        selected_items = new SparseBooleanArray();
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView name;
        public TextView brief;
        public TextView size;
        public View lyt_parent;

        public OriginalViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.fileImageView);
            name = v.findViewById(R.id.fileItemTextview);
            brief = v.findViewById(R.id.dateItemTimeTextView);
            size = v.findViewById(R.id.sizeItemTimeTextView);
            lyt_parent = v.findViewById(R.id.listItemLinearLayout);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        vh = new OriginalViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        final File obj = items.get(position);
        if (holder instanceof OriginalViewHolder) {
            OriginalViewHolder view = (OriginalViewHolder) holder;
            view.name.setText(obj.getName());
            Date lastModDate = new Date(obj.lastModified());
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ENGLISH);
            String strDate = formatter.format(lastModDate);
            view.brief.setText(strDate);
            view.size.setText(GetSize(obj.length()));

            view.lyt_parent.setOnClickListener(v -> {
                if (mOnItemClickListener == null) return;
                mOnItemClickListener.onItemClick(v, obj, position);
            });

            view.lyt_parent.setOnLongClickListener(v -> {
                if (mOnItemClickListener == null) return false;
                mOnItemClickListener.onItemLongClick(v, obj, position);
                return true;
            });

            toggleCheckedIcon(holder, position);
            view.image.setImageResource(R.drawable.ic_iconfinder_24_2133056);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggleCheckedIcon(RecyclerView.ViewHolder holder, int position) {
        OriginalViewHolder view = (OriginalViewHolder) holder;
        if (selected_items.get(position, false)) {
            view.lyt_parent.setBackgroundColor(Color.parseColor("#4A32740A"));
        } else {
            view.lyt_parent.setBackgroundColor(Color.parseColor("#ffffff"));
        }
        if (current_selected_idx == position) resetCurrentIndex();
    }

    public String GetSize(long size) {
        String[] dictionary = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int index;
        double m = size;
        DecimalFormat dec = new DecimalFormat("0.00");
        for (index = 0; index < dictionary.length; index++) {
            if (m < 1024) {
                break;
            }
            m = m / 1024;
        }
        return dec.format(m).concat(" " + dictionary[index]);

    }

    public void toggleSelection(int pos) {
        current_selected_idx = pos;
        if (selected_items.get(pos, false)) {
            selected_items.delete(pos);
        } else {
            selected_items.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public int getSelectedItemCount() {
        return selected_items.size();
    }

    public void selectAll() {
        for (int i = 0; i < items.size(); i++) {
            selected_items.put(i, true);
            notifyItemChanged(i);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelections() {
        selected_items.clear();
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selected_items.size());
        for (int i = 0; i < selected_items.size(); i++) {
            items.add(selected_items.keyAt(i));
        }
        return items;
    }

    public void removeData(int position) {
        items.remove(position);
        resetCurrentIndex();
    }

    private void resetCurrentIndex() {
        current_selected_idx = -1;
    }

}