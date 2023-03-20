package com.example.androidLIS.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.example.androidLIS.model.Alive;
import com.example.androidLIS.model.AliveReqData;
import com.example.androidLIS.util.AppConfig;

public class AccelerometerSensor implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float[] currentValues = new float[3];
    public double acc = 0.0;


    private Handler mHandler;
    public Thread mSendThread;
    public boolean mSend = false;

    public AccelerometerSensor(Context context, Handler handler) {
        mHandler = handler;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
        sendAccSensorData();
    }

    public void start() {
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        mSend = false;
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            currentValues[0] = event.values[0];
            currentValues[1] = event.values[1];
            currentValues[2] = event.values[2];
            acc += Math.sqrt(currentValues[0] * currentValues[0] + currentValues[1] * currentValues[1] + currentValues[2] * currentValues[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public void sendAccSensorData(){
        mSend = true;
        if (mSendThread != null){
            mSendThread.interrupt();
            mSendThread = null;
        }
        mSendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mSend) {
                    Message message = mHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("acc", acc);
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                    acc = 0;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }

            }

        });
        mSendThread.start();
    }

}