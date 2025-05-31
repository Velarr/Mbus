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
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";

    private static final LatLng DEFAULT_LOCATION = new LatLng(32.65, -16.97);
    private static final float DEFAULT_ZOOM = 13f;

    private GoogleMap map;
    private LocationsRepository locationsRepository;
    private FirebaseFirestore firestore;

    private final Map<String, RouteData> routeDataMap = new HashMap<>();
    private GeoJsonLayer currentLayer;

    private final List<Marker> locationMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationsRepository = new LocationsRepository();
        firestore = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o mapa.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            Log.d(TAG, "onMarkerClick! Marker ID (tag) = " + rotaId);

            if (rotaId == null) {
                Toast.makeText(MapsActivity.this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return true;
            }

            drawRouteForMarker(rotaId, marker);
            return false;
        });

        loadAllRoutesFromFirestore();
    }

    private void loadAllRoutesFromFirestore() {
        firestore.collection("rotas")
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshots) -> {
                    Log.d(TAG, "loadAllRoutesFromFirestore: total de documentos = " + querySnapshots.size());

                    for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                        String id = doc.getId();
                        String geojson = doc.getString("geojson");
                        String cor = doc.getString("cor");
                        String rotaNome = doc.getString("rota");
                        Long nrota = doc.getLong("nrota");

                        if (geojson == null || cor == null || rotaNome == null || nrota == null) {
                            Log.w(TAG, "Documento 'rotas/" + id + "' incompleto; pulando. " +
                                    "geojson=" + (geojson != null) +
                                    ", cor=" + (cor != null) +
                                    ", rota=" + (rotaNome != null) +
                                    ", nrota=" + (nrota != null));
                            continue;
                        }

                        RouteData rd = new RouteData(geojson, cor, rotaNome, nrota);
                        routeDataMap.put(id, rd);
                        Log.d(TAG, "RouteData carregado: id=" + id +
                                " | rotaNome=" + rotaNome +
                                " | nrota=" + nrota +
                                " | cor=" + cor);
                    }

                    Log.d(TAG, "loadAllRoutesFromFirestore: routeDataMap.size() = " + routeDataMap.size());

                    startListeningLocations();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao carregar rotas do Firestore: " + e.getMessage(), e);
                    Toast.makeText(MapsActivity.this,
                            "Falha ao carregar dados de rotas. Tente novamente.", Toast.LENGTH_LONG).show();
                });
    }

    private void startListeningLocations() {
        locationsRepository.startListening(new LocationsRepository.LocationsListener() {
            @Override
            public void onLocationsUpdate(Map<String, Map<String, Object>> locations) {
                runOnUiThread(() -> {

                    for (Marker oldMarker : locationMarkers) {
                        oldMarker.remove();
                    }
                    locationMarkers.clear();

                    Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
                    for (Map.Entry<String, Map<String, Object>> entry : locations.entrySet()) {
                        Map<String, Object> locData = entry.getValue();
                        Double lat = getDouble(locData.get("latitude"));
                        Double lng = getDouble(locData.get("longitude"));
                        if (lat == null || lng == null) continue;

                        String key = lat + "," + lng;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(locData);
                    }

                    for (List<Map<String, Object>> group : grouped.values()) {
                        int index = 0;
                        for (Map<String, Object> locData : group) {
                            Double lat = getDouble(locData.get("latitude"));
                            Double lng = getDouble(locData.get("longitude"));
                            String id = (String) locData.get("id");

                            if (lat != null && lng != null && id != null) {
                                LatLng originalPos = new LatLng(lat, lng);
                                LatLng offsetPos = MapUtils.offsetLatLng(originalPos, index);

                                // Define título com base em routeDataMap
                                RouteData rd = routeDataMap.get(id);
                                String tituloMarcador;
                                if (rd != null) {
                                    tituloMarcador = rd.nrota + " - " + rd.rotaNome;
                                } else {
                                    tituloMarcador = "Sem rota";
                                }

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(offsetPos)
                                        .title(tituloMarcador)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                Marker marker = map.addMarker(markerOptions);
                                if (marker != null) {
                                    marker.setTag(id);
                                    locationMarkers.add(marker);
                                    Log.d(TAG, "Marcador criado: tag = " + id +
                                            " | título = [" + tituloMarcador + "]" +
                                            " | posição = " + offsetPos);
                                }
                                index++;
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Erro ao receber localizações: " + message);
                runOnUiThread(() ->
                        Toast.makeText(MapsActivity.this, "Erro ao carregar localizações.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void drawRouteForMarker(String routeId, Marker marker) {
        Log.d(TAG, "drawRouteForMarker: procurando RouteData para ID = " + routeId);

        if (currentLayer != null) {
            currentLayer.removeLayerFromMap();
            currentLayer = null;
        }

        RouteData rd = routeDataMap.get(routeId);
        if (rd == null) {
            Log.e(TAG, "drawRouteForMarker: RouteData NULA para ID = " + routeId);
            Toast.makeText(this, "Rota não encontrada para ID: " + routeId, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject geoJsonObject = new JSONObject(rd.geojson);
            currentLayer = new GeoJsonLayer(map, geoJsonObject);

            String corValida = rd.corHex;
            for (GeoJsonFeature feature : currentLayer.getFeatures()) {
                if (feature.getGeometry() instanceof com.google.maps.android.data.geojson.GeoJsonLineString) {
                    GeoJsonLineStringStyle style = feature.getLineStringStyle();
                    if (style == null) {
                        style = new GeoJsonLineStringStyle();
                        feature.setLineStringStyle(style);
                    }
                    style.setColor(Color.parseColor(corValida));
                    style.setWidth(8);
                }
            }


            currentLayer.addLayerToMap();
            Log.d(TAG, "drawRouteForMarker: rota desenhada para ID = " + routeId);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao desenhar GeoJSON para ID = " + routeId + ": " + e.getMessage(), e);
            Toast.makeText(this,
                    "Erro ao desenhar rota: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Double getDouble(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private static class RouteData {
        final String geojson;
        final String corHex;
        final String rotaNome;
        final Long   nrota;

        RouteData(String geojson, String corHex, String rotaNome, Long nrota) {
            this.geojson = geojson;
            this.corHex  = corHex;
            this.rotaNome = rotaNome;
            this.nrota   = nrota;
        }
    }
}
