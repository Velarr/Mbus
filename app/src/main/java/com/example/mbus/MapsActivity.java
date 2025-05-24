package com.example.mbus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.InputStream;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.mbus.databinding.ActivityMapsBinding;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.json.JSONException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        LinearLayout btnOptions = findViewById(R.id.btn_bus);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MapOptionsMenu menu = new MapOptionsMenu(this, btnOptions, geoJsonResId -> loadGeoJsonLayer(geoJsonResId));

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

        Receiver receiver = new Receiver(mMap, this);
        receiver.startListening();

        configureMapBounds();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadGeoJsonLayer(int geoJsonResId) {
        mMap.clear();
        configureMapBounds();

        try {
            GeoJsonLayer geoJsonLayer = new GeoJsonLayer(mMap, geoJsonResId, getApplicationContext());
            geoJsonLayer.addLayerToMap();
        } catch (Exception e) {
            Log.e("GeoJSON", "Error loading GeoJSON file: " + e.getMessage());
        }
    }



    private void configureMapBounds() {
        LatLngBounds madeiraBounds = new LatLngBounds(
                new LatLng(32.50, -17.30), // SW
                new LatLng(33.10, -16.30)  // NE
        );

        mMap.setLatLngBoundsForCameraTarget(madeiraBounds);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(madeiraBounds, 0));
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(18.0f);
    }
}
