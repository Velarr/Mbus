package com.example.mbus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Receiver extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker marker;  // Marcador para atualizar a posição no mapa

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // Código de solicitação de permissão

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Configurar o fragmento do Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Verificar e pedir permissões de localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d("Receiver", "Permissão já concedida");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("Receiver", "Mapa pronto");

        // Verifica se as permissões de localização foram concedidas
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
            Log.e("Receiver", "Permissão de localização não concedida");
            return;
        }

        // Coordenadas fixas para teste
        double latitude = 32.6488215;
        double longitude = -16.91331267356873;

        LatLng localizacao = new LatLng(latitude, longitude);
        Log.d("Receiver", "Coordenadas: " + latitude + ", " + longitude);

        // Adiciona o marcador
        marker = mMap.addMarker(new MarkerOptions().position(localizacao).title("Localização de Teste"));
        if (marker != null) {
            Log.d("Receiver", "Marcador adicionado com sucesso");
        } else {
            Log.e("Receiver", "Falha ao adicionar marcador");
        }

        // Movimenta a câmera para a localização
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localizacao, 15));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, habilita a localização no mapa
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    Log.d("Receiver", "Permissão concedida, localização habilitada");
                }
            } else {
                Toast.makeText(this, "Permissão de localização negada", Toast.LENGTH_SHORT).show();
                Log.e("Receiver", "Permissão de localização negada");
            }
        }
    }
}
