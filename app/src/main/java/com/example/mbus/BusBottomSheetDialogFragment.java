package com.example.mbus;

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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BusBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private final List<BusInfo> busList;
    private OnBusSelectedListener listener;
    private String selectedCompany = "Todos";
    private String searchQuery = "";
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private Spinner companySpinner;
    private List<String> companyOptions = new ArrayList<>();

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

        recyclerView = contentView.findViewById(R.id.bus_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchEditText = contentView.findViewById(R.id.edittext_search);
        companySpinner = contentView.findViewById(R.id.spinner_companhia);

        fetchCompaniesFromFirestore();

        companySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCompany = companyOptions.get(position);
                updateBusList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                companySpinner.setSelection(0); // Força seleção para "Todos"
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                updateBusList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateBusList();
    }

    private void fetchCompaniesFromFirestore() {
        Set<String> companySet = new HashSet<>();
        companySet.add("Todos");

        for (BusInfo bus : busList) {
            if (bus.companhia != null && !bus.companhia.isEmpty()) {
                companySet.add(bus.companhia);
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
        companySpinner.setSelection(0); // Define "Todos" como padrão
    }

    private void updateBusList() {
        List<BusInfo> filtered = new ArrayList<>();

        for (BusInfo bus : busList) {
            boolean matchesCompany = selectedCompany.equals("Todos") || bus.companhia.equalsIgnoreCase(selectedCompany);
            boolean matchesSearch = searchQuery.isEmpty()
                    || String.valueOf(bus.nrota).contains(searchQuery)
                    || bus.rotaNome.toLowerCase().contains(searchQuery.toLowerCase());

            if (matchesCompany && matchesSearch) {
                filtered.add(bus);
            }
        }

        recyclerView.setAdapter(new BusAdapter(filtered, id -> {
            if (listener != null) {
                listener.onBusSelected(id);
            }
            dismiss(); // Fecha o BottomSheet após seleção
        }));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBusSelectedListener) {
            listener = (OnBusSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " deve implementar OnBusSelectedListener");
        }
    }
}
