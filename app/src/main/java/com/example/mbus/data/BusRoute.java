package com.example.mbus.data;

import com.google.android.gms.maps.model.LatLng;

import static java.lang.Math.*;

import java.util.*;

public class BusRoute {
    private String id;
    private List<List<LatLng>> multiLinePoints;
    private double minDistanceToTarget;

    private String name;     // Route name
    private int number;      // Route number
    private String color;    // Route color (hex)

    public BusRoute(String id, List<List<LatLng>> multiLinePoints) {
        this.id = id;
        this.multiLinePoints = multiLinePoints;
    }

    public BusRoute(String id, List<List<LatLng>> multiLinePoints, String name, int number, String color) {
        this.id = id;
        this.multiLinePoints = multiLinePoints;
        this.name = name;
        this.number = number;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public List<List<LatLng>> getSegments() {
        return multiLinePoints;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public String getColor() {
        return color;
    }

    public void setDistanceToTarget(double d) {
        minDistanceToTarget = d;
    }

    public double getDistanceToTarget() {
        return minDistanceToTarget;
    }

    public double minDistanceTo(double lat, double lng) {
        double r = 6371000; // Earth radius in meters
        double min = Double.MAX_VALUE;

        for (List<LatLng> line : multiLinePoints) {
            for (LatLng p : line) {
                double dLat = toRadians(p.latitude - lat);
                double dLon = toRadians(p.longitude - lng);
                double a = sin(dLat / 2) * sin(dLat / 2)
                        + cos(toRadians(lat)) * cos(toRadians(p.latitude))
                        * sin(dLon / 2) * sin(dLon / 2);
                double c = 2 * atan2(sqrt(a), sqrt(1 - a));
                double dist = r * c;
                if (dist < min) min = dist;
            }
        }

        return min;
    }
}
