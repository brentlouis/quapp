package com.example.quapp;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One row per owned queue on the Today tab: name, a detail line, and how many were served. */
public class TodayQueueAdapter
        extends RecyclerView.Adapter<TodayQueueAdapter.TodayQueueViewHolder> {

    public interface OnTodayQueueClickListener {
        void onTodayQueueClick(Queue queue);
    }

    private final List<Queue> queues = new ArrayList<>();
    private final Map<String, QueueStats> statsById = new HashMap<>();
    private final OnTodayQueueClickListener clickListener;

    public TodayQueueAdapter(OnTodayQueueClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitQueues(List<Queue> newQueues, Map<String, QueueStats> newStats) {
        queues.clear();
        queues.addAll(newQueues);
        statsById.clear();
        statsById.putAll(newStats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TodayQueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_queue, parent, false);
        return new TodayQueueViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TodayQueueViewHolder holder, int position) {
        Queue queue = queues.get(position);
        holder.bind(queue, statsById.get(queue.getId()), clickListener);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    static class TodayQueueViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameText;
        private final TextView detailText;
        private final TextView servedText;

        TodayQueueViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.today_row_name);
            detailText = itemView.findViewById(R.id.today_row_detail);
            servedText = itemView.findViewById(R.id.today_row_served);
        }

        void bind(final Queue queue, QueueStats stats,
                  final OnTodayQueueClickListener clickListener) {
            nameText.setText(queue.getName());
            servedText.setText(String.valueOf(stats == null ? 0 : stats.getServedToday()));
            detailText.setText(detail(itemView.getContext(), queue, stats));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onTodayQueueClick(queue);
                }
            });
        }

        /** "Closed · 3 no-shows · 2.1 min per person". Each part only when it applies. */
        private static String detail(Context context, Queue queue, QueueStats stats) {
            List<String> parts = new ArrayList<>();

            if (queue.getStatus() == Queue.Status.PAUSED) {
                parts.add(context.getString(R.string.today_status_paused));
            } else if (queue.getStatus() == Queue.Status.CLOSED) {
                parts.add(context.getString(R.string.today_status_closed));
            }

            if (stats != null) {
                int noShows = stats.getNoShowsToday();
                parts.add(context.getResources()
                        .getQuantityString(R.plurals.today_no_shows, noShows, noShows));
                // No average until someone has been served; a made-up number would mislead.
                if (stats.getServiceSampleCount() > 0) {
                    parts.add(context.getString(R.string.today_minutes_per_person,
                            stats.getAverageServiceMinutes()));
                }
            }

            return TextUtils.join(context.getString(R.string.today_detail_separator), parts);
        }
    }
}
