package com.example.mbus.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mbus.R;
import com.example.mbus.data.BusInfo;
import com.example.mbus.ui.adapters.BusAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SchedulesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BusAdapter adapter;
    private List<BusInfo> busList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedules);

        recyclerView = findViewById(R.id.recyclerSchedules);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BusAdapter(busList, busId -> {
            // Opcional: ação ao clicar num item
        });
        recyclerView.setAdapter(adapter);

        loadBusesFromFirestore();
    }

    private void loadBusesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rotas").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    busList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String companyName = doc.getString("companyName");
                        int routeNumber = doc.getLong("routeNumber").intValue();
                        String routeName = doc.getString("routeName");
                        String geojson = doc.getString("geojson");
                        String color = doc.getString("color");

                        busList.add(new BusInfo(id, companyName, routeNumber, routeName, geojson, color));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("SchedulesActivity", "Erro ao carregar rotas", e);
                    Toast.makeText(this, "Erro ao carregar rotas", Toast.LENGTH_SHORT).show();
                });
    }
}
