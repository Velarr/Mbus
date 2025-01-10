package com.example.mbus;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonUtils {

    // Método para adicionar camada GeoJSON ao mapa
    public static void addGeoJsonLayer(Context context, GoogleMap map, int geoJsonResId, int color) {
        try {
            InputStream inputStream = context.getResources().openRawResource(geoJsonResId);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();

            JSONObject geoJsonData = new JSONObject(stringBuilder.toString());
            GeoJsonLayer layer = new GeoJsonLayer(map, geoJsonData);

            // Adiciona o estilo para a linha
            layer.getDefaultLineStringStyle().setColor(color);
            layer.addLayerToMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para carregar GeoJsonLayer a partir de um arquivo Raw
    public static GeoJsonLayer loadGeoJsonLayer(Context context, int geoJsonResId) {
        try {
            InputStream inputStream = context.getResources().openRawResource(geoJsonResId);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();

            JSONObject geoJsonData = new JSONObject(stringBuilder.toString());
            return new GeoJsonLayer(null, geoJsonData); // Retorna a camada sem o mapa (para usar fora do mapa)
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Método para obter as coordenadas de uma linha GeoJSON
    public static List<LatLng> getCoordinatesFromGeoJson(GeoJsonLayer layer) {
        List<LatLng> coordinates = new ArrayList<>();

        // Para cada feature, verificamos se ela é uma GeoJsonLineString
        for (GeoJsonFeature feature : layer.getFeatures()) {
            if (feature.getGeometry() instanceof GeoJsonLineString) {
                GeoJsonLineString lineString = (GeoJsonLineString) feature.getGeometry();
                coordinates.addAll(lineString.getCoordinates());
            }
        }

        return coordinates;
    }
}
