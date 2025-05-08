package com.example.mbus;

import android.content.Context;
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

    public Receiver(GoogleMap mMap, Context context) {
        this.mMap = mMap;
        this.context = context;
        this.ref = FirebaseDatabase.getInstance().getReference("locations");
    }

    public void startListening() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mMap.clear();
                userLineMap.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String name = child.getKey();
                        Double lat = child.child("latitude").getValue(Double.class);
                        Double lng = child.child("longitude").getValue(Double.class);
                        String linha = child.child("linha").getValue(String.class);

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .snippet("Clique para ver o trajeto"));

                            if (marker != null && linha != null) {
                                userLineMap.put(marker.getId(), linha);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Receiver", "Erro ao ler dados: " + e.getMessage());
                    }
                }

                // Listener para clique em marcador
                mMap.setOnMarkerClickListener(marker -> {
                    String linha = userLineMap.get(marker.getId());
                    if (linha != null) {
                        mostrarGeoJson(linha);
                    }
                    return false; // Retorna false para também mostrar o info window
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
