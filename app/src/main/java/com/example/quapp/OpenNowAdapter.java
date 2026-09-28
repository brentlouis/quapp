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

    public OpenNowAdapter(QueueAdapter.OnQueueClickListener clickListener) {
        this.clickListener = clickListener;
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
        holder.bind(queues.get(position), position > 0, clickListener);
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
