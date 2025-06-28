package com.example.mbus.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mbus.data.BusInfo;
import com.example.mbus.data.LocationsRepository;
import com.example.mbus.listeners.OnBusFilterChangedListener;
import com.example.mbus.utils.MapUtils;
import com.example.mbus.utils.MarkerUtils;
import com.example.mbus.listeners.OnBusSelectedListener;

import com.example.mbus.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnBusSelectedListener, OnBusFilterChangedListener {


    private static final String TAG = "MapsActivity";

    private static final LatLng DEFAULT_LOCATION = new LatLng(32.65, -16.97);
    private static final float DEFAULT_ZOOM = 13f;

    private GoogleMap map;
    private LocationsRepository locationsRepository;
    private FirebaseFirestore firestore;

    private final Map<String, BusInfo> routeDataMap = new HashMap<>();
    private Polyline currentPolyline;

    private final List<Marker> locationMarkers = new ArrayList<>();
    private final List<Polyline> currentPolylines = new ArrayList<>();
    private boolean shouldOpenBusMenu = false;
    private List<BusInfo> pendingFilter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);


        locationsRepository = new LocationsRepository();
        firestore = FirebaseFirestore.getInstance();

        NavigationBar.setup(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o mapa.", Toast.LENGTH_SHORT).show();
        }

        shouldOpenBusMenu = getIntent().getBooleanExtra("open_bus_menu", false);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        map.setOnMarkerClickListener(marker -> {
            String routeId = (String) marker.getTag();
            Log.d(TAG, "onMarkerClick: routeId = " + routeId);

            if (routeId == null) {
                Toast.makeText(this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return true;
            }

            marker.showInfoWindow();
            drawRouteForMarker(routeId);
            return true;
        });

        if (shouldOpenBusMenu) {
            openBusMenu();
        }

        loadAllRoutesFromFirestore();
    }

    public void openBusMenu() {
        locationsRepository.startListeningBuses(new LocationsRepository.BusListListener() {
            @Override
            public void onBusListUpdate(List<BusInfo> buses) {
                BusBottomSheetDialogFragment bottomSheet = new BusBottomSheetDialogFragment(
                        buses,
                        MapsActivity.this,
                        MapsActivity.this
                );
                bottomSheet.show(getSupportFragmentManager(), "bus_bottom_sheet");

            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading bus list: " + message);
                Toast.makeText(MapsActivity.this, "Erro ao carregar autocarros.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllRoutesFromFirestore() {
        firestore.collection("routes")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                        String id = doc.getId();
                        String geojson = doc.getString("geojson");
                        String color = doc.getString("color");
                        String routeName = doc.getString("routeName");
                        Long routeNumber = doc.getLong("routeNumber");

                        if (geojson == null || color == null || routeName == null || routeNumber == null) {
                            continue;
                        }

                        BusInfo routeData = new BusInfo(geojson, color, routeName, routeNumber.intValue());
                        routeDataMap.put(id, routeData);
                    }
                    startListeningLocations();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load routes: " + e.getMessage());
                    Toast.makeText(this, "Falha ao carregar dados de rotas.", Toast.LENGTH_LONG).show();
                });
    }

    public void applyMapFilter(List<BusInfo> filteredBuses) {
        if (map == null) return;

        if (locationsRepository.getLastSnapshot() == null) {
            Log.w(TAG, "Snapshot ainda nulo. Guardando filtro para depois...");
            pendingFilter = filteredBuses;
            return;
        }

        pendingFilter = null;

        for (Marker marker : locationMarkers) {
            marker.remove();
        }
        locationMarkers.clear();

        Map<String, Map<String, Object>> snapshot = locationsRepository.getLastSnapshot();

        Set<String> idsFiltrados = new HashSet<>();
        for (BusInfo bus : filteredBuses) {
            idsFiltrados.add(bus.getId());
        }

        Log.d("Filtro", "IDs filtrados: " + idsFiltrados);
        Log.d("Snapshot", "IDs disponíveis: " + snapshot.keySet());

        Map<String, List<Map<String, Object>>> agrupado = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : snapshot.entrySet()) {
            Map<String, Object> locData = entry.getValue();
            if (locData == null) continue;

            String id = (String) locData.get("id");
            if (id == null || !idsFiltrados.contains(id)) continue;

            Double lat = getDouble(locData.get("latitude"));
            Double lng = getDouble(locData.get("longitude"));
            if (lat == null || lng == null) continue;

            String key = lat + "," + lng;
            agrupado.computeIfAbsent(key, k -> new ArrayList<>()).add(locData);
        }

        for (List<Map<String, Object>> grupo : agrupado.values()) {
            int index = 0;
            for (Map<String, Object> locData : grupo) {
                Double lat = getDouble(locData.get("latitude"));
                Double lng = getDouble(locData.get("longitude"));
                String id = (String) locData.get("id");

                if (lat != null && lng != null && id != null && idsFiltrados.contains(id)) {
                    LatLng originalPos = new LatLng(lat, lng);
                    LatLng offsetPos = MapUtils.offsetLatLng(originalPos, index);

                    BusInfo route = routeDataMap.get(id);
                    String markerTitle = (route != null)
                            ? (route.getRouteNumber() + " - " + route.getRouteName())
                            : "Sem rota";

                    int number = route != null ? route.getRouteNumber() : 0;
                    String colorStr = route != null ? route.getColor() : "#FF0000";
                    int color = Color.RED;
                    try {
                        color = Color.parseColor(colorStr);
                    } catch (IllegalArgumentException ignored) {}

                    Bitmap customIcon = MarkerUtils.createBusMarkerIcon(
                            this,
                            number,
                            75,
                            color,
                            Color.WHITE
                    );

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(offsetPos)
                            .title(markerTitle)
                            .icon(BitmapDescriptorFactory.fromBitmap(customIcon));

                    Marker marker = map.addMarker(markerOptions);
                    if (marker != null) {
                        marker.setTag(id);
                        locationMarkers.add(marker);
                    }

                    index++;
                }
            }
        }
    }

    private void startListeningLocations() {
        locationsRepository.startListening(new LocationsRepository.LocationsListener() {
            @Override
            public void onLocationsUpdate(Map<String, Map<String, Object>> locations) {
                runOnUiThread(() -> {
                    if (pendingFilter != null) {
                        Log.d("MapsActivity", "Aplicando filtro pendente");
                        applyMapFilter(pendingFilter);
                        return;
                    }

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

                                BusInfo route = routeDataMap.get(id);
                                String markerTitle = (route != null)
                                        ? (route.getRouteNumber() + " - " + route.getRouteName())
                                        : "Sem rota";

                                int number = route != null ? route.getRouteNumber() : 0;
                                String colorStr = route != null ? route.getColor() : "#FF0000";
                                int color = Color.RED;
                                try {
                                    color = Color.parseColor(colorStr);
                                } catch (IllegalArgumentException ignored) {}

                                Bitmap customIcon = MarkerUtils.createBusMarkerIcon(
                                        MapsActivity.this,
                                        number,
                                        75,
                                        color,
                                        Color.WHITE
                                );

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(offsetPos)
                                        .title(markerTitle)
                                        .icon(BitmapDescriptorFactory.fromBitmap(customIcon));

                                Marker marker = map.addMarker(markerOptions);
                                if (marker != null) {
                                    marker.setTag(id);
                                    locationMarkers.add(marker);
                                }

                                index++;
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Location error: " + message);
                runOnUiThread(() ->
                        Toast.makeText(MapsActivity.this, "Erro ao carregar localizações.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void drawRouteForMarker(String routeId) {
        if (currentPolylines != null) {
            for (Polyline polyline : currentPolylines) {
                polyline.remove();
            }
            currentPolylines.clear();
        }

        BusInfo route = routeDataMap.get(routeId);
        if (route == null) {
            Toast.makeText(this, "Rota não encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject root = new JSONObject(route.getGeojson());
            List<LatLng> boundsPoints = new ArrayList<>();

            if (root.has("features")) {
                JSONArray features = root.getJSONArray("features");
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    if (!feature.has("geometry")) continue;

                    JSONObject geometry = feature.getJSONObject("geometry");
                    String type = geometry.getString("type");

                    if ("LineString".equals(type)) {
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        List<LatLng> points = new ArrayList<>();

                        for (int j = 0; j < coords.length(); j++) {
                            JSONArray point = coords.getJSONArray(j);
                            double lng = point.getDouble(0);
                            double lat = point.getDouble(1);
                            LatLng latLng = new LatLng(lat, lng);
                            points.add(latLng);
                            boundsPoints.add(latLng);
                        }

                        int color = Color.RED;
                        try {
                            color = Color.parseColor(route.getColor());
                        } catch (IllegalArgumentException ignored) {}

                        PolylineOptions polyOpts = new PolylineOptions()
                                .addAll(points)
                                .color(color)
                                .width(8f)
                                .startCap(new RoundCap())
                                .endCap(new RoundCap());

                        Polyline polyline = map.addPolyline(polyOpts);
                        currentPolylines.add(polyline);
                    }
                }
            }

            if (boundsPoints.isEmpty()) {
                Toast.makeText(this, "GeoJSON sem coordenadas.", Toast.LENGTH_SHORT).show();
                return;
            }

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : boundsPoints) {
                builder.include(p);
            }
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        } catch (Exception e) {
            Log.e(TAG, "Error drawing route: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao desenhar rota.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBusFilterChanged(List<BusInfo> filteredBuses) {
        applyMapFilter(filteredBuses);
    }

    @Override
    public void onBusSelected(String routeId) {
        boolean found = false;

        for (Marker marker : locationMarkers) {
            if (routeId.equals(marker.getTag())) {
                marker.showInfoWindow();
                drawRouteForMarker(routeId);
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                found = true;
                break;
            }
        }

        if (!found) {
            // Tenta aplicar filtro e esperar que o marcador apareça
            Log.w(TAG, "Marcador não encontrado ainda. Tentando aplicar filtro...");

            List<BusInfo> filtroSimples = new ArrayList<>();
            BusInfo route = routeDataMap.get(routeId);
            if (route != null) {
                route = new BusInfo(routeId, "", route.getRouteNumber(), route.getRouteName(), route.getGeojson(), route.getColor());
                filtroSimples.add(route);
                applyMapFilter(filtroSimples);
            }

            // Delay de 500ms para dar tempo de aparecer
            new android.os.Handler(getMainLooper()).postDelayed(() -> onBusSelected(routeId), 500);
        }
    }


    private Double getDouble(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Float) return ((Float) obj).doubleValue();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
