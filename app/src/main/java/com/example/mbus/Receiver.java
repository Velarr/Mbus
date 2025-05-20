package com.example.mbus;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.*;

import com.google.maps.android.data.geojson.GeoJsonLayer;

public class Receiver {

    private final GoogleMap mMap;
    private final Context context;
    private final DatabaseReference ref;
    private final Map<String, String> userLineMap = new HashMap<>();
    private GeoJsonLayer currentLayer;
    private final List<Marker> todosOsMarcadores = new ArrayList<>();

    public Receiver(GoogleMap mMap, Context context) {
        this.mMap = mMap;
        this.context = context;
        this.ref = FirebaseDatabase.getInstance().getReference("locations");
    }

    public void startListening() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mMap.clear(); // Clears all markers, polylines, etc.
                todosOsMarcadores.clear(); // Clear your local list of markers
                userLineMap.clear();

                // The problematic lines that used undefined 'nome' and 'latLng' should be removed from here.

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String name = child.getKey(); // 'name' will be used as the title
                        Double lat = child.child("latitude").getValue(Double.class);
                        Double lng = child.child("longitude").getValue(Double.class);
                        String linha = child.child("linha").getValue(String.class);

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng); // 'position' will be used for the marker
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name) // Use 'name' here
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // If you want this icon for all markers
                                    .snippet("Clique para ver o trajeto"));

                            if (marker != null) { // Check if marker was successfully added
                                todosOsMarcadores.add(marker); // Add to your list
                                if (linha != null) {
                                    userLineMap.put(marker.getId(), linha);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Receiver", "Erro ao ler dados: " + e.getMessage());
                    }
                }

                // Listener para clique em marcador
                mMap.setOnMarkerClickListener(marker -> {
                    // ... rest of your click listener logic
                    // Make sure 'todosOsMarcadores' is correctly populated by this point.
                    LatLng position = marker.getPosition();
                    List<Marker> overlappingMarkers = new ArrayList<>();

                    // You are iterating over 'todosOsMarcadores' here, so it needs to be populated correctly.
                    for (Marker otherMarker : todosOsMarcadores) {
                        if (!otherMarker.equals(marker)) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    position.latitude, position.longitude,
                                    otherMarker.getPosition().latitude, otherMarker.getPosition().longitude,
                                    results
                            );
                            if (results[0] < 10) { // distância menor que 10 metros
                                overlappingMarkers.add(otherMarker);
                            }
                        }
                    }

                    if (!overlappingMarkers.isEmpty()) {
                        double angleStep = 360.0 / (overlappingMarkers.size() + 1);
                        double radius = 0.0001; // ~11 metros

                        int i = 0;
                        for (Marker overlapping : overlappingMarkers) {
                            double angle = Math.toRadians(i * angleStep);
                            double offsetLat = position.latitude + radius * Math.cos(angle);
                            double offsetLng = position.longitude + radius * Math.sin(angle);

                            overlapping.setPosition(new LatLng(offsetLat, offsetLng));
                            i++;
                        }
                    }

                    String linha = userLineMap.get(marker.getId());
                    if (linha != null) {
                        mostrarGeoJson(linha);
                    }

                    return false;
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Receiver", "Erro no Firebase: " + error.getMessage());
            }
        });
    }

    private void mostrarGeoJson(String nomeArquivo) {
        try {
            if (currentLayer != null) {
                currentLayer.removeLayerFromMap(); // remove anterior
            }

            // Obtem o id do recurso (R.raw.old_street, por exemplo)
            int rawResId = context.getResources().getIdentifier(
                    nomeArquivo.replace(".geojson", ""), "raw", context.getPackageName());

            if (rawResId == 0) {
                Log.e("Receiver", "Arquivo não encontrado: " + nomeArquivo);
                return;
            }

            InputStream inputStream = context.getResources().openRawResource(rawResId);
            JSONObject json = new JSONObject(new Scanner(inputStream).useDelimiter("\\A").next());
            currentLayer = new GeoJsonLayer(mMap, json);
            currentLayer.addLayerToMap();

        } catch (Exception e) {
            Log.e("Receiver", "Erro ao mostrar GeoJSON: " + e.getMessage());
        }
    }
}
