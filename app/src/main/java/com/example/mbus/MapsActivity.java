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

/**
 * MapsActivity (ajustada)
 *
 * 1) Não removemos mais currentLayer em onLocationsUpdate, para que a rota
 *    exibida permaneça até o usuário clicar em outro marcador.
 * 2) setOnMarkerClickListener é configurado apenas em onMapReady, não a cada atualização de posição.
 * 3) Detalhes de campo do Firestore:
 *    - A coleção se chama "rotas" (caso use outro nome, substitua aqui).
 *    - Cada documento de rota deve ter campos:
 *         companhia   (não usado aqui)
 *         cor         (String, ex. "#BA8E23")
 *         geojson     (String, ex. JSON válido)
 *         nrota       (Long, ex. 8)
 *         rota        (String, ex. "Sta Quintéria")
 *      E o ID do documento deve ser exatamente igual ao campo `id` gravado no Realtime Database.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";

    // Posição e zoom iniciais
    private static final LatLng DEFAULT_LOCATION = new LatLng(32.65, -16.97);
    private static final float DEFAULT_ZOOM = 13f;

    private GoogleMap map;
    private LocationsRepository locationsRepository;
    private FirebaseFirestore firestore;

    /** Mapa em memória de routeId → RouteData (pré-carregado). */
    private final Map<String, RouteData> routeDataMap = new HashMap<>();

    /** Camada GeoJSON que representa a rota atualmente desenhada. */
    private GeoJsonLayer currentLayer;

    /** Lista de todos os marcadores de localização ativos, para removê-los manualmente. */
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

        // Move câmera para a posição inicial
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));

        // Configura o listener de clique uma única vez
        map.setOnMarkerClickListener(marker -> {
            String rotaId = (String) marker.getTag();
            Log.d(TAG, "onMarkerClick! Marker ID (tag) = " + rotaId);

            if (rotaId == null) {
                Toast.makeText(MapsActivity.this, "ID da rota não encontrado", Toast.LENGTH_SHORT).show();
                return true; // consome o clique para não abrir InfoWindow sem dados
            }

            drawRouteForMarker(rotaId, marker);
            return false; // permite que o próprio Maps abra a InfoWindow do marcador
        });

        // 1) Pré-carrega todas as rotas do Firestore em routeDataMap
        loadAllRoutesFromFirestore();
    }

    /**
     * Carrega todos os documentos da coleção "rotas" no Firestore e preenche routeDataMap.
     * Importante: agora buscamos "geojson" (tudo minúsculo).
     * Se a coleção tiver outro nome, altere firestore.collection("rotas") para o nome correto.
     */
    private void loadAllRoutesFromFirestore() {
        firestore.collection("rotas")  // <-- ajuste aqui se sua coleção tiver nome diferente
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshots) -> {
                    Log.d(TAG, "loadAllRoutesFromFirestore: total de documentos = " + querySnapshots.size());

                    for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                        String id = doc.getId();
                        String geojson = doc.getString("geojson"); // minúsculo
                        String cor = doc.getString("cor");
                        String rotaNome = doc.getString("rota");
                        Long nrota = doc.getLong("nrota");

                        // Pula documento se faltar algum campo
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

                    // 2) Inicia o listener de localizações após carregar as rotas
                    startListeningLocations();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao carregar rotas do Firestore: " + e.getMessage(), e);
                    Toast.makeText(MapsActivity.this,
                            "Falha ao carregar dados de rotas. Tente novamente.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Inicia o listener para receber atualizações de localização do Realtime Database.
     * A cada onLocationsUpdate:
     *   - NÃO removemos currentLayer aqui (para não apagar a rota visível).
     *   - Removemos apenas os marcadores antigos manualmente e recriamos novos.
     *   - Os títulos já são setados baseados em routeDataMap (ou "Sem rota" se não encontrar).
     */
    private void startListeningLocations() {
        locationsRepository.startListening(new LocationsRepository.LocationsListener() {
            @Override
            public void onLocationsUpdate(Map<String, Map<String, Object>> locations) {
                runOnUiThread(() -> {
                    // **Não removemos currentLayer aqui!** Mantemos a rota visível até o usuário clicar em outro marcador.

                    // 1) Remove apenas os marcadores antigos
                    for (Marker oldMarker : locationMarkers) {
                        oldMarker.remove();
                    }
                    locationMarkers.clear();

                    // 2) Agrupa localizações pela mesma lat/lng
                    Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
                    for (Map.Entry<String, Map<String, Object>> entry : locations.entrySet()) {
                        Map<String, Object> locData = entry.getValue();
                        Double lat = getDouble(locData.get("latitude"));
                        Double lng = getDouble(locData.get("longitude"));
                        if (lat == null || lng == null) continue;

                        String key = lat + "," + lng;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(locData);
                    }

                    // 3) Para cada grupo, cria marcadores com offset
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

                    // **Não redefinimos o listener de clique aqui**, pois já o configuramos em onMapReady.
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

    /**
     * Desenha o GeoJSON da rota correspondente a routeId (já em routeDataMap).
     * Remove a camada anterior (se houver) e desenha a nova.
     */
    private void drawRouteForMarker(String routeId, Marker marker) {
        Log.d(TAG, "drawRouteForMarker: procurando RouteData para ID = " + routeId);

        // 1) Remove camada antiga (se existir)
        if (currentLayer != null) {
            currentLayer.removeLayerFromMap();
            currentLayer = null;
        }

        // 2) Busca RouteData em memória
        RouteData rd = routeDataMap.get(routeId);
        if (rd == null) {
            Log.e(TAG, "drawRouteForMarker: RouteData NULA para ID = " + routeId);
            Toast.makeText(this, "Rota não encontrada para ID: " + routeId, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 3) Converte string "geojson" para JSONObject
            JSONObject geoJsonObject = new JSONObject(rd.geojson);
            currentLayer = new GeoJsonLayer(map, geoJsonObject);

            // 4) Aplica cor e largura a cada feature LineString
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
