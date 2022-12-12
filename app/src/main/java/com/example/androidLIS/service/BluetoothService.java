package com.example.androidLIS.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Created by Bruce_Chiang on 2017/3/10.
 */

public class BluetoothService extends Service {

    public static final String BLE_ACTION_SERVICE_START = "BLE_ACTION_SERVICE_START";
    public static final String BLE_ACTION_SERVICE_STOP = "BLE_ACTION_SERVICE_STOP";
    public static final String BLE_ACTION_CONNECT = "BLE_ACTION_CONNECT";
    public static final String BLE_ACTION_SEND_DATA	= "BLE_ACTION_SEND_DATA";
    public static final String BLE_ACTION_SEND_MTU	= "BLE_ACTION_SEND_MTU";
    public static final String BLE_ACTION_RECEIVE_DATA	= "BLE_ACTION_RECEIVE_DATA";
    public static final String BLE_ACTION_DISCONNECT = "BLE_ACTION_DISCONNECT";
    public static final String BLE_ACTION_GATT_CONNECTED = "BLE_ACTION_GATT_CONNECTED";
    public static final String BLE_ACTION_GATT_DISCONNECTED = "BLE_ACTION_GATT_DISCONNECTED";
    public static final String BLE_ACTION_CHANGE_INTERFACE = "BLE_ACTION_CHANGE_INTERFACE";
    public static final String BLE_ACTION_GATT_MTU = "BLE_ACTION_GATT_MTU";
    public static final String BLE_ACTION_GATT_MTU_CALLBACK = "BLE_ACTION_GATT_MTU_CALLBACK";

    public static final String INTERFACE_BLE = "INTERFACE_BLE";
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String BYTES_DATA = "BYTES_DATA";
    public static final String STRING_DATA = "STRING_DATA";
    public static final String INT_DATA = "INT_DATA";

    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int REQUEST_CODE_LOCATION_SETTINGS = 2;

    private MsgBLEReceiver mMsgBLEReceiver;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic
            mWriteGattCharacteristic = null,
            mReadGattCharacteristic = null,
            mMtuWGattCharacteristic = null,
            mMtuRGattCharacteristic = null;
    private BluetoothDevice mBluetoothDevice = null;
    private boolean mLeConnected = false;
    private int mMTUDataLen = 128;//20;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mMsgBLEReceiver = new MsgBLEReceiver();
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_SERVICE_START));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_SERVICE_STOP));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_CONNECT));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_SEND_DATA));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_SEND_MTU));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_DISCONNECT));
        registerReceiver(mMsgBLEReceiver, new IntentFilter(BLE_ACTION_GATT_MTU));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
        unregisterReceiver(mMsgBLEReceiver);
    }


    public class MsgBLEReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BLE_ACTION_SERVICE_START:
                    break;
                case BLE_ACTION_SERVICE_STOP:
                    if(mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                    }
                    mLeConnected = false;
                    break;
                case BLE_ACTION_CONNECT:
                    if (!mLeConnected) {
                        mBluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(intent.getExtras().getString(DEVICE_ADDRESS));
                        //Log.e("conn",BluetoothDevice.DEVICE_TYPE_LE +",,"+mBluetoothDevice.getType());
                        connectTo(mBluetoothDevice);
                    }
                    break;
                case BLE_ACTION_SEND_DATA:
                    byte[] _data = intent.getExtras().getByteArray(BYTES_DATA);
                    sendData(_data);
                    break;
                case BLE_ACTION_SEND_MTU:
                    int _val = intent.getExtras().getInt(INT_DATA);
                    byte[] _mtu = {(byte)_val};
                    sendMtu(_mtu);
                    break;
                case BLE_ACTION_DISCONNECT:
                    if(mBluetoothGatt != null) {
                        sendBroadcast(BLE_ACTION_GATT_DISCONNECTED, "Disconnected from GATT server.");
                        mBluetoothGatt.disconnect();
                        mBluetoothGatt.close();
                        mLeConnected = false;
                    }
                    break;
                case BLE_ACTION_GATT_MTU:
                    int len = intent.getExtras().getInt(INT_DATA);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if(len >= 20) {
                            if (!mBluetoothGatt.requestMtu(len + 3)) {
                                sendBroadcast(BLE_ACTION_GATT_MTU_CALLBACK, "The new MTU value has been requested unsuccessfully.");
                            }
                            else {
                                sendBroadcast(BLE_ACTION_GATT_MTU_CALLBACK, String.format("send MTU request = %3d.", len));
                            }
                        }
                    }
                    else {
                        sendBroadcast(BLE_ACTION_GATT_MTU_CALLBACK, "The SDK version less than LOLLIPOP(21).");
                    }
                    break;
            }
        }
    }


    private synchronized void connectTo(@NonNull BluetoothDevice device) {
//        if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE)
            this.mBluetoothGatt = device.connectGatt(this, false, mGattCallback);


    }




    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                sendBroadcast(BLE_ACTION_GATT_CONNECTED, "Connected to GATT server.");
                mBluetoothGatt.discoverServices();
                mLeConnected = true;
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                sendBroadcast(BLE_ACTION_GATT_DISCONNECTED, "Disconnected from GATT server.");
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mLeConnected = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> _gattServices = gatt.getServices();
            BluetoothGattService _targetGattService = null;
            String _uuid;

            for (BluetoothGattService gattService : _gattServices) {
                _uuid = gattService.getUuid().toString();
                if(DefineGattAttributes.lookUpService(_uuid)) {
                    _targetGattService = gattService;
                    break;
                }
            }

            if (_targetGattService != null) {

                _uuid = _targetGattService.getUuid().toString();
                List<BluetoothGattCharacteristic> characteristic = _targetGattService.getCharacteristics();

                switch(_uuid) {
                    case DefineGattAttributes.TELINK_SPP_UUID_SERVICE:
                        mWriteGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.TELINK_WRITE_UUID);
                        mReadGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.TELINK_READ_UUID);
                        break;
                    case DefineGattAttributes.SPP_UUID_SERVICE:
                        mWriteGattCharacteristic = characteristic.get(3);
                        mReadGattCharacteristic = characteristic.get(0);
                        break;
                    case DefineGattAttributes.SLICON_BLE_UUID_SERVICE:
                    default:
                        mWriteGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.SLICON_WRITE_UUID);
                        mReadGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.SLICON_READ_UUID);
                        mMtuWGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.SLICON_MTUW_UUID);
                        mMtuRGattCharacteristic = _targetGattService.getCharacteristic(DefineGattAttributes.SLICON_MTUR_UUID);
                            break;
                }


                if (mReadGattCharacteristic != null) {
                    boolean _b = gatt.setCharacteristicNotification(mReadGattCharacteristic, true);

                    if(_b) {
                        List<BluetoothGattDescriptor> descriptorList = mReadGattCharacteristic.getDescriptors();
                        if(descriptorList != null && descriptorList.size() > 0) {
                            for(BluetoothGattDescriptor descriptor : descriptorList) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBluetoothGatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }

                if (mMtuRGattCharacteristic != null) {
                    boolean _b = gatt.setCharacteristicNotification(mMtuRGattCharacteristic, true);
                    if(_b) {
                        List<BluetoothGattDescriptor> descriptorList = mMtuRGattCharacteristic.getDescriptors();
                        if(descriptorList != null && descriptorList.size() > 0) {
                            for(BluetoothGattDescriptor descriptor : descriptorList) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBluetoothGatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }

            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (DefineGattAttributes.SLICON_MTUR_UUID.equals(characteristic.getUuid())) {
                    data = characteristic.getValue();
                    mMTUDataLen = data[0];
                }
                else {
                    data = characteristic.getValue();
                    if (data != null && data.length > 0) {//&& data[0] != 0
                        sendBroadcast(BLE_ACTION_RECEIVE_DATA, data);
                    }
                }
            }
            //super.onCharacteristicRead(gatt, characteristic, status);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data;
            if (DefineGattAttributes.SLICON_MTUR_UUID.equals(characteristic.getUuid())) {

                data = characteristic.getValue();
                mMTUDataLen = data[0];
            }
            else {
                data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    sendBroadcast(BLE_ACTION_RECEIVE_DATA, data);
                }
            }
            //super.onCharacteristicChanged(gatt, characteristic);
        }

        //Callback indicating the MTU for a given device connection has changed.
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == 0) {
				mMTUDataLen = mtu - 3;
				sendBroadcast(BLE_ACTION_GATT_MTU_CALLBACK, String.format("Callback indicating the MTU has changed to %3d.", mMTUDataLen));
			}
            else {
                mMTUDataLen = 20;//20;
                sendBroadcast(BLE_ACTION_GATT_MTU_CALLBACK, String.format("Callback error that the MTU use default value %3d.", mMTUDataLen));
            }
            super.onMtuChanged(gatt, mtu, status);
        }


    };

    //change data field from 20 to mMTUDataLen.
    private synchronized void sendData(@NonNull byte[] value) {
        if(mWriteGattCharacteristic != null && mBluetoothGatt != null && mLeConnected == true) {

            int _targetLen = 0;
            int offset = 0;
            for(int len = value.length; len > 0; len -= mMTUDataLen) {
                if(len < mMTUDataLen)
                    _targetLen = len;
                else
                    _targetLen = mMTUDataLen;
                byte[] _targetByte = new byte[_targetLen];
                System.arraycopy(value, offset, _targetByte, 0, _targetLen);
                offset += mMTUDataLen;
                mWriteGattCharacteristic.setValue(_targetByte);
                mBluetoothGatt.writeCharacteristic(mWriteGattCharacteristic);
                try {
                    Thread.sleep(6);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //if(mReadGattCharacteristic != null)
                //mBluetoothGatt.setCharacteristicNotification(mReadGattCharacteristic, true);
                //mBluetoothGatt.readCharacteristic(mReadGattCharacteristic);
        }
    }

    private synchronized void sendMtu(@NonNull byte[] value) {
        if(mMtuWGattCharacteristic != null && mBluetoothGatt != null && mLeConnected == true) {
            mMtuWGattCharacteristic.setValue(value);
            mBluetoothGatt.writeCharacteristic(mMtuWGattCharacteristic);
        }
    }

    private void sendBroadcast(String action, @NonNull byte[] data) {
        Intent i = new Intent(action);
        i.putExtra(BYTES_DATA, data);
        sendBroadcast(i);
    }

    private void sendBroadcast(String action, @NonNull String data) {
        Intent i = new Intent(action);
        i.putExtra(STRING_DATA, data);
        sendBroadcast(i);
    }
}
