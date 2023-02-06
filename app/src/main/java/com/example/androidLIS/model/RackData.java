package com.example.androidLIS.model;

public class RackData {
    public int floor;
    public String rack;
    public long time;
    public RackData(int floor, String address, long time){
        this.floor = floor;
        this.rack = address;
        this.time = time;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
