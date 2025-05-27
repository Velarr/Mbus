package com.example.mbus;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

public class RoutesRepository {

    private final FirebaseFirestore firestore;

    public interface RouteCallback {
        void onRouteLoaded(String geoJson, String cor, String rotaNome, Long nrota);
        void onFailure(String message);
    }

    public RoutesRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void loadRouteById(String routeId, RouteCallback callback) {
        firestore.collection("rotas").document(routeId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("Rota não encontrada no Firestore");
                        return;
                    }
                    String geoJson = documentSnapshot.getString("geojson");
                    String cor = documentSnapshot.getString("cor");
                    String rotaNome = documentSnapshot.getString("rota");
                    Long nrota = documentSnapshot.getLong("nrota");

                    if (geoJson == null) {
                        callback.onFailure("GeoJSON não disponível");
                        return;
                    }

                    callback.onRouteLoaded(geoJson, cor, rotaNome, nrota);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
