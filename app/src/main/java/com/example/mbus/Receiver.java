package com.example.mbus;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.*;

import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.*;

public class Receiver {

    private final GoogleMap map;
    private final Context context;
    private final DatabaseReference firebaseRef;
    private final Map<String, String> markerLineMap = new HashMap<>();
    private final List<Marker> allMarkers = new ArrayList<>();
    private GeoJsonLayer currentLayer;

    public Receiver(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
        this.firebaseRef = FirebaseDatabase.getInstance().getReference("locations");
    }

    public void startListening() {
        firebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                map.clear();
                allMarkers.clear();
                markerLineMap.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String name = child.getKey();
                        Double lat = child.child("latitude").getValue(Double.class);
                        Double lng = child.child("longitude").getValue(Double.class);
                        String line = child.child("linha").getValue(String.class);

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            Marker marker = map.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                    .snippet("Click to view route"));

                            if (marker != null) {
                                allMarkers.add(marker);
                                if (line != null) {
                                    markerLineMap.put(marker.getId(), line);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Receiver", "Error reading data: " + e.getMessage());
                    }
                }

                map.setOnMarkerClickListener(marker -> {
                    LatLng position = marker.getPosition();
                    List<Marker> overlappingMarkers = new ArrayList<>();

                    for (Marker otherMarker : allMarkers) {
                        if (!otherMarker.equals(marker)) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    position.latitude, position.longitude,
                                    otherMarker.getPosition().latitude, otherMarker.getPosition().longitude,
                                    results
                            );
                            if (results[0] < 10) {
                                overlappingMarkers.add(otherMarker);
                            }
                        }
                    }

                    if (!overlappingMarkers.isEmpty()) {
                        double angleStep = 360.0 / (overlappingMarkers.size() + 1);
                        double radius = 0.0001;

                        int i = 0;
                        for (Marker overlapping : overlappingMarkers) {
                            double angle = Math.toRadians(i * angleStep);
                            double offsetLat = position.latitude + radius * Math.cos(angle);
                            double offsetLng = position.longitude + radius * Math.sin(angle);

                            overlapping.setPosition(new LatLng(offsetLat, offsetLng));
                            i++;
                        }
                    }

                    String line = markerLineMap.get(marker.getId());
                    if (line != null) {
                        showGeoJson(line);
                    }

                    return false;
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Receiver", "Firebase error: " + error.getMessage());
            }
        });
    }

    private void showGeoJson(String fileNameWithoutExtension) {
        try {
            if (currentLayer != null) {
                currentLayer.removeLayerFromMap();
                currentLayer = null;
            }

            int rawResId = context.getResources().getIdentifier(
                    fileNameWithoutExtension.replace(".geojson", ""), "raw", context.getPackageName());

            if (rawResId == 0) {
                Log.e("Receiver", "GeoJSON file not found: " + fileNameWithoutExtension);
                return;
            }

            InputStream inputStream = context.getResources().openRawResource(rawResId);
            JSONObject jsonObject = new JSONObject(convertStreamToString(inputStream));
            currentLayer = new GeoJsonLayer(map, jsonObject);
            currentLayer.addLayerToMap();

        } catch (JSONException | java.io.IOException e) {
            Log.e("Receiver", "Error showing GeoJSON: " + e.getMessage());
        }
    }

    private String convertStreamToString(InputStream inputStream) throws java.io.IOException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
