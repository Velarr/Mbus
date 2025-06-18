package com.example.mbus;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

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
        // Em vez de PopupMenu, vamos buscar os autocarros e abrir o BottomSheet

        LocationsRepository locationsRepository = new LocationsRepository();

        locationsRepository.startListeningBuses(new LocationsRepository.BusListListener() {
            @Override
            public void onBusListUpdate(List<BusInfo> buses) {
                // Cria e mostra o BottomSheet com a lista
                BusBottomSheetDialogFragment bottomSheet = new BusBottomSheetDialogFragment(buses);

                // context precisa ser uma Activity ou Context que permita pegar o FragmentManager
                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    bottomSheet.show(activity.getSupportFragmentManager(), "bus_bottom_sheet");
                } else {
                    Log.e("MapOptionsMenu", "Context não é AppCompatActivity, não pode abrir BottomSheet");
                }
            }

            @Override
            public void onError(String message) {
                Log.e("MapOptionsMenu", "Erro ao buscar autocarros: " + message);
            }
        });
    }

}
