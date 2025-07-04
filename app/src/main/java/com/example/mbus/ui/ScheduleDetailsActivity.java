package com.example.mbus.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.util.Locale;

public class ScheduleDetailsActivity extends AppCompatActivity {

    private TextView txtRouteInfo;
    private LinearLayout colWeekday, colSaturday, colSunday;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_details);

        txtRouteInfo = findViewById(R.id.txt_route_info);
        colWeekday = findViewById(R.id.col_weekday);
        colSaturday = findViewById(R.id.col_saturday);
        colSunday = findViewById(R.id.col_sunday);

        firestore = FirebaseFirestore.getInstance();

        String routeId = getIntent().getStringExtra("routeId");
        String routeName = getIntent().getStringExtra("routeName");
        String routeNumber = getIntent().getStringExtra("routeNumber");
        String companyName = getIntent().getStringExtra("companyName");

        txtRouteInfo.setText(companyName + " - " + routeNumber + " " + routeName);

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
                        var schedules = (java.util.Map<String, Object>) documentSnapshot.get("schedules");

                        showSchedule(schedules, "weekday", colWeekday);
                        showSchedule(schedules, "saturday", colSaturday);
                        showSchedule(schedules, "sunday", colSunday);
                    } else {
                        Toast.makeText(this, "Rota não encontrada", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao carregar horários", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSchedule(java.util.Map<String, Object> schedules, String key, LinearLayout column) {
        column.removeAllViews();

        if (schedules == null || !schedules.containsKey(key)) return;

        List<String> times = (List<String>) schedules.get(key);
        if (times == null || times.isEmpty()) return;

        Collections.sort(times); // Opcional

        for (String time : times) {
            TextView txt = new TextView(this);
            txt.setText(time);
            txt.setGravity(Gravity.CENTER_HORIZONTAL);
            txt.setPadding(4, 8, 4, 8);
            txt.setTextSize(16);
            column.addView(txt);
        }
    }
}
