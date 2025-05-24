package com.example.mbus;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
// No import for com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlPlacemark; // Keep other imports
import com.google.maps.android.data.kml.KmlLineString;
import org.xmlpull.v1.XmlPullParserException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KmlLayer { // Your class

    // Método para adicionar camada KML ao mapa com cor customizada para linhas
    public static void addKmlLayer(Context context, GoogleMap map, int kmlResId, int color) {
        try {
            InputStream inputStream = context.getResources().openRawResource(kmlResId);
            // Use the fully qualified name for the library's KmlLayer
            com.google.maps.android.data.kml.KmlLayer layer = new com.google.maps.android.data.kml.KmlLayer(map, inputStream, context);

            // Percorrer os placemarks para alterar estilo das linhas
            for (KmlPlacemark placemark : layer.getPlacemarks()) {
                if (placemark.getGeometry() instanceof KmlLineString) {
                    // Alterar a cor do estilo da linha (se possível)
                    // Infelizmente a API não oferece método direto para mudar cor de KmlLayer após carregamento.
                    // Uma solução seria alterar o arquivo KML para definir a cor desejada.
                }
            }

            layer.addLayerToMap();
        } catch (XmlPullParserException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

    // Método para carregar camada KML sem adicionar ao mapa
    public static com.google.maps.android.data.kml.KmlLayer loadKmlLayer(Context context, int kmlResId) { // Return type is the library's KmlLayer
        try {
            InputStream inputStream = context.getResources().openRawResource(kmlResId);
            // Use the fully qualified name
            return new com.google.maps.android.data.kml.KmlLayer(null, inputStream, context);
        } catch (XmlPullParserException | java.io.IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Método para obter coordenadas das linhas do KML
    // Parameter type is the library's KmlLayer
    public static List<LatLng> getCoordinatesFromKml(com.google.maps.android.data.kml.KmlLayer layer) {
        List<LatLng> coordinates = new ArrayList<>();

        try {
            // Percorre todos os placemarks
            for (KmlPlacemark placemark : layer.getPlacemarks()) {
                if (placemark.getGeometry() instanceof KmlLineString) {
                    KmlLineString lineString = (KmlLineString) placemark.getGeometry();
                    coordinates.addAll(lineString.getGeometryObject());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return coordinates;
    }
}