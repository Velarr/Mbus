package com.example.mbus.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.ui.ScheduleDetailsActivity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<BusInfo> buses;
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Set<String> favoriteIds;

    public ScheduleAdapter(Context context, List<BusInfo> buses) {
        this.context = context;
        this.buses = buses;
        this.sharedPreferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
        this.favoriteIds = new HashSet<>(sharedPreferences.getStringSet("favorite_routes", new HashSet<>()));
        reorderFavorites();
    }

    private void reorderFavorites() {
        Collections.sort(buses, (a, b) -> {
            boolean aFav = favoriteIds.contains(a.getId());
            boolean bFav = favoriteIds.contains(b.getId());
            return Boolean.compare(!aFav, !bFav); // favoritos primeiro
        });
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
                holder.number.setBackgroundColor(bgColor);
            }
        } catch (Exception e) {
            holder.number.setBackgroundColor(Color.GRAY);
        }

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ScheduleDetailsActivity.class);
            intent.putExtra("routeId", bus.getId());
            intent.putExtra("routeName", bus.getRouteName());
            intent.putExtra("routeNumber", String.valueOf(bus.getRouteNumber())); // ✅ como antes
            intent.putExtra("companyName", bus.getCompanyName());
            intent.putExtra("color", bus.getColor());
            view.getContext().startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return buses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, name, description;
        ImageView favoriteIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.schedule_number);
            name = itemView.findViewById(R.id.schedule_name);
            description = itemView.findViewById(R.id.schedule_description);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon); // ainda falta no XML

        }
    }
}
