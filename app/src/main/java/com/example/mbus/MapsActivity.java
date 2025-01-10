package com.example.mbus;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.mbus.databinding.ActivityMapsBinding;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import java.io.InputStream;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Define os limites do arquipélago da Madeira
        LatLngBounds madeiraBounds = new LatLngBounds(
                new LatLng(32.50, -17.30), // SW bounds
                new LatLng(33.10, -16.30)  // NE bounds
        );

        mMap.setLatLngBoundsForCameraTarget(madeiraBounds);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(madeiraBounds, 0));
        mMap.setMinZoomPreference(8.0f);
        mMap.setMaxZoomPreference(18.0f);

        LatLng madeiraCenter = new LatLng(32.7607, -16.9599);
        mMap.addMarker(new MarkerOptions().position(madeiraCenter).title("Ilha da Madeira"));

        // Adiciona a linha do arquivo GeoJSON
        addGeoJsonLayer();
    }

    private void addGeoJsonLayer() {
        try {
            // Lê o arquivo GeoJSON da pasta raw
            InputStream inputStream = getResources().openRawResource(R.raw.linhabus);
            GeoJsonLayer layer = new GeoJsonLayer(mMap, inputStream, getApplicationContext());

            // Adiciona a camada ao mapa
            layer.addLayerToMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}