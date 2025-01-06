package com.example.mbus;

import android.graphics.Color;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.List;

public class LineStreetsActivity {

    public void addPaths(GoogleMap googleMap) {
        // Define as coordenadas dos pontos do caminho
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(32.6467, -16.9127)); // Anadia

        points.add(new LatLng(32.6570, -16.9750)); // Estreito

        // Cria um objeto PolylineOptions para configurar o estilo do caminho
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(8)
                .addAll(points);

        // Adiciona o caminho ao mapa
        googleMap.addPolyline(polylineOptions);
    }
}