package com.example.androidLIS.object;

import java.io.Serializable;

/**
 * Created by Bruce_Chiang on 2017/3/27.
 */

public class Common implements Serializable {

    private boolean title;//false = [TX]; true = [RX]
    private String data;
    private String time;

    public Common(boolean b, String data, String time) {
        this.title = b;
        this.data = data;
        this.time = time;
    }

    public void Title(boolean b) { this.title = b; }
    public void Data(String s) { this.data = s; }
    public void Time(String s) { this.time = s; }

    public boolean Title() { return this.title; }
    public String Data() { return this.data; }
    public String Time() { return this.time; }
}
