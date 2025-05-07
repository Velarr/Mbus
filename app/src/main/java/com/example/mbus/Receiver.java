package com.example.mbus;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Receiver {

    private final GoogleMap mMap;
    private final Context context;
    private final DatabaseReference ref;

    public Receiver(GoogleMap mMap, Context context) {
        this.mMap = mMap;
        this.context = context;
        this.ref = FirebaseDatabase.getInstance().getReference("locations"); // <<== Aqui você inicializa 'ref'
    }

    public void startListening() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mMap.clear(); // Limpa os marcadores anteriores

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String name = child.getKey();
                        Double lat = child.child("latitude").getValue(Double.class);
                        Double lng = child.child("longitude").getValue(Double.class);

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .snippet("Lat: " + lat + ", Lng: " + lng));
                        }
                    } catch (Exception e) {
                        Log.e("Receiver", "Erro ao ler dados do usuário: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Receiver", "Erro no Firebase: " + error.getMessage());
            }
        });
    }
}
