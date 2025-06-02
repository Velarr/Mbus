package com.example.mbus;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LocationsRepository {   // Conecta e "escuta" em tempo real

    private final DatabaseReference locationsRef;

    public interface LocationsListener {
        void onLocationsUpdate(Map<String, Map<String, Object>> locations);
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
}
