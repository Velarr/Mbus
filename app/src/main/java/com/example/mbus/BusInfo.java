package com.example.mbus;

public class BusInfo {
    public String id;
    public String companhia;
    public int nrota;
    public String rotaNome;
    public String geojson;
    public String corHex;

    public BusInfo(String id, String companhia, int nrota, String rotaNome, String geojson, String corHex) {
        this.id = id;
        this.companhia = companhia;
        this.nrota = nrota;
        this.rotaNome = rotaNome;
        this.geojson = geojson;
        this.corHex = corHex;
    }

    public BusInfo(String geojson, String corHex, String rotaNome, int nrota) {
        this.geojson = geojson;
        this.corHex = corHex;
        this.rotaNome = rotaNome;
        this.nrota = nrota;
    }

}



