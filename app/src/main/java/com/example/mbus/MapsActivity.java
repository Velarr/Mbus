package com.example.mbus;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
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

    private final Map<String, BusInfo> routeDataMap = new HashMap<>();
    private Polyline currentPolyline;

    private final List<Marker> locationMarkers = new ArrayList<>();
    private List<Polyline> currentPolylines = new ArrayList<>();
    private List<BusInfo> currentBusList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationsRepository = new LocationsRepository();
        firestore = FirebaseFirestore.getInstance();

        LinearLayout btnBus = findViewById(R.id.btn_bus);
        btnBus.setOnClickListener(v -> {
            locationsRepository.startListeningBuses(new LocationsRepository.BusListListener() {
                @Override
                public void onBusListUpdate(List<BusInfo> buses) {
                    BusBottomSheetDialogFragment bottomSheet = new BusBottomSheetDialogFragment(buses);
                    bottomSheet.show(getSupportFragmentManager(), "bus_bottom_sheet");
                }

                @Override
                public void onError(String message) {
                    Log.e("MapsActivity", "Erro ao carregar lista de autocarros: " + message);
                }
            });
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Erro ao carregar o mapa.", Toast.LENGTH_SHORT).show();
        }
    }


    // Função chamada quando o mapa estiver pronto para ser usado
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        // Define comportamento ao clicar num marcador
        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            Log.d(TAG, "onMarkerClick: rotaId recebido = " + rotaId);

            if (rotaId == null) {
                Toast.makeText(MapsActivity.this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return true;
            }

            marker.showInfoWindow();
            drawPolylineForMarker(rotaId);
            return true;
        });

        loadAllRoutesFromFirestore();
    }

    // Carrega todos os documentos da coleção "rotas" do Firestore
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

                        // Armazena os dados da rota no mapa local
                        BusInfo rd = new BusInfo(geojson, cor, rotaNome, nrota.intValue());
                        routeDataMap.put(id, rd);
                        Log.d(TAG, "BusInfo carregado: id=" + id +
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

    // Inicia a "escuta" de atualizações de localização em tempo real
    private void startListeningLocations() {
        locationsRepository.startListening(new LocationsRepository.LocationsListener() {
            @Override
            public void onLocationsUpdate(Map<String, Map<String, Object>> locations) {
                runOnUiThread(() -> {
                    for (Marker oldMarker : locationMarkers) {
                        oldMarker.remove();
                    }
                    locationMarkers.clear();

                    // Agrupa localizações por coordenadas
                    Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
                    for (Map.Entry<String, Map<String, Object>> entry : locations.entrySet()) {
                        Map<String, Object> locData = entry.getValue();
                        Double lat = getDouble(locData.get("latitude"));
                        Double lng = getDouble(locData.get("longitude"));
                        if (lat == null || lng == null) continue;

                        String key = lat + "," + lng;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(locData);
                    }

                    // Cria os marcadores no mapa
                    for (List<Map<String, Object>> group : grouped.values()) {
                        int index = 0;
                        for (Map<String, Object> locData : group) {
                            Double lat = getDouble(locData.get("latitude"));
                            Double lng = getDouble(locData.get("longitude"));
                            String id = (String) locData.get("id");

                            if (lat != null && lng != null && id != null) {
                                LatLng originalPos = new LatLng(lat, lng);
                                LatLng offsetPos = MapUtils.offsetLatLng(originalPos, index);

                                BusInfo rd = routeDataMap.get(id);
                                String tituloMarcador = (rd != null)
                                        ? (rd.nrota + " - " + rd.rotaNome)
                                        : "Sem rota";

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(offsetPos)
                                        .title(tituloMarcador)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                Marker marker = map.addMarker(markerOptions);
                                if (marker != null) {
                                    marker.setTag(id);
                                    locationMarkers.add(marker);
                                    Log.d(TAG, "Marcador criado: tag=" + id +
                                            " | título=[" + tituloMarcador + "]" +
                                            " | posição=" + offsetPos);
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

    // Desenha a polyline(rota) correspondente ao marcador clicado
    private void drawPolylineForMarker(String routeId) {
        Log.d(TAG, "drawPolylineForMarker: iniciado para routeId=" + routeId);

        // Remove as polylines anteriores do mapa
        if (currentPolylines != null) {
            for (Polyline polyline : currentPolylines) {
                polyline.remove();
            }
            currentPolylines.clear();
        }

        BusInfo rd = routeDataMap.get(routeId);
        if (rd == null) {
            Log.e(TAG, "RouteData NULA para ID = " + routeId);
            Toast.makeText(this, "Rota não encontrada para ID: " + routeId, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject root = new JSONObject(rd.geojson);
            List<LatLng> allPointsForBounds = new ArrayList<>();

            // Caso com múltiplas features (GeoJSON padrão)
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
                            allPointsForBounds.add(latLng);
                        }

                        PolylineOptions polyOpts = new PolylineOptions()
                                .addAll(points)
                                .color(Color.parseColor(rd.corHex))
                                .width(8f)
                                .startCap(new RoundCap())
                                .endCap(new RoundCap());

                        Polyline polyline = map.addPolyline(polyOpts);
                        currentPolylines.add(polyline);
                    }
                }
            } else {
                // Caso o GeoJSON seja só uma LineString direta, sem features
                String type = root.optString("type");
                if ("LineString".equals(type)) {
                    JSONArray coords = root.getJSONArray("coordinates");
                    List<LatLng> points = new ArrayList<>();

                    for (int j = 0; j < coords.length(); j++) {
                        JSONArray point = coords.getJSONArray(j);
                        double lng = point.getDouble(0);
                        double lat = point.getDouble(1);
                        LatLng latLng = new LatLng(lat, lng);
                        points.add(latLng);
                        allPointsForBounds.add(latLng);
                    }

                    PolylineOptions polyOpts = new PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor(rd.corHex))
                            .width(8f)
                            .startCap(new RoundCap())
                            .endCap(new RoundCap());

                    Polyline polyline = map.addPolyline(polyOpts);
                    currentPolylines.add(polyline);
                }
            }

            if (allPointsForBounds.isEmpty()) {
                Log.e(TAG, "Nenhum ponto encontrado no GeoJSON para rotaId=" + routeId);
                Toast.makeText(this, "GeoJSON sem coordenadas válidas.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ajusta câmera para mostrar toda a rota desenhada
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : allPointsForBounds) {
                builder.include(p);
            }
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            Log.d(TAG, "Polylines adicionadas para rotaId=" + routeId);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao parsear/desenhar Polyline para ID=" + routeId + ": " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao desenhar rota: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Converte diferentes tipos de valores para Double, de forma segura
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
}
