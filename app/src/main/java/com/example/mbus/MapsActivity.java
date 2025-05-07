package com.example.mbus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.mbus.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private int touchCount = 0; // Variável para contar os toques
    private Handler handler = new Handler(); // Handler para dar um pequeno delay e evitar problemas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        LinearLayout btnOptions = findViewById(R.id.btn_bus);


        // Inicializa o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configura o menu de opções
        MapOptionsMenu menu = new MapOptionsMenu(this, btnOptions, new MapOptionsMenu.OnOptionSelectedListener() {
            @Override
            public void onOptionSelected(int geoJsonResId) {
                loadGeoJsonLayer(geoJsonResId);
            }
        });

        btnOptions.setOnClickListener(v -> menu.show());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                    }
                });

        // Firebase: buscar a localização do outro app
        Receiver receiver = new Receiver(mMap, this);
        receiver.startListening();


        // Definir os limites do mapa para a Madeira
        LatLngBounds madeiraBounds = new LatLngBounds(
                new LatLng(32.50, -17.30), // SW bounds
                new LatLng(33.10, -16.30)  // NE bounds
        );

        mMap.setLatLngBoundsForCameraTarget(madeiraBounds);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(madeiraBounds, 0));
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(18.0f);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, ativa a localização
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
        Toast.makeText(this, "Permissão de localização concedida", Toast.LENGTH_SHORT).show();
    }

    private void loadGeoJsonLayer(int geoJsonResId) {
        mMap.clear();
        configureMap();

        int color;
        if (geoJsonResId == R.raw.vr_line) {
            color = 0xFFFF0000; // Vermelho para a Via Rápida
        } else if (geoJsonResId == R.raw.old_street_line) {
            color = 0xFF0000FF; // Azul para o Caminho Velho
        } else {
            color = 0xFF00FF00; // Verde padrão
        }

        GeoJsonUtils.addGeoJsonLayer(this, mMap, geoJsonResId, color);
    }

    private void configureMap() {
        LatLngBounds madeiraBounds = new LatLngBounds(
                new LatLng(32.50, -17.30), // SW bounds
                new LatLng(33.10, -16.30)  // NE bounds
        );

        mMap.setLatLngBoundsForCameraTarget(madeiraBounds);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(madeiraBounds, 0));
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(18.0f);
    }

    // Método para calcular a distância até as rotas e escolher a mais próxima
    private void calculateClosestRoute(LatLng userLocation) {
        // Carregar as GeoJsonLayer das rotas
        GeoJsonLayer vrLineLayer = GeoJsonUtils.loadGeoJsonLayer(this, R.raw.vr_line);
        GeoJsonLayer oldStreetLineLayer = GeoJsonUtils.loadGeoJsonLayer(this, R.raw.old_street_line);

        // Calcular a distância até as rotas
        double vrDistance = calculateDistanceToGeoJson(userLocation, vrLineLayer);
        double oldStreetDistance = calculateDistanceToGeoJson(userLocation, oldStreetLineLayer);

        // Escolher a rota mais próxima
        if (vrDistance < oldStreetDistance) {
            // Rota Via Rápida é mais próxima
            drawRoute(vrLineLayer);
        } else {
            // Rota Caminho Velho é mais próxima
            drawRoute(oldStreetLineLayer);
        }
    }

    // Método para calcular a distância até uma linha GeoJSON
    private double calculateDistanceToGeoJson(LatLng userLocation, GeoJsonLayer layer) {
        double minDistance = Double.MAX_VALUE;

        // Para cada coordenada da GeoJsonLayer, calculamos a distância até o ponto do usuário
        for (GeoJsonFeature feature : layer.getFeatures()) {
            if (feature.getGeometry() instanceof GeoJsonLineString) {
                GeoJsonLineString lineString = (GeoJsonLineString) feature.getGeometry(); // Obtém a geometria da feature
                for (LatLng latLng : lineString.getCoordinates()) {
                    double distance = SphericalUtil.computeDistanceBetween(userLocation, latLng);
                    minDistance = Math.min(minDistance, distance);
                }
            }
        }

        return minDistance;
    }

    // Método para desenhar a rota escolhida no mapa
    private void drawRoute(GeoJsonLayer selectedRouteLayer) {
        // Limpar qualquer rota desenhada anteriormente
        mMap.clear();

        // Adicionar a camada da rota escolhida
        selectedRouteLayer.addLayerToMap();

        // Ajustar a câmera para a rota escolhida
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (GeoJsonFeature feature : selectedRouteLayer.getFeatures()) {
            if (feature.getGeometry() instanceof GeoJsonLineString) {
                GeoJsonLineString lineString = (GeoJsonLineString) feature.getGeometry(); // Obtém a geometria da feature
                for (LatLng latLng : lineString.getCoordinates()) {
                    builder.include(latLng);
                }
            }
        }
        LatLngBounds bounds = builder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

}
