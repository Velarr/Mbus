package com.example.mbus.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mbus.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScheduleDetailsActivity extends AppCompatActivity {

    private TextView txtRouteNumber, txtRouteName;
    private LinearLayout headerContainer;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_details);

        txtRouteNumber = findViewById(R.id.txt_route_number);
        txtRouteName = findViewById(R.id.txt_route_name);
        headerContainer = findViewById(R.id.header_container);

        firestore = FirebaseFirestore.getInstance();

        NavigationBar.setup(this);

        String routeId = getIntent().getStringExtra("routeId");
        String routeName = getIntent().getStringExtra("routeName");
        String routeNumber = getIntent().getStringExtra("routeNumber");
        String companyName = getIntent().getStringExtra("companyName");
        String color = getIntent().getStringExtra("color");

        txtRouteName.setText(routeName);
        txtRouteNumber.setText(routeNumber);

        try {
            int bgColor = Color.parseColor(color);
            headerContainer.setBackgroundColor(bgColor);
        } catch (Exception e) {
            headerContainer.setBackgroundColor(Color.DKGRAY);
        }

        if (routeId != null) {
            loadAllSchedules(routeId);
        } else {
            Toast.makeText(this, "ID da rota não fornecido", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAllSchedules(String routeId) {
        firestore.collection("routes")
                .document(routeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> schedules = (Map<String, Object>) documentSnapshot.get("schedules");

                        List<String> weekday = schedules != null && schedules.get("weekday") != null
                                ? (List<String>) schedules.get("weekday") : Collections.emptyList();
                        List<String> saturday = schedules != null && schedules.get("saturday") != null
                                ? (List<String>) schedules.get("saturday") : Collections.emptyList();
                        List<String> sunday = schedules != null && schedules.get("sunday") != null
                                ? (List<String>) schedules.get("sunday") : Collections.emptyList();

                        fillScheduleTable(weekday, saturday, sunday);
                    } else {
                        Toast.makeText(this, "Rota não encontrada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao carregar horários", Toast.LENGTH_SHORT).show();
                });
    }

    private void fillScheduleTable(List<String> weekday, List<String> saturday, List<String> sunday) {
        TableLayout table = findViewById(R.id.schedule_table);
        table.removeAllViews();

        TableRow headerRow = new TableRow(this);
        headerRow.addView(createHeaderCell("Dias úteis"));
        headerRow.addView(createHeaderCell("Sábado"));
        headerRow.addView(createHeaderCell("Domingos/Feriados"));
        table.addView(headerRow);

        int maxSize = Math.max(weekday.size(), Math.max(saturday.size(), sunday.size()));
        for (int i = 0; i < maxSize; i++) {
            TableRow row = new TableRow(this);
            row.addView(createCell(i < weekday.size() ? weekday.get(i) : ""));
            row.addView(createCell(i < saturday.size() ? saturday.get(i) : ""));
            row.addView(createCell(i < sunday.size() ? sunday.get(i) : ""));
            table.addView(row);
        }
    }

    private TextView createCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 8, 12, 8);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16);
        tv.setBackgroundResource(R.drawable.cell_border);
        return tv;
    }

    private TextView createHeaderCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(12, 12, 12, 12);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.cell_border);
        return tv;
    }
}
