package com.example.androidLIS.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Message;

import android.os.Handler;
import android.util.Log;

public class StatusMonitor extends BroadcastReceiver {
    private OnBatteryAndNetworkChangeListener listener;
    private boolean[] SystemStatusList;
    private boolean StatusCameraQR = false;
    private boolean StatusCameraTOF = false;
    private boolean StatusSensorDistance = false;
    private boolean StatusRFIDScanner = false;
    private boolean StatusNetwork = false;
    private boolean StatusBattery = false;
    private boolean StatusSensorAccelerometer = true;
    private boolean SystemStatus = true;

    private long LastAccChangedTime = 0;

    private Handler mHandler;
    private Context mContext;
    public StatusMonitor(Context context, Handler handler){
        mHandler = handler;
        mContext = context;
        SystemStatusList = new boolean[5];
        LastAccChangedTime = System.currentTimeMillis();
    }

    public void start() {
        // Register battery and network change receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(this, filter);
    }

    public void stop() {
        // Unregister battery and network change receiver
        try {
            mContext.unregisterReceiver(this);
        } catch (IllegalArgumentException e){
                e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {

        }
    }

    public void setOnBatteryAndNetworkChangeListener(OnBatteryAndNetworkChangeListener listener) {
        this.listener = listener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int batteryPercentage = (int) (((float) level / (float) scale) * 100);

            if (listener != null) {
                listener.onBatteryPercentageChanged(batteryPercentage);
            }
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null;
            if (connectivityManager != null) {
                networkInfo = connectivityManager.getActiveNetworkInfo();
            }

            if (networkInfo != null && networkInfo.isConnected()) {
                int networkType = networkInfo.getType();
                String networkTypeName = "";
                switch (networkType) {
                    case ConnectivityManager.TYPE_MOBILE:
                        networkTypeName = "Mobile";
                        break;
                    case ConnectivityManager.TYPE_WIFI:
                        networkTypeName = "Wifi";
                        break;
                }

                if (listener != null) {
                    listener.onNetworkStateChanged(true, networkTypeName);
                }
            } else {
                if (listener != null) {
                    listener.onNetworkStateChanged(false, "");
                }
            }
        }
    }

    public void setStatusCameraQR(boolean status) {
        Log.d("status!!", "qr");
        SystemStatusList[0] = status;
        StatusCameraQR = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 0);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 0);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();

    }

    public void setStatusCameraTOF(boolean status) {
        Log.d("status!!", "tof");

        SystemStatusList[1] = status;
        StatusCameraTOF = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 1);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 1);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();
    }

    public void setStatusSensorDistance(boolean status) {
        Log.d("status!!", "dis");

        SystemStatusList[2] = status;
        StatusSensorDistance = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 2);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 2);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();

    }
    public void setStatusRFIDScanner(boolean status) {
        Log.d("status!!", "rf");

        SystemStatusList[3] = status;
        StatusRFIDScanner = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 3);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 3);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();

    }
    public void setStatusNetwork(boolean status) {
        Log.d("status!!", "net");

        SystemStatusList[4] = status;
        StatusNetwork = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 4);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 4);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();

    }

    public void setStatusBattery(boolean status, int value) {
        Log.d("status!!", "ba");
        StatusBattery = status;
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(status){
            bundle.putBoolean("status", true);
            bundle.putInt("type", 5);
            bundle.putInt("value", value);
        }else{
            bundle.putBoolean("status", false);
            bundle.putInt("type", 5);
            bundle.putInt("value", value);
        }
        message.setData(bundle);
        mHandler.sendMessage(message);
        isAllReady();
    }


    public void setStatusSensorAcceleroMeter(double acc) {
        Log.e("accccc",acc+"");
        if(acc > AppConfig.ACC_ACTION_THRESHOLD){
            LastAccChangedTime = System.currentTimeMillis();
            if(!StatusSensorAccelerometer) {
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putBoolean("status", true);
                bundle.putInt("type", 6);
                message.setData(bundle);
                mHandler.sendMessage(message);
            }
            StatusSensorAccelerometer = true;
        }else{
            if(System.currentTimeMillis() - LastAccChangedTime > AppConfig.ACC_ACTION_TIMEOUT){
                if(StatusSensorAccelerometer) {
                    Message message = mHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("status", false);
                    bundle.putInt("type", 6);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                }
                StatusSensorAccelerometer = false;

            }
        }
    }

    public void isAllReady(){
        boolean ready = true;
        for (int i = 0; i < SystemStatusList.length; i++) {
            if (!SystemStatusList[i]) {
                ready = false;
                SystemStatus = false;
            }
        }
        if (!SystemStatus) {
            if (ready) {
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putBoolean("status", true);
                bundle.putInt("type", 7);
                message.setData(bundle);
                mHandler.sendMessage(message);
                SystemStatus = true;
            }
        }
    }

    public void checkAllStatus(){
        for (int i = 0; i < SystemStatusList.length; i++) {
            if (!SystemStatusList[i]) {
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putBoolean("status", false);
                bundle.putInt("type", i);
                message.setData(bundle);
                mHandler.sendMessage(message);
            }
        }
    }

    public interface OnBatteryAndNetworkChangeListener {
        void onBatteryPercentageChanged(int percentage);

        void onNetworkStateChanged(boolean isConnected, String networkTypeName);
    }

    public boolean isStatusCameraQR() {
        return StatusCameraQR;
    }

    public boolean isStatusCameraTOF() {
        return StatusCameraTOF;
    }

    public boolean isStatusSensorDistance() {
        return StatusSensorDistance;
    }

    public boolean isStatusRFIDScanner() {
        return StatusRFIDScanner;
    }

    public boolean isStatusNetwork() {
        return StatusNetwork;
    }

    public boolean isStatusBattery() {
        return StatusBattery;
    }

    public boolean isStatusSensorAccelerometer() {
        return StatusSensorAccelerometer;
    }

    public boolean isSystemStatus() {
        return SystemStatus;
    }
}
