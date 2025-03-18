package com.example.mbus;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mbus.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Receiver extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference locationRef;
    private Marker marker;  // Marcador para atualizar a posição no mapa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        // Configurar o fragmento do Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Referência ao nó do Firebase onde a app secundária envia a localização
        locationRef = FirebaseDatabase.getInstance().getReference("locations/secondaryApp");

        // Escutar mudanças na localização
        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);

                    if (latitude != null && longitude != null) {
                        atualizarMapa(latitude, longitude);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Receiver.this, "Erro ao carregar localização", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private void atualizarMapa(double latitude, double longitude) {
        LatLng novaLocalizacao = new LatLng(latitude, longitude);

        if (mMap != null) {
            if (marker != null) {
                marker.remove(); // Remove o marcador antigo
            }

            // Adiciona um novo marcador na posição recebida
            marker = mMap.addMarker(new MarkerOptions().position(novaLocalizacao).title("Localização Atual"));

            // Movimenta a câmera para a nova posição
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(novaLocalizacao, 15));
        }
    }
}
