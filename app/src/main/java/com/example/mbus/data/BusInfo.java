package com.example.mbus.data;

public class BusInfo {
    private String id;
    private String companyId;
    private String companyName;
    private int routeNumber;
    private String routeName;
    private String geojson;
    private String color;

    public BusInfo(String id, String companyId, int routeNumber, String routeName, String geojson, String color) {
        this.id = id;
        this.companyId = companyId;
        this.routeNumber = routeNumber;
        this.routeName = routeName;
        this.geojson = geojson;
        this.color = color;
    }

    public BusInfo(String geojson, String color, String routeName, int routeNumber) {
        this.geojson = geojson;
        this.color = color;
        this.routeName = routeName;
        this.routeNumber = routeNumber;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public int getRouteNumber() {
        return routeNumber;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getGeojson() {
        return geojson;
    }

    public String getColor() {
        return color;
    }

    // Setter para companyName (vai ser usado após buscar da Firestore)
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
