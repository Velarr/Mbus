package com.example.mbus.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.data.LocationsRepository;
import com.example.mbus.listeners.OnBusFilterChangedListener;
import com.example.mbus.listeners.OnBusSelectedListener;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

public class NavigationBar {

    public static void setup(final Activity activity) {
        LinearLayout btnHome = activity.findViewById(R.id.btn_home);
        LinearLayout btnBus = activity.findViewById(R.id.btn_bus);
        LinearLayout btnSchedules = activity.findViewById(R.id.btn_schedules);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                if (!(activity instanceof MapsActivity)) {
                    Intent intent = new Intent(activity, MapsActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnBus != null) {
            btnBus.setOnClickListener(v -> {
                if (activity instanceof MapsActivity) {
                    MapsActivity mapsActivity = (MapsActivity) activity;
                    LocationsRepository locationsRepository = new LocationsRepository();
                    locationsRepository.startListeningBuses(new LocationsRepository.BusListListener() {
                        @Override
                        public void onBusListUpdate(List<BusInfo> buses) {
                            BusBottomSheetDialogFragment bottomSheet = new BusBottomSheetDialogFragment(
                                    buses,
                                    mapsActivity,
                                    mapsActivity
                            );
                            bottomSheet.show(mapsActivity.getSupportFragmentManager(), "bus_bottom_sheet");

                            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(activity);
                            Bundle bundle = new Bundle();
                            bundle.putString("navigation_menu", "bus");
                            analytics.logEvent("bus_menu_opened", bundle);
                        }

                        @Override
                        public void onError(String message) {
                            Log.e("NavigationBar", "Erro ao carregar lista de autocarros: " + message);
                        }
                    });
                } else {
                    Intent intent = new Intent(activity, MapsActivity.class);
                    intent.putExtra("open_bus_menu", true);
                    activity.startActivity(intent);

                    FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(activity);
                    Bundle bundle = new Bundle();
                    bundle.putString("navigation_menu", "bus");
                    analytics.logEvent("bus_menu_opened", bundle);
                }
            });
        }

        if (btnSchedules != null) {
            btnSchedules.setOnClickListener(v -> {
                if (!(activity instanceof SchedulesActivity)) {
                    Intent intent = new Intent(activity, SchedulesActivity.class);
                    activity.startActivity(intent);
                }
            });
        }
    }
}
