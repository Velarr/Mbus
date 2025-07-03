package com.example.mbus.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;

import java.util.List;

public class NearbyRoutesAdapter extends RecyclerView.Adapter<NearbyRoutesAdapter.RouteViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(String routeId);
    }

    private final List<BusInfo> routes;
    private final OnRouteClickListener listener;

    public NearbyRoutesAdapter(List<BusInfo> routes, OnRouteClickListener listener) {
        this.routes = routes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listbuses, parent, false);
        return new RouteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        BusInfo route = routes.get(position);

        // Texto do número
        holder.routeNumber.setText(String.valueOf(route.getRouteNumber()));

        // Nome da rota
        holder.routeName.setText(route.getRouteName());

        // Descrição: nome da companhia
        holder.routeDescription.setText(route.getCompanyName());

        // Cor de fundo do círculo
        try {
            holder.routeNumber.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor(route.getColor()))
            );
        } catch (Exception ignored) {
            holder.routeNumber.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.GRAY)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRouteClick(route.getId());
        });
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView routeNumber, routeName, routeDescription;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            routeNumber = itemView.findViewById(R.id.schedule_number);
            routeName = itemView.findViewById(R.id.schedule_name);
            routeDescription = itemView.findViewById(R.id.schedule_description);
        }
    }
}
