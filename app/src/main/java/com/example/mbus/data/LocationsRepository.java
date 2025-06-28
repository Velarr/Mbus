package com.example.mbus.data;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationsRepository {

    private final DatabaseReference locationsRef;
    private Map<String, Map<String, Object>> lastSnapshot = null;

    public Map<String, Map<String, Object>> getLastSnapshot() {
        return lastSnapshot;
    }


    public interface LocationsListener {
        void onLocationsUpdate(Map<String, Map<String, Object>> locations);
        void onError(String message);
    }

    public interface BusListListener {
        void onBusListUpdate(List<BusInfo> buses);
        void onError(String message);
    }

    private final Map<String, LatLng> lastKnownLocations = new HashMap<>();

    public LocationsRepository() {
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");
    }

    public void startListening(final LocationsListener listener) {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Map<String, Object>> locations = new HashMap<>();
                lastKnownLocations.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) child.getValue();
                    if (data != null) {
                        locations.put(child.getKey(), data);

                        Double lat = getDouble(data.get("latitude"));
                        Double lng = getDouble(data.get("longitude"));
                        if (lat != null && lng != null) {
                            lastKnownLocations.put(child.getKey(), new LatLng(lat, lng));
                        }
                    }
                }
                listener.onLocationsUpdate(locations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public LatLng getLastKnownLocation(String id) {
        return lastKnownLocations.get(id);
    }

    public void startListeningBuses(final BusListListener listener) {
        DatabaseReference locationsRef = FirebaseDatabase.getInstance().getReference("locations");
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onBusListUpdate(new ArrayList<>());
                    return;
                }

                List<String> idsFirestore = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) child.getValue();
                    if (data != null && data.get("id") != null) {
                        String idFirestore = (String) data.get("id");
                        idsFirestore.add(idFirestore);
                    }
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, String> companyMap = new HashMap<>();

                // Primeiro carrega a coleção de companhias
                db.collection("company").get().addOnSuccessListener(companyQuery -> {
                    for (var doc : companyQuery) {
                        companyMap.put(doc.getId(), doc.getString("name"));
                    }

                    final List<BusInfo> buses = new ArrayList<>();
                    final int totalIds = idsFirestore.size();
                    final int[] completedCount = {0};

                    if (totalIds == 0) {
                        listener.onBusListUpdate(buses);
                        return;
                    }

                    for (String id : idsFirestore) {
                        db.collection("routes")
                                .document(id)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String companyId = documentSnapshot.getString("companyId");
                                        String companyName = companyMap.get(companyId);

                                        Long routeNumberLong = documentSnapshot.getLong("routeNumber");
                                        int routeNumber = routeNumberLong != null ? routeNumberLong.intValue() : 0;
                                        String routeName = documentSnapshot.getString("routeName");
                                        String geojson = documentSnapshot.getString("geojson");
                                        String color = documentSnapshot.getString("color");

                                        BusInfo bus = new BusInfo(id, companyId, routeNumber, routeName, geojson, color);
                                        bus.setCompanyName(companyName); // <<< ESSENCIAL
                                        buses.add(bus);
                                    }

                                    completedCount[0]++;
                                    if (completedCount[0] == totalIds) {
                                        listener.onBusListUpdate(buses);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    completedCount[0]++;
                                    if (completedCount[0] == totalIds) {
                                        listener.onBusListUpdate(buses);
                                    }
                                });
                    }

                }).addOnFailureListener(e -> {
                    listener.onError("Erro ao carregar companhias: " + e.getMessage());
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }



    private Double getDouble(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
