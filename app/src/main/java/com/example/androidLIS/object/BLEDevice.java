package com.example.androidLIS.object;

import java.io.Serializable;

/**
 * Created by Bruce_Chiang on 2017/3/13.
 */

public class BLEDevice implements Serializable {
    private int imageId;
    private String  name;
    private String  rssi;
    private String  address;

    public BLEDevice(String name, String rssi, String address)
    {
        this.name = name;
        this.rssi = rssi;
        this.address = address;
    }

    public void setImage(int id) { this.imageId = id; }

    public int getImage() { return this.imageId; }

    public String getName() {
        return this.name;
    }

    public String getRSSI() {
        return this.rssi;
    }

    public String getAddress() {
        return this.address;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BLEDevice))
            return false;
        if (obj == this)
            return true;
        return this.address == ((BLEDevice) obj).address;
    }
}
