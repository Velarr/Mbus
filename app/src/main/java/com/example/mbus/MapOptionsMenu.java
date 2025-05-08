package com.example.mbus;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public class MapOptionsMenu {

    public interface OnOptionSelectedListener {
        void onOptionSelected(int geoJsonResId);
    }

    private final Context context;
    private final View anchorView;
    private final OnOptionSelectedListener listener;

    public MapOptionsMenu(Context context, View anchorView, OnOptionSelectedListener listener) {
        this.context = context;
        this.anchorView = anchorView;
        this.listener = listener;
    }

    public void show() {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.map_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.option_vr_line) {
                    listener.onOptionSelected(R.raw.vr_line);
                    return true;
                } else if (itemId == R.id.option_old_street_line) {
                    listener.onOptionSelected(R.raw.old_street);
                    return true;
                }
                return false;
            }
        });

        popupMenu.show();
    }
}
