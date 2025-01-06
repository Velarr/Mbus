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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private LineStreetsActivity lineStreetsActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        lineStreetsActivity = new LineStreetsActivity();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Define os limites do arquipélago da Madeira
        LatLngBounds madeiraBounds = new LatLngBounds(
                new LatLng(32.50, -17.30), // SW bounds
                new LatLng(33.10, -16.30)  // NE bounds
        );

        // Define os limites do mapa
        mMap.setLatLngBoundsForCameraTarget(madeiraBounds);

        // Move a câmera para o centro dos limites
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(madeiraBounds, 0));

        // Define o nível de zoom mínimo e máximo
        mMap.setMinZoomPreference(6.0f); // Permite mais zoom out
        mMap.setMaxZoomPreference(18.0f); // Permite mais zoom in

        // Opcional: Adiciona um marcador no centro da Ilha da Madeira
        LatLng madeiraCenter = new LatLng(32.7607, -16.9599);
        mMap.addMarker(new MarkerOptions().position(madeiraCenter).title("Ilha da Madeira"));

        // Adiciona os caminhos usando a LineStreetsActivity
        lineStreetsActivity.addPaths(mMap);
    }
}