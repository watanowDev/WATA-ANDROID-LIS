package com.example.androidLIS.ApulseRFID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apulsetech.lib.event.DeviceEvent;
import com.apulsetech.lib.event.ReaderEventListener;
import com.apulsetech.lib.remote.service.BtSppRemoteService;
import com.apulsetech.lib.remote.type.ConfigValues;
import com.apulsetech.lib.remote.type.Msg;
import com.apulsetech.lib.remote.type.RemoteDevice;
import com.apulsetech.lib.remote.type.Setting;
import com.apulsetech.lib.rfid.Reader;
import com.apulsetech.lib.rfid.type.RfidResult;
import com.example.androidLIS.MainActivity;
import com.example.androidLIS.R;
import com.example.androidLIS.model.RfidScanData;
import com.example.androidLIS.tof.DepthFrameVisualizer;
import com.example.androidLIS.util.AppConfig;
import com.example.androidLIS.util.WorkObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ApulseRFIDInstance implements ReaderEventListener {
    //Apulse RFID Scanner
    private boolean mInitialized = false;
    private Reader mReader;
    private boolean mInventoryStarted = false;
    private boolean mContinuousModeEnabled = true;
    private boolean mIgnorePC = false;
    private BtSppRemoteService mBtSppRemoteService;
    private boolean mBtSppRemoteServiceBound = false;

    private static final int DEFAULT_SCAN_PERIOD = 120000;
    private int mScanPeriod = DEFAULT_SCAN_PERIOD;
    public RemoteDevice scanDevice = null;

    public Setting mSetting;
    public ArrayList<String> mPreviouslyConnectedDevices;
    public MainActivity.RfidHandler mHandler = null;
    public Activity mActivity = null;

    private Thread thread;


    public ArrayList<RfidScanData> scanData = new ArrayList<RfidScanData>();
    public ApulseRFIDInstance(Activity activity,MainActivity.RfidHandler handler){
        mActivity = activity;

        mSetting = new Setting(mActivity);
        mPreviouslyConnectedDevices = mSetting.getPreviouslyConnectedDevices();
        mHandler = handler;

    }


    public void bindBtSppRemoteService() {
        Log.d("bindBtSppRemoteService", "bindBtSppRemoteService()");
        mBtSppRemoteServiceBound = true;

        if (mBtSppRemoteService != null) {
            Log.d("bindBtSppRemoteService", "BT SPP service already bound.");
        }

        mActivity.bindService(new Intent(mActivity, BtSppRemoteService.class),
                mBtSppRemoteServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindBtSppRemoteService() {
        Log.d("unbindBtSppRemoteService", "unbindBtSppRemoteService()");
        mBtSppRemoteServiceBound = false;

        if (mBtSppRemoteService != null) {
            mActivity.unbindService(mBtSppRemoteServiceConnection);
        }
    }

    public void closeInstance(){
        if(mInventoryStarted){
            mInventoryStarted = false;
            mReader.stopOperation();
            mReader.stop();
            mReader.destroy();
        }

        if (mBtSppRemoteServiceBound) {
            unbindBtSppRemoteService();
        }

    }

    public void stopScan(){
        if (mReader != null) {
            toggleInventory();
        }
    }

    private final ServiceConnection mBtSppRemoteServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.d("mBtSppRemoteServiceConnection", "onServiceConnected() BT SPP service.");
            mBtSppRemoteService = ((BtSppRemoteService.LocalBinder)binder).getService(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("mBtSppRemoteServiceConnection", "onServiceDisconnected() BT SPP service.");
            mBtSppRemoteService = null;
        }
    };
    /**
     * BLE 연결 관련
     */

    public void startScan(){
        for(int i = 0 ; i < mPreviouslyConnectedDevices.size() ; i++){
            RemoteDevice preDevice = RemoteDevice.fromGson(mPreviouslyConnectedDevices.get(i));
            if(preDevice.getAddress().equals(AppConfig.RFID_MAC)){
                initialize(preDevice,40000);
                return;
            }
        }

        if (mBtSppRemoteService != null) {
            if (mBtSppRemoteService.isScanning()) {
                mBtSppRemoteService.stopRemoteDeviceScan();
            } else {
                mBtSppRemoteService.startRemoteDeviceScan(mScanPeriod);
            }
        } else {
            Log.d("connBLE","BT SPP Remote service is not binded!");
        }

    }





    public void initialize(RemoteDevice device, int timeout) {
        WorkObserver.waitFor(mActivity,
                mActivity.getString(R.string.common_alert_connecting),
                new WorkObserver.ObservableWork() {
                    @Override
                    public Object run() {
                        initializeRfid(device, timeout);
                        return null;
                    }

                    @Override
                    public void onWorkDone(Object result) {
                        mInitialized = true;
                    }
                });
    }


    public void initializeRfid(RemoteDevice device, int timeout) {
        Log.d("initRfid", "start");
        mReader = Reader.getReader(mActivity , device, false, timeout);
        if (mReader != null) {
            mReader.setEventListener(this);
            if (mReader.start()) {
                mReader.setToggle(0);
                mReader.setInventoryAntennaPortReportState(1);
                mReader.setRadioPower(27);
                mReader.setTxOnTime(100);
                mReader.setTxOnOverheadTime(300);
                Log.d("initRfid", "reader open success!");
                toggleInventory();
            } else {
                Log.d("initRfid", "reader open failed!");
                mReader.destroy();
                mReader = null;
                Toast.makeText(mActivity,
                        R.string.rfid_main_message_unable_to_connect_module,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d("initRfid", "reader instance is null!");

            Toast.makeText(mActivity,
                    R.string.rfid_main_message_unable_to_connect_module,
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onReaderDeviceStateChanged(DeviceEvent state) {
        Log.d("onReaderDeviceStateChanged", "DeviceEvent : " + state);

        if (state == DeviceEvent.DISCONNECTED) {
            mReader = null;
        }
    }


    @Override
    public void onReaderEvent(int event, int result, @Nullable String data) {
        Log.d("onReaderEvent", "onReaderEvent(): event=" + event
                + ", result=" + result
                + ", data=" + data);

        switch (event) {
            case Reader.READER_CALLBACK_EVENT_INVENTORY:
                if (result == RfidResult.SUCCESS) {
                    if ((data != null) && mInventoryStarted) {
                        processTagData(data);
                    }
                }
                break;

            case Reader.READER_CALLBACK_EVENT_START_INVENTORY:
                if (!mInventoryStarted) {
                    mInventoryStarted = true;
                }
                break;

            case Reader.READER_CALLBACK_EVENT_STOP_INVENTORY:
                if (mInventoryStarted && !mReader.isOperationRunning()) {
                    mInventoryStarted = false;
                }
                break;
        }
    }

    @Override
    public void onReaderRemoteKeyEvent(int action, int keyCode) {
        Log.d("onReaderRemoteKeyEvent","onReaderRemoteKeyEvent : action=" + action + " keyCode=" + keyCode);
    }

    @Override
    public void onReaderRemoteSettingChanged(int i, Object o) {

    }


    private void processTagData(String data) {
        String epc = null;
        String rssi = null;
        String phase = null;
        String fastID = null;
        String channel = null;
        String antenna = null;

        String[] dataItems = data.split(";");
        for (String dataItem : dataItems) {
            if (dataItem.contains("rssi")) {
                int point = dataItem.indexOf(':') + 1;
                rssi = dataItem.substring(point);
            } else if (dataItem.contains("phase")) {
                int point = dataItem.indexOf(':') + 1;
                phase = dataItem.substring(point);
            } else if (dataItem.contains("fastID")) {
                int point = dataItem.indexOf(':') + 1;
                fastID = dataItem.substring(point);
            } else if (dataItem.contains("channel")) {
                int point = dataItem.indexOf(':') + 1;
                channel = dataItem.substring(point);
            }else if (dataItem.contains("antenna")) {
                int point = dataItem.indexOf(':') + 1;
                antenna = dataItem.substring(point);
            } else {
                epc = dataItem;
            }
        }
        if(antenna.equals("1")){

        }else if(antenna.equals("2")){

        }

        scanData.add(new RfidScanData(epc,rssi,antenna,System.currentTimeMillis()));


    }


    public void updateScanData(){
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mInventoryStarted) {
                    for (int i = 0; i < scanData.size(); i++) {
                        if (scanData.size() != 0 && System.currentTimeMillis() - scanData.get(i).time > 1000) {
                            scanData.remove(i);
                            i--;
                        }
                    }
                    Message message = Message.obtain(mHandler, AppConfig.RFID_SCAN_RESULT);
                    mHandler.sendMessage(message);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public ArrayList<RfidScanData> getScanData(){
        ArrayList<RfidScanData> result = new ArrayList<RfidScanData>(scanData);
        return result;
    }

    private void toggleInventory() {
//        mInventoryButton.setEnabled(false);

        int result;
        if (mInventoryStarted) {
            result = mReader.stopOperation();
            if (result == RfidResult.SUCCESS) {
                mInventoryStarted = false;
            } else {
                Toast.makeText(mActivity,
                        mActivity.getString(R.string.rfid_alert_stop_inventory_failed)
                                + " (" + result + ")",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            result = mReader.startInventory(
                    mContinuousModeEnabled,
                    false,
                    mIgnorePC);
            if (result == RfidResult.SUCCESS) {
                mInventoryStarted = true;
                updateScanData();
            } else if (result == RfidResult.LOW_BATTERY) {
                Toast.makeText(mActivity,
                        R.string.rfid_alert_low_battery_warning,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mActivity,
                        R.string.rfid_alert_start_inventory_failed,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}
