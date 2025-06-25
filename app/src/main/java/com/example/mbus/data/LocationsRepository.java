package com.example.mbus.data;

import androidx.annotation.NonNull;

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

    public interface LocationsListener {
        void onLocationsUpdate(Map<String, Map<String, Object>> locations);
        void onError(String message);
    }

    public interface BusListListener {
        void onBusListUpdate(List<BusInfo> buses);
        void onError(String message);
    }

    public LocationsRepository() {
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");
    }

    public void startListening(final LocationsListener listener) {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Map<String, Object>> locations = new HashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) child.getValue();
                    if (data != null) {
                        locations.put(child.getKey(), data);
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

    public void startListeningBuses(final BusListListener listener) {
        DatabaseReference locationsRef = FirebaseDatabase.getInstance().getReference("locations");
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onBusListUpdate(new ArrayList<>());
                    return;
                }

                List<BusInfo> buses = new ArrayList<>();
                List<String> idsFirestore = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> data = (Map<String, Object>) child.getValue();
                    if (data != null && data.get("id") != null) {
                        String idFirestore = (String) data.get("id");
                        idsFirestore.add(idFirestore);
                    }
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
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
                                    String companyName = documentSnapshot.getString("companyName");
                                    Long routeNumberLong = documentSnapshot.getLong("routeNumber");
                                    int routeNumber = routeNumberLong != null ? routeNumberLong.intValue() : 0;
                                    String routeName = documentSnapshot.getString("routeName");
                                    String geojson = documentSnapshot.getString("geojson");
                                    String color = documentSnapshot.getString("color");

                                    buses.add(new BusInfo(id, companyName, routeNumber, routeName, geojson, color));
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }
}
