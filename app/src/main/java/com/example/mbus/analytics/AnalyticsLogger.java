package com.example.mbus.analytics;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsLogger {
    public static void logEvent(String type, String rotaId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> event = new HashMap<>();
        event.put("type", type); // exemplo: rota_clicada, app_aberto, etc.
        event.put("rotaId", rotaId);
        event.put("timestamp", FieldValue.serverTimestamp());

        db.collection("eventos").add(event)
                .addOnSuccessListener(documentReference -> {
                    // Log opcional
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }
}
