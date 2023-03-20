package com.example.androidLIS.TerabeeSensor;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.example.androidLIS.MainActivity;
import com.example.androidLIS.util.AppUtil;
import com.terabee.sdk.TerabeeSdk;

import org.json.JSONObject;

public class TerabeeInstance {
    Context mContext = null;
    private TerabeeSdk.DeviceType mCurrentType = TerabeeSdk.DeviceType.AUTO_DETECT;
    MainActivity.TerabeeHandler mHandler;

    public TerabeeInstance(Context context, MainActivity.TerabeeHandler handler){
        mContext = context;
        mHandler = handler;
    }


    public void initTerabeeSensor(){
        /**
         * 거리센서 SDK 초기화
         */
        TerabeeSdk.getInstance().init(mContext.getApplicationContext());
        TerabeeSdk.getInstance().registerDataReceive(mDataDistanceCallback);
        connectToDevice();
    }



    /**
     * 거리센서 콜백함수
     */
    private final TerabeeSdk.DataDistanceCallback mDataDistanceCallback = new
            TerabeeSdk.DataDistanceCallback() {
                @Override
                public void onDistanceReceived(int distance, int dataBandwidth, int
                        dataSpeed) {
                    Message message = mHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("type",1);
                    bundle.putInt("floor",distance);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                    // received distance from the sensor
                }

                @Override
                public void onReceivedData(byte[] bytes, int i, int i1) {
                    // received raw data from the sensor
//                    Log.d("TerabeeLog", AppUtil.getInstance().byteArrayToHex(bytes));
                }
            };


    private void connectToDevice() {
        Thread connectThread = new Thread(() -> {
            try {
                TerabeeSdk.getInstance().connect(new TerabeeSdk.IUsbConnect() {
                    @Override
                    public void connected(boolean success, TerabeeSdk.DeviceType
                            deviceType) {
                        Message message = mHandler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putInt("type",2);
                        if(!success) {
                            bundle.putInt("connect", 0);
                        }else{
                            bundle.putInt("connect", 1);
                        }
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void disconnected() {
                        Message message = mHandler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putInt("type",2);
                        bundle.putInt("connect", 2);
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void permission(boolean granted) {

                    }
//                }, TerabeeSdk.DeviceType.EVO_60M);
                }, mCurrentType);
            } catch (Exception e) {
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("type",2);
                bundle.putInt("connect", 3);
                message.setData(bundle);
                mHandler.sendMessage(message);
                Log.e("Terabee", e.getMessage());
            }
        });

        connectThread.start();
    }



    public void closeInstance(){
        TerabeeSdk.getInstance().unregisterDataReceive(mDataDistanceCallback);
        disconnectDevice();
        TerabeeSdk.getInstance().dispose();
    }

    private void disconnectDevice() {
        try {
            TerabeeSdk.getInstance().disconnect();
        } catch (Exception e) {
            Log.e("Terabee", e.getMessage());
        }
    }


}
