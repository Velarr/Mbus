package com.example.mbus;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private DatabaseReference locationsRef;
    private FirebaseFirestore firestore;
    private GeoJsonLayer currentLayer;
    private Marker selectedMarker;  // marcador clicado atualmente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        firestore = FirebaseFirestore.getInstance();
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        LatLng defaultLatLng = new LatLng(32.65, -16.97);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 13f));

        listenLocationsRealtime();

        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            if (rotaId == null) {
                Toast.makeText(this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return false;
            }

            selectedMarker = marker;  // guarda marcador clicado
            loadGeoJsonFromFirestore(rotaId);
            return false; // permite mostrar info window normalmente
        });
    }

    private void listenLocationsRealtime() {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                map.clear();

                if (currentLayer != null) {
                    currentLayer.removeLayerFromMap();
                    currentLayer = null;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.child("id").getValue(String.class);
                    String rota = child.child("rota").getValue(String.class);
                    Long nrotaLong = child.child("nrota").getValue(Long.class);
                    Double lat = child.child("latitude").getValue(Double.class);
                    Double lng = child.child("longitude").getValue(Double.class);

                    if (lat != null && lng != null && id != null) {
                        LatLng position = new LatLng(lat, lng);

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(position)
                                .title(rota != null ? rota : "Rota")
                                .snippet("Nº: " + (nrotaLong != null ? nrotaLong : "-"))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                        Marker marker = map.addMarker(markerOptions);
                        marker.setTag(id);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapsActivity", "Erro Realtime DB: " + error.getMessage());
                Toast.makeText(MapsActivity.this, "Erro ao carregar localizações.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGeoJsonFromFirestore(String rotaId) {
        if (currentLayer != null) {
            currentLayer.removeLayerFromMap();
            currentLayer = null;
        }

        firestore.collection("rotas").document(rotaId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Rota não encontrada no Firestore", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String geoJsonString = documentSnapshot.getString("geojson");
                    String corHex = documentSnapshot.getString("cor");
                    String rotaNome = documentSnapshot.getString("rota");
                    Long nrota = documentSnapshot.getLong("nrota");

                    if (geoJsonString == null) {
                        Toast.makeText(this, "GeoJSON não disponível", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject geoJsonObject = new JSONObject(geoJsonString);
                        currentLayer = new GeoJsonLayer(map, geoJsonObject);

                        // Verifica e ajusta a cor da linha (aceita #RRGGBB ou #AARRGGBB)
                        String corValida = "#FF0000"; // vermelho padrão
                        if (corHex != null && corHex.trim().startsWith("#") && (corHex.trim().length() == 7 || corHex.trim().length() == 9)) {
                            corValida = corHex.trim();
                        }
                        Log.d("MapsActivity", "Cor da linha do GeoJSON: " + corValida);

                        for (GeoJsonFeature feature : currentLayer.getFeatures()) {
                            if (feature.getGeometry() instanceof com.google.maps.android.data.geojson.GeoJsonLineString) {
                                GeoJsonLineStringStyle lineStringStyle = feature.getLineStringStyle();
                                if (lineStringStyle == null) {
                                    lineStringStyle = new GeoJsonLineStringStyle();
                                    feature.setLineStringStyle(lineStringStyle);
                                }
                                lineStringStyle.setColor(Color.parseColor(corValida));
                                lineStringStyle.setWidth(8);
                            }
                        }

                        currentLayer.addLayerToMap();

                        // Atualiza info window do marcador selecionado com dados do Firestore
                        if (selectedMarker != null) {
                            if (rotaNome != null) selectedMarker.setTitle(rotaNome);
                            if (nrota != null) selectedMarker.setSnippet("Nº: " + nrota);
                            selectedMarker.showInfoWindow();  // atualiza info window
                        }

                    } catch (Exception e) {
                        Log.e("MapsActivity", "Erro ao carregar GeoJSON: ", e);
                        Toast.makeText(this, "Erro ao carregar rota: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MapsActivity", "Erro Firestore: ", e);
                    Toast.makeText(this, "Falha ao buscar rota no Firestore.", Toast.LENGTH_SHORT).show();
                });
    }
}
