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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LocationsRepository locationsRepository;
    private RoutesRepository routesRepository;

    private GeoJsonLayer currentLayer;
    private Marker selectedMarker;

    private static final String TAG = "MapsActivity";
    private static final LatLng DEFAULT_LOCATION = new LatLng(32.65, -16.97);
    private static final float DEFAULT_ZOOM = 13f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationsRepository = new LocationsRepository();
        routesRepository = new RoutesRepository();

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

        startListeningLocations();

        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            if (rotaId == null) {
                Toast.makeText(this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return false;
            }

            selectedMarker = marker;
            loadGeoJsonFromRouteId(rotaId);
            return false;
        });
    }

    private void startListeningLocations() {
        locationsRepository.startListening(new LocationsRepository.LocationsListener() {
            @Override
            public void onLocationsUpdate(Map<String, Map<String, Object>> locations) {
                runOnUiThread(() -> {
                    map.clear();

                    if (currentLayer != null) {
                        currentLayer.removeLayerFromMap();
                        currentLayer = null;
                    }

                    // Agrupa marcadores pela mesma posição (lat,lng)
                    Map<String, List<Map<String, Object>>> grouped = new HashMap<>();

                    for (Map.Entry<String, Map<String, Object>> entry : locations.entrySet()) {
                        Map<String, Object> locData = entry.getValue();
                        Double lat = getDouble(locData.get("latitude"));
                        Double lng = getDouble(locData.get("longitude"));
                        if (lat == null || lng == null) continue;

                        String key = lat + "," + lng;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(locData);
                    }

                    // Para cada grupo, adiciona marcadores com offset para evitar sobreposição
                    for (List<Map<String, Object>> group : grouped.values()) {
                        int index = 0;
                        for (Map<String, Object> locData : group) {
                            Double lat = getDouble(locData.get("latitude"));
                            Double lng = getDouble(locData.get("longitude"));
                            String id = (String) locData.get("id");

                            if (lat != null && lng != null && id != null) {
                                LatLng originalPos = new LatLng(lat, lng);
                                LatLng offsetPos = MapUtils.offsetLatLng(originalPos, index);

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(offsetPos)
                                        .title("Carregando...")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                Marker marker = map.addMarker(markerOptions);
                                if (marker != null) marker.setTag(id);

                                index++;
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Erro ao receber localizações: " + message);
                runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Erro ao carregar localizações.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadGeoJsonFromRouteId(String routeId) {
        if (currentLayer != null) {
            currentLayer.removeLayerFromMap();
            currentLayer = null;
        }

        routesRepository.loadRouteById(routeId, new RoutesRepository.RouteCallback() {
            @Override
            public void onRouteLoaded(String geoJson, String corHex, String rotaNome, Long nrota) {
                runOnUiThread(() -> {
                    try {
                        JSONObject geoJsonObject = new JSONObject(geoJson);
                        currentLayer = new GeoJsonLayer(map, geoJsonObject);

                        String corValida = "#FF0000"; // padrão vermelho
                        if (corHex != null && corHex.trim().matches("#[A-Fa-f0-9]{6}([A-Fa-f0-9]{2})?")) {
                            corValida = corHex.trim();
                        }

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

                        if (selectedMarker != null) {
                            if (rotaNome != null && nrota != null) {
                                selectedMarker.setTitle(nrota + " - " + rotaNome);
                            } else if (rotaNome != null) {
                                selectedMarker.setTitle(rotaNome);
                            } else if (nrota != null) {
                                selectedMarker.setTitle(String.valueOf(nrota));
                            }
                            selectedMarker.showInfoWindow();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao carregar GeoJSON", e);
                        Toast.makeText(MapsActivity.this, "Erro ao carregar rota: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> Toast.makeText(MapsActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Método utilitário para converter objeto do Map em Double
    private Double getDouble(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
