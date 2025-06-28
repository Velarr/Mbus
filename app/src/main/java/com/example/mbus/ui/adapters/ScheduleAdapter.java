package com.example.mbus.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<BusInfo> buses;

    public ScheduleAdapter(List<BusInfo> buses) {
        this.buses = buses;
    }

    @NonNull
    @Override
    public ScheduleAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listbuses, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleAdapter.ViewHolder holder, int position) {
        BusInfo bus = buses.get(position);

        holder.number.setText(String.valueOf(bus.getRouteNumber()));
        holder.name.setText(bus.getRouteName());

        String company = bus.getCompanyName() != null ? bus.getCompanyName() : "Rota";
        holder.description.setText(company + " - " + bus.getRouteName());

        try {
            int bgColor = Color.parseColor(bus.getColor());
            GradientDrawable drawable = (GradientDrawable) holder.number.getBackground().mutate();
            drawable.setColor(bgColor);
        } catch (Exception e) {
            // fallback
            holder.number.setBackgroundColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return buses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, name, description;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.schedule_number);
            name = itemView.findViewById(R.id.schedule_name);
            description = itemView.findViewById(R.id.schedule_description);
        }
    }
}
