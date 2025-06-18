package com.example.mbus;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class BusBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final List<BusInfo> busList;

    public BusBottomSheetDialogFragment(List<BusInfo> buses) {
        this.busList = buses;
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_buses, null);
        dialog.setContentView(contentView);

        FrameLayout bottomSheet = contentView.getRootView()
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

        int height = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * 0.85);
        bottomSheet.getLayoutParams().height = height;
        behavior.setPeekHeight(height);

        RecyclerView recyclerView = contentView.findViewById(R.id.bus_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new BusAdapter(busList));
    }
}
