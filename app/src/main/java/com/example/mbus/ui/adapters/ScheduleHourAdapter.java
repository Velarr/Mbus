package com.example.mbus.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;

import java.util.List;

public class ScheduleHourAdapter extends RecyclerView.Adapter<ScheduleHourAdapter.ViewHolder> {

    private final List<String> times;

    public ScheduleHourAdapter(List<String> times) {
        this.times = times;
    }

    @NonNull
    @Override
    public ScheduleHourAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_hour, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleHourAdapter.ViewHolder holder, int position) {
        holder.txtTime.setText(times.get(position));
    }

    @Override
    public int getItemCount() {
        return times.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime;

        public ViewHolder(View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txt_time);
        }
    }
}
