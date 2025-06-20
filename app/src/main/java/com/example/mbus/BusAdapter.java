package com.example.mbus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {

    private final List<BusInfo> buses;
    private final OnBusSelectedListener listener;


    public BusAdapter(List<BusInfo> buses, OnBusSelectedListener listener) {
        this.buses = buses;
        this.listener = listener;
    }


    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        BusInfo bus = buses.get(position);
        holder.textView.setText(bus.nrota + " - " + bus.rotaNome);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBusSelected(bus.id); // envia o ID para a activity
            }
        });

    }


    @Override
    public int getItemCount() {
        return buses.size();
    }

    static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
