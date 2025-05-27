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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private DatabaseReference locationsRef;
    private FirebaseFirestore firestore;
    private GeoJsonLayer currentLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Inicializa Firestore
        firestore = FirebaseFirestore.getInstance();

        // Inicializa Realtime Database referência
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");

        // Obtém o SupportMapFragment e solicita callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Centraliza o mapa num ponto default (opcional)
        LatLng defaultLatLng = new LatLng(32.65, -16.97);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 13f));

        // Escuta Realtime Database para receber localizações
        listenLocationsRealtime();

        // Configura clique em marcador para carregar GeoJSON do Firestore
        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            if (rotaId == null) {
                Toast.makeText(this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return false;
            }

            loadGeoJsonFromFirestore(rotaId);
            return false; // Mostrar info window também
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
                    String cor = child.child("cor").getValue(String.class);
                    Double lat = child.child("latitude").getValue(Double.class);
                    Double lng = child.child("longitude").getValue(Double.class);

                    if (lat != null && lng != null && id != null) {
                        LatLng position = new LatLng(lat, lng);

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(position)
                                .title(rota != null ? rota : "Rota")
                                .snippet("Nº: " + (nrotaLong != null ? nrotaLong : "-") + " | Toque para ver rota")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                        Marker marker = map.addMarker(markerOptions);

                        // Armazena o ID da rota Firestore no marcador para buscar depois
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

                    if (geoJsonString == null) {
                        Toast.makeText(this, "GeoJSON não disponível", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject geoJsonObject = new JSONObject(geoJsonString);
                        currentLayer = new GeoJsonLayer(map, geoJsonObject);

                        // Aplica cor nas linhas
                        for (GeoJsonFeature feature : currentLayer.getFeatures()) {
                            if (feature.getGeometry() instanceof com.google.maps.android.data.geojson.GeoJsonLineString) {
                                if (feature.getLineStringStyle() != null) {
                                    feature.getLineStringStyle().setColor(Color.parseColor(corHex != null ? corHex : "#FF0000"));
                                    feature.getLineStringStyle().setWidth(8);
                                } else {
                                    Log.w("GeoJson", "LineStringStyle is null for this feature.");
                                }
                            } else {
                                Log.w("GeoJson", "Feature is not a LineString: " + feature.getGeometry().getClass().getSimpleName());
                            }
                        }


                        currentLayer.addLayerToMap();

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
