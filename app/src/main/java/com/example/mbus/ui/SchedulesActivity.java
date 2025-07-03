package com.example.mbus.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.ui.adapters.ScheduleAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SchedulesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<BusInfo> busList = new ArrayList<>();
    private List<BusInfo> filteredList = new ArrayList<>();

    private EditText editSearch;
    private Spinner spinnerCompany;

    private List<String> companies = new ArrayList<>();
    private String selectedCompany = "Todos";
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedules);

        recyclerView = findViewById(R.id.recyclerSchedules);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        editSearch = findViewById(R.id.editSearch);
        spinnerCompany = findViewById(R.id.spinnerCompany);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerCompany.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCompany = companies.get(position);
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCompany = "Todos";
                applyFilter();
            }
        });

        loadBusesFromFirestore();
        NavigationBar.setup(this);
    }

    private void loadBusesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Primeiro buscar os nomes das companhias
        db.collection("company").get().addOnSuccessListener(companyQuery -> {
            Map<String, String> companyMap = new HashMap<>();
            for (var doc : companyQuery) {
                companyMap.put(doc.getId(), doc.getString("name"));
            }

            // Agora buscar as rotas
            db.collection("routes").get().addOnSuccessListener(query -> {
                busList.clear();
                for (var doc : query) {
                    String id = doc.getId();
                    String companyId = doc.getString("companyId");
                    String companyName = companyMap.get(companyId); // traduzir ID para nome

                    Long routeNumberLong = doc.getLong("routeNumber");
                    if (routeNumberLong == null) continue;
                    int routeNumber = routeNumberLong.intValue();
                    String routeName = doc.getString("routeName");
                    String geojson = doc.getString("geojson");
                    String color = doc.getString("color");

                    BusInfo bus = new BusInfo(id, companyId, routeNumber, routeName, geojson, color);
                    bus.setCompanyName(companyName); // define o nome real
                    busList.add(bus);
                }

                setupCompanyFilter();
                applyFilter();
            }).addOnFailureListener(e -> {
                Log.e("SchedulesActivity", "Erro ao carregar rotas", e);
                Toast.makeText(this, "Erro ao carregar rotas", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Log.e("SchedulesActivity", "Erro ao carregar companhias", e);
            Toast.makeText(this, "Erro ao carregar companhias", Toast.LENGTH_SHORT).show();
        });
    }


    private void setupCompanyFilter() {
        companies.clear();
        companies.add("Todos");

        for (BusInfo bus : busList) {
            String name = bus.getCompanyName();
            if (name != null && !companies.contains(name)) {
                companies.add(name);
            }
        }

        Collections.sort(companies.subList(1, companies.size())); // mantém "Todos" no topo
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,         // layout do item selecionado
                companies
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinnerCompany.setAdapter(adapter);
    }

    private void applyFilter() {
        filteredList.clear();

        for (BusInfo bus : busList) {
            boolean matchCompany = selectedCompany.equals("Todos") ||
                    (bus.getCompanyName() != null && bus.getCompanyName().equalsIgnoreCase(selectedCompany));

            String normalizedRouteName = normalize(bus.getRouteName());
            String normalizedSearch = normalize(searchQuery);
            String routeNumber = String.valueOf(bus.getRouteNumber());

            boolean matchSearch =
                    normalizedSearch.isEmpty() ||
                            routeNumber.startsWith(normalizedSearch) ||
                            normalizedRouteName.startsWith(normalizedSearch);

            if (matchCompany && matchSearch) {
                filteredList.add(bus);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
