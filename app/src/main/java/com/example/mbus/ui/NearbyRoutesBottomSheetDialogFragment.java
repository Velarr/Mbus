package com.example.mbus.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.listeners.OnBusSelectedListener;
import com.example.mbus.ui.adapters.NearbyRoutesAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class NearbyRoutesBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROUTES = "arg_routes";
    private List<BusInfo> nearbyBuses;
    private OnBusSelectedListener listener;

    public static NearbyRoutesBottomSheetDialogFragment newInstance(List<BusInfo> buses, OnBusSelectedListener listener) {
        NearbyRoutesBottomSheetDialogFragment fragment = new NearbyRoutesBottomSheetDialogFragment();
        fragment.listener = listener;

        Bundle args = new Bundle();
        args.putSerializable(ARG_ROUTES, (java.io.Serializable) buses);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        View view = View.inflate(getContext(), R.layout.bottom_sheet_nearby_routes, null);
        dialog.setContentView(view);

        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.5);
        view.setLayoutParams(params);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(params.height);
        }

        if (getArguments() != null) {
            nearbyBuses = (List<BusInfo>) getArguments().getSerializable(ARG_ROUTES);
        }

        RecyclerView recycler = view.findViewById(R.id.recycler_nearby_routes);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new NearbyRoutesAdapter(nearbyBuses, routeId -> {
            if (listener != null) listener.onBusSelected(routeId);
            dismiss();
        }));

        TextView title = view.findViewById(R.id.nearby_routes_title);
        title.setText("Autocarros que passam perto");
    }
}
