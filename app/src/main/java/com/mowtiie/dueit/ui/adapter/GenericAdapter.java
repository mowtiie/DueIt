package com.mowtiie.dueit.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GenericAdapter<T> extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {

    public interface Binder<T> {
        void bind(View itemView, T item, int position);
    }

    public interface OnItemClickListener<T> {
        void OnItemClick(T item);
    }

    private final int layoutResId;
    private final Binder<T> binder;
    private List<T> items;
    private OnItemClickListener<T> clickListener;

    public GenericAdapter(int layoutResId, List<T> items, Binder<T> binder) {
        this.layoutResId = layoutResId;
        this.items = items;
        this.binder = binder;
    }

    public void setOnItemClickListener(OnItemClickListener<T> clickListener) {
        this.clickListener = clickListener;
    }

    public void updateList(List<T> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GenericAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenericAdapter.ViewHolder holder, int position) {
        T item = items.get(position);
        binder.bind(holder.itemView, item, position);

        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.OnItemClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
