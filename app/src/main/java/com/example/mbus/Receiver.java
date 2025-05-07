package com.example.mbus;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Receiver {

    private final GoogleMap mMap;
    private final Context context;
    private Marker firebaseMarker;

    public Receiver(GoogleMap map, Context ctx) {
        this.mMap = map;
        this.context = ctx;
    }

    public void startListening() {
        DatabaseReference locationRef = FirebaseDatabase.getInstance().getReference("locations/secondaryApp");

        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lng = snapshot.child("longitude").getValue(Double.class);

                    if (lat != null && lng != null) {
                        LatLng newLocation = new LatLng(lat, lng);

                        if (firebaseMarker == null) {
                            firebaseMarker = mMap.addMarker(new MarkerOptions()
                                    .position(newLocation)
                                    .title("Localização Firebase"));
                        } else {
                            firebaseMarker.setPosition(newLocation);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(context, "Erro ao ler localização do Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
