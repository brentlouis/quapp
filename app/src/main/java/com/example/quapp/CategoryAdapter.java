package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/** The eight categories in the picker sheet, with a check on the one already chosen. */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryPickedListener {
        void onCategoryPicked(Category category);
    }

    private final Category[] categories = Category.values();
    @Nullable
    private final Category selected;
    private final OnCategoryPickedListener listener;

    public CategoryAdapter(@Nullable Category selected, OnCategoryPickedListener listener) {
        this.selected = selected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories[position], categories[position] == selected, listener);
    }

    @Override
    public int getItemCount() {
        return categories.length;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView label;
        private final TextView hint;
        private final View check;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.category_icon);
            label = itemView.findViewById(R.id.category_label);
            hint = itemView.findViewById(R.id.category_hint);
            check = itemView.findViewById(R.id.category_check);
        }

        void bind(final Category category, boolean isSelected,
                  final OnCategoryPickedListener listener) {
            icon.setImageResource(category.icon);
            label.setText(category.label);
            hint.setText(category.hint);
            check.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            itemView.setSelected(isSelected);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onCategoryPicked(category);
                }
            });
        }
    }
}
