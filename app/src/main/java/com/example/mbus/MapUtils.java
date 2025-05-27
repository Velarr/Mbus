package com.example.mbus;

import com.google.android.gms.maps.model.LatLng;

public class MapUtils {

    /**
     * Aplica um pequeno offset para evitar sobreposição de marcadores na mesma posição.
     */
    public static LatLng offsetLatLng(LatLng original, int index) {
        if (index == 0) return original;

        double offset = 0.000010;
        double angle = index * 45;
        double radians = Math.toRadians(angle);

        double latOffset = offset * Math.cos(radians);
        double lngOffset = offset * Math.sin(radians);

        return new LatLng(original.latitude + latOffset, original.longitude + lngOffset);
    }
}
