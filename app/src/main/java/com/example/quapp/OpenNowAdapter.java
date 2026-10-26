package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** "Open now across Bohol" on a first visit: short rows, not cards (canvas 33). */
public class OpenNowAdapter extends RecyclerView.Adapter<OpenNowAdapter.OpenNowViewHolder> {

    private final List<Queue> queues = new ArrayList<>();
    private final QueueAdapter.OnQueueClickListener clickListener;
    private final Runnable onSavedChanged;

    public OpenNowAdapter(QueueAdapter.OnQueueClickListener clickListener) {
        this(clickListener, null);
    }

    public OpenNowAdapter(QueueAdapter.OnQueueClickListener clickListener, Runnable onSavedChanged) {
        this.clickListener = clickListener;
        this.onSavedChanged = onSavedChanged;
    }

    public void submitQueues(List<Queue> newQueues) {
        queues.clear();
        queues.addAll(newQueues);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OpenNowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_open_now, parent, false);
        return new OpenNowViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OpenNowViewHolder holder, int position) {
        Queue queue = queues.get(position);
        holder.bind(queue, position > 0, clickListener);
        View save = holder.itemView.findViewById(R.id.open_now_save);
        boolean bookmarks = onSavedChanged != null;
        save.setVisibility(bookmarks ? View.VISIBLE : View.GONE);
        holder.itemView.findViewById(R.id.open_now_chevron).setVisibility(bookmarks ? View.GONE : View.VISIBLE);
        holder.name.setMaxLines(bookmarks ? Integer.MAX_VALUE : 1);
        if (bookmarks) {
            boolean saved = new SavedServices(save.getContext()).contains(queue.getId());
            save.setSelected(saved);
            ((ImageView) save).setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            save.setContentDescription(holder.itemView.getContext().getString(
                    saved ? R.string.saved_remove_named : R.string.saved_save_named, queue.getName()));
            save.setOnClickListener(v -> {
                new SavedServices(v.getContext()).toggle(queue.getId());
                notifyItemChanged(holder.getBindingAdapterPosition());
                onSavedChanged.run();
            });
            android.content.Context context = holder.itemView.getContext();
            if (queue.acceptsJoins()) holder.detail.setText(context.getString(R.string.saved_service_detail,
                    queue.getVenue(), queue.getEstimatedWaitMinutes()));
            else holder.detail.setText(context.getString(R.string.saved_service_status, queue.getVenue(),
                    context.getString(queue.getStatus() == Queue.Status.CLOSED ? R.string.detail_status_closed
                            : queue.getStatus() == Queue.Status.PAUSED ? R.string.detail_status_paused : R.string.detail_status_upcoming)));
        }
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    static class OpenNowViewHolder extends RecyclerView.ViewHolder {

        private final View rule;
        private final ImageView icon;
        private final TextView name;
        private final TextView detail;

        OpenNowViewHolder(@NonNull View itemView) {
            super(itemView);
            rule = itemView.findViewById(R.id.open_now_rule);
            icon = itemView.findViewById(R.id.open_now_icon);
            name = itemView.findViewById(R.id.open_now_name);
            detail = itemView.findViewById(R.id.open_now_detail);
        }

        void bind(final Queue queue, boolean showRule,
                  final QueueAdapter.OnQueueClickListener clickListener) {
            rule.setVisibility(showRule ? View.VISIBLE : View.GONE);
            icon.setImageResource(queue.getCategory().icon);
            name.setText(queue.getName());
            detail.setText(itemView.getContext().getString(R.string.browse_open_now_detail_format,
                    queue.getMunicipality(), queue.getEstimatedWaitMinutes()));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onQueueClick(queue);
                }
            });
        }
    }
}
