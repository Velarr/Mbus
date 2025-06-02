package com.example.mbus;

import com.google.android.gms.maps.model.LatLng;

public class MapUtils {

    public static LatLng offsetLatLng(LatLng original, int index) {
        if (index == 0) return original;

        double offset = 0.000020;
        double angle = index * 45;
        double radians = Math.toRadians(angle);

        double latOffset = offset * Math.cos(radians);
        double lngOffset = offset * Math.sin(radians);

        return new LatLng(original.latitude + latOffset, original.longitude + lngOffset);
    }
}
