package com.example.mbus;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GeoJsonUtils {

    // Adiciona a camada GeoJSON ao mapa
    public static void addGeoJsonLayer(Context context, GoogleMap map, int geoJsonResId) {
        try {
            InputStream inputStream = context.getResources().openRawResource(geoJsonResId);
            String geoJsonString = new Scanner(inputStream).useDelimiter("\\A").next();
            JSONObject geoJsonObject = new JSONObject(geoJsonString);

            GeoJsonLayer layer = new GeoJsonLayer(map, geoJsonObject);
            layer.addLayerToMap();
        } catch (Exception e) {
            Log.e("GeoJsonUtils", "Failed to load GeoJSON layer: " + e.getMessage());
        }
    }

    // Carrega uma camada GeoJSON e retorna sem adicionar ao mapa
    public static GeoJsonLayer loadGeoJsonLayer(Context context, int geoJsonResId) {
        try {
            InputStream inputStream = context.getResources().openRawResource(geoJsonResId);
            String geoJsonString = new Scanner(inputStream).useDelimiter("\\A").next();
            JSONObject geoJsonObject = new JSONObject(geoJsonString);

            return new GeoJsonLayer(null, geoJsonObject); // Not yet added to map
        } catch (Exception e) {
            Log.e("GeoJsonUtils", "Failed to load GeoJSON: " + e.getMessage());
        }
        return null;
    }

    // Extrai todas as coordenadas das linhas (GeoJsonLineString)
    public static List<LatLng> getCoordinatesFromGeoJson(GeoJsonLayer layer) {
        List<LatLng> coordinates = new ArrayList<>();

        try {
            for (GeoJsonFeature feature : layer.getFeatures()) {
                if (feature.getGeometry() instanceof GeoJsonLineString) {
                    GeoJsonLineString lineString = (GeoJsonLineString) feature.getGeometry();
                    coordinates.addAll(lineString.getCoordinates());
                }
            }
        } catch (Exception e) {
            Log.e("GeoJsonUtils", "Failed to get coordinates: " + e.getMessage());
        }

        return coordinates;
    }
}
