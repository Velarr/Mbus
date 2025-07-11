package com.example.mbus.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.listeners.OnBusFilterChangedListener;
import com.example.mbus.listeners.OnBusSelectedListener;
import com.example.mbus.ui.adapters.BusAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BusBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static BusBottomSheetDialogFragment newInstance(List<BusInfo> buses,
                                                           OnBusSelectedListener listener,
                                                           OnBusFilterChangedListener filterListener) {
        return new BusBottomSheetDialogFragment(buses, listener, filterListener);
    }

    private final List<BusInfo> busList;
    private final OnBusSelectedListener listener;
    private final OnBusFilterChangedListener filterChangedListener;

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private Spinner companySpinner;

    private List<String> companyOptions = new ArrayList<>();
    private static String selectedCompany = "Todos";
    private static String searchQuery = "";

    public BusBottomSheetDialogFragment(List<BusInfo> buses,
                                        OnBusSelectedListener listener,
                                        OnBusFilterChangedListener filterChangedListener) {
        this.busList = buses;
        this.listener = listener;
        this.filterChangedListener = filterChangedListener;
    }

    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_buses, null);
        dialog.setContentView(contentView);

        FrameLayout bottomSheet = contentView.getRootView()
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

        // Reduz a altura para 60%
        int height = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * 0.60);
        bottomSheet.getLayoutParams().height = height;
        behavior.setPeekHeight(height);

        recyclerView = contentView.findViewById(R.id.bus_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchEditText = contentView.findViewById(R.id.editSearch);
        companySpinner = contentView.findViewById(R.id.spinnerCompany);

        setupCompanies();

        companySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCompany = companyOptions.get(position);
                updateBusList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                companySpinner.setSelection(0);
            }
        });

        searchEditText.setText(searchQuery);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                updateBusList();

                Bundle searchBundle = new Bundle();
                searchBundle.putString("searched_company", selectedCompany);
                searchBundle.putString("search_query", searchQuery);
                FirebaseAnalytics.getInstance(getContext()).logEvent("bus_search", searchBundle);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        updateBusList();
    }

    private void setupCompanies() {
        Set<String> companySet = new HashSet<>();
        companySet.add("Todos");

        for (BusInfo bus : busList) {
            String companyName = bus.getCompanyName();
            if (companyName != null && !companyName.isEmpty()) {
                companySet.add(companyName);
            }
        }

        companyOptions = new ArrayList<>(companySet);
        Collections.sort(companyOptions);
        if (companyOptions.contains("Todos")) {
            companyOptions.remove("Todos");
            companyOptions.add(0, "Todos");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, companyOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        companySpinner.setAdapter(adapter);

        int selectedIndex = companyOptions.indexOf(selectedCompany);
        if (selectedIndex >= 0) {
            companySpinner.setSelection(selectedIndex);
        } else {
            companySpinner.setSelection(0);
            selectedCompany = "Todos";
        }
    }

    private void updateBusList() {
        List<BusInfo> filtered = new ArrayList<>();

        for (BusInfo bus : busList) {
            String companyName = bus.getCompanyName() != null ? bus.getCompanyName() : "";

            boolean matchesCompany = selectedCompany.equals("Todos") || companyName.equalsIgnoreCase(selectedCompany);
            boolean matchesSearch = searchQuery.isEmpty()
                    || String.valueOf(bus.getRouteNumber()).startsWith(searchQuery)
                    || normalize(bus.getRouteName()).startsWith(normalize(searchQuery));

            if (matchesCompany && matchesSearch) {
                filtered.add(bus);
            }
        }

        // Ordenar favoritos primeiro (sem mostrar estrela)
        Set<String> favoriteIds = getContext()
                .getSharedPreferences("favorites", Context.MODE_PRIVATE)
                .getStringSet("favorite_routes", new HashSet<>());

        Collections.sort(filtered, (a, b) -> {
            boolean aFav = favoriteIds.contains(a.getId());
            boolean bFav = favoriteIds.contains(b.getId());
            return Boolean.compare(!aFav, !bFav);
        });

        recyclerView.setAdapter(new BusAdapter(filtered, id -> {
            if (listener != null) {
                listener.onBusSelected(id);
            }

            Bundle selectedBundle = new Bundle();
            selectedBundle.putString("selected_bus_id", id);
            FirebaseAnalytics.getInstance(requireContext()).logEvent("bus_selected", selectedBundle);

            dismiss();
        }));

        if (filterChangedListener != null) {
            filterChangedListener.onBusFilterChanged(filtered);
        }
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof OnBusSelectedListener)) {
            throw new RuntimeException(context.toString() + " deve implementar OnBusSelectedListener");
        }
    }
}
