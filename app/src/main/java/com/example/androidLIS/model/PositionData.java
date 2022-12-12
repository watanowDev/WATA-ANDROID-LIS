package com.example.androidLIS.model;

public class PositionData {
    public int floor;
    public String address;
    public long time;
    public PositionData(int floor, String address, long time){
        this.floor = floor;
        this.address = address;
        this.time = time;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
