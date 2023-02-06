package com.example.androidLIS.model;

public class LocationInfo {
    public String workLocationId;
    public String vehicleId;
    public String epc;

    public  LocationInfo(String workLocationId, String vehicleId, String epc){
        this.workLocationId = workLocationId;
        this.vehicleId = vehicleId;
        this.epc = epc;
    }

}
