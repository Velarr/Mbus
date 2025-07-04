package com.example.mbus.ui.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.ui.ScheduleDetailsActivity;

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
            Drawable background = holder.number.getBackground().mutate();

            if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(bgColor);
            } else if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(bgColor);
            } else {
                holder.number.setBackgroundColor(bgColor); // fallback
            }

        } catch (Exception e) {
            holder.number.setBackgroundColor(Color.GRAY);
        }

        // Abre a tela de detalhes da rota com os horários de hoje
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ScheduleDetailsActivity.class);
            intent.putExtra("routeId", bus.getId());
            intent.putExtra("routeName", bus.getRouteName());
            intent.putExtra("routeNumber", bus.getRouteNumber());
            intent.putExtra("companyName", bus.getCompanyName());
            view.getContext().startActivity(intent);
        });
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