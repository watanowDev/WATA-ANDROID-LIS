package com.example.androidLIS.model;

public class RfidScanData {
    public String epc;
    public double rssi;
    public String antenna;
    public long time;
    public RfidScanData(String epc, double rssi, String antenna, long time){
        this.epc = epc;
        this.rssi = rssi;
        this.antenna = antenna;
        this.time = time;
    }
}
