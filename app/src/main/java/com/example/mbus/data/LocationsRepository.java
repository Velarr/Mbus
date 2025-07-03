package com.example.mbus.data;

import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class LocationsRepository {

    private final DatabaseReference locationsRef;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, LatLng> lastKnownLocations = new HashMap<>();
    private Map<String, Map<String, Object>> lastSnapshot = null;

    public LocationsRepository() {
        locationsRef = FirebaseDatabase.getInstance().getReference("locations");
    }

    public Map<String, Map<String, Object>> getLastSnapshot() {
        return lastSnapshot;
    }

    public interface LocationsListener {
        void onLocationsUpdate(Map<String, Map<String, Object>> locations);
        void onError(String message);
    }

    public interface BusListListener {
        void onBusListUpdate(List<BusInfo> buses);
        void onError(String message);
    }

    public interface Callback {
        void onComplete(List<BusRoute> routes);
        void onError(Exception e);
    }

    public LatLng getLastKnownLocation(String id) {
        return lastKnownLocations.get(id);
    }

    public void startListening(final LocationsListener listener) {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Map<String, Object>> locations = new HashMap<>();
                lastKnownLocations.clear();

                for (DataSnapshot rotaSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot child : rotaSnapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (data != null) {
                            String uid = child.getKey();
                            locations.put(uid, data);

                            Double lat = getDouble(data.get("latitude"));
                            Double lng = getDouble(data.get("longitude"));
                            if (lat != null && lng != null) {
                                lastKnownLocations.put(uid, new LatLng(lat, lng));
                            }
                        }
                    }
                }

                lastSnapshot = locations;
                listener.onLocationsUpdate(locations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public void startListeningBuses(final BusListListener listener) {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> idsFirestore = new ArrayList<>();

                for (DataSnapshot rotaSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot child : rotaSnapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) child.getValue();
                        if (data != null && data.get("id") != null) {
                            String idFirestore = (String) data.get("id");
                            idsFirestore.add(idFirestore);
                        }
                    }
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, String> companyMap = new HashMap<>();

                db.collection("company").get().addOnSuccessListener(companyQuery -> {
                    for (var doc : companyQuery) {
                        companyMap.put(doc.getId(), doc.getString("name"));
                    }

                    final List<BusInfo> buses = new ArrayList<>();
                    final int totalIds = idsFirestore.size();
                    final int[] completedCount = {0};

                    if (totalIds == 0) {
                        listener.onBusListUpdate(buses);
                        return;
                    }

                    for (String id : idsFirestore) {
                        db.collection("routes")
                                .document(id)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String companyId = documentSnapshot.getString("companyId");
                                        String companyName = companyMap.get(companyId);

                                        Long routeNumberLong = documentSnapshot.getLong("routeNumber");
                                        int routeNumber = routeNumberLong != null ? routeNumberLong.intValue() : 0;
                                        String routeName = documentSnapshot.getString("routeName");
                                        String geojson = documentSnapshot.getString("geojson");
                                        String color = documentSnapshot.getString("color");

                                        BusInfo bus = new BusInfo(id, companyId, routeNumber, routeName, geojson, color);
                                        bus.setCompanyName(companyName);
                                        buses.add(bus);
                                    }

                                    completedCount[0]++;
                                    if (completedCount[0] == totalIds) {
                                        listener.onBusListUpdate(buses);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    completedCount[0]++;
                                    if (completedCount[0] == totalIds) {
                                        listener.onBusListUpdate(buses);
                                    }
                                });
                    }

                }).addOnFailureListener(e -> {
                    listener.onError("Erro ao carregar companhias: " + e.getMessage());
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public void getAllRoutes(Callback callback) {
        db.collection("routes")
                .get()
                .addOnSuccessListener(qs -> {
                    List<BusRoute> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String id = doc.getId();
                        String geojson = doc.getString("geojson");
                        String routeName = doc.getString("routeName");
                        Long routeNumberLong = doc.getLong("routeNumber");
                        int routeNumber = routeNumberLong != null ? routeNumberLong.intValue() : 0;
                        String color = doc.getString("color");

                        List<List<LatLng>> multiLinePoints = new ArrayList<>();

                        if (geojson != null) {
                            try {
                                JSONObject geo = new JSONObject(geojson);
                                JSONArray features = geo.getJSONArray("features");

                                for (int i = 0; i < features.length(); i++) {
                                    JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");
                                    String type = geometry.getString("type");

                                    if ("LineString".equals(type)) {
                                        JSONArray coords = geometry.getJSONArray("coordinates");
                                        List<LatLng> line = new ArrayList<>();
                                        for (int j = 0; j < coords.length(); j++) {
                                            JSONArray coord = coords.getJSONArray(j);
                                            double lng = coord.getDouble(0);
                                            double lat = coord.getDouble(1);
                                            line.add(new LatLng(lat, lng));
                                        }
                                        multiLinePoints.add(line);
                                    } else if ("MultiLineString".equals(type)) {
                                        JSONArray lines = geometry.getJSONArray("coordinates");
                                        for (int j = 0; j < lines.length(); j++) {
                                            JSONArray lineCoords = lines.getJSONArray(j);
                                            List<LatLng> line = new ArrayList<>();
                                            for (int k = 0; k < lineCoords.length(); k++) {
                                                JSONArray coord = lineCoords.getJSONArray(k);
                                                double lng = coord.getDouble(0);
                                                double lat = coord.getDouble(1);
                                                line.add(new LatLng(lat, lng));
                                            }
                                            multiLinePoints.add(line);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (!multiLinePoints.isEmpty()) {
                            BusRoute route = new BusRoute(id, multiLinePoints, routeName, routeNumber, color);
                            list.add(route);
                        }
                    }

                    callback.onComplete(list);
                })
                .addOnFailureListener(callback::onError);
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
