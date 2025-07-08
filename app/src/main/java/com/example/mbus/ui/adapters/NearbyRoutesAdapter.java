package com.example.mbus.ui.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        holder.routeNumber.setText(String.valueOf(route.getRouteNumber()));
        holder.routeName.setText(route.getRouteName());
        holder.routeDescription.setText(route.getCompanyName());

        try {
            holder.routeNumber.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(route.getColor())));
        } catch (Exception ignored) {
            holder.routeNumber.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        }

        View favoriteIcon = holder.itemView.findViewById(R.id.favorite_icon);
        if (favoriteIcon != null) {
            favoriteIcon.setVisibility(View.GONE);
        }

        loadNextDeparture(route.getId(), holder.nextDeparture);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRouteClick(route.getId());
        });
    }


    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView routeNumber, routeName, routeDescription, nextDeparture;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            routeNumber = itemView.findViewById(R.id.schedule_number);
            routeName = itemView.findViewById(R.id.schedule_name);
            routeDescription = itemView.findViewById(R.id.schedule_description);
            nextDeparture = itemView.findViewById(R.id.tvNextDeparture);
        }
    }


    private void loadNextDeparture(String routeId, TextView textView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String field;
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY) {
            field = "saturday";
        } else if (day == Calendar.SUNDAY) {
            field = "sunday";
        } else {
            field = "weekday";
        }

        db.collection("routes").document(routeId)
                .get().addOnSuccessListener(document -> {
                    textView.setVisibility(View.VISIBLE);
                    if (document.exists()) {
                        Object rawSchedule = document.get("schedules." + field);
                        if (rawSchedule instanceof List) {
                            List<String> times = (List<String>) rawSchedule;
                            if (times != null && !times.isEmpty()) {
                                Collections.sort(times);
                                String now = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                                for (String t : times) {
                                    if (t.compareTo(now) > 0) {
                                        textView.setText("Próxima saída: " + t);
                                        return;
                                    }
                                }
                                textView.setText("Último já saiu");
                            } else {
                                textView.setText("Sem horários");
                            }
                        } else {
                            textView.setText("Sem horários");
                        }
                    } else {
                        textView.setText("Sem horários");
                    }
                }).addOnFailureListener(e -> {
                    textView.setVisibility(View.VISIBLE);
                    textView.setText("Erro ao buscar");
                });
    }

}