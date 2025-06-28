package com.example.mbus.ui.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;

import java.util.List;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {

    public interface OnBusClickListener {
        void onBusClick(String routeId);
    }

    private final List<BusInfo> busList;
    private final OnBusClickListener listener;

    public BusAdapter(List<BusInfo> busList, OnBusClickListener listener) {
        this.busList = busList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listbuses, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        BusInfo bus = busList.get(position);

        holder.routeNumber.setText(String.valueOf(bus.getRouteNumber()));
        holder.routeName.setText(bus.getRouteName());
        holder.routeDescription.setText(bus.getCompanyName());

        // Círculo com cor de fundo
        GradientDrawable circle = (GradientDrawable) holder.routeNumber.getBackground();
        try {
            circle.setColor(android.graphics.Color.parseColor(bus.getColor()));
        } catch (IllegalArgumentException e) {
            circle.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.teal_700));
        }

        holder.itemView.setOnClickListener(v -> listener.onBusClick(bus.getId()));
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView routeNumber, routeName, routeDescription;

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            routeNumber = itemView.findViewById(R.id.schedule_number);
            routeName = itemView.findViewById(R.id.schedule_name);
            routeDescription = itemView.findViewById(R.id.schedule_description);
        }
    }
}
