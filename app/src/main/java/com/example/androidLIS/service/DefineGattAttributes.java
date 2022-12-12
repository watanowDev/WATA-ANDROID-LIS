package com.example.androidLIS.service;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Bruce_Chiang on 2017/3/10.
 */

public class DefineGattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    public static final String DEVICE_INFORMATION = "180a";

    public static final String TELINK_SPP_UUID_SERVICE = "00010203-0405-0607-0809-0a0b0c0d1910";
    private  static String TELINK_SPP_DATA_SERVER2CLIENT = "00010203-0405-0607-0809-0a0b0c0d2b10";
    private  static String TELINK_SPP_DATA_CLIENT2SERVER = "00010203-0405-0607-0809-0a0b0c0d2b11";

    public static final String SPP_UUID_SERVICE = "49535343-fe7d-4ae5-8fa9-9fafd205e455";
    private  static String SPP_DATA_CLIENT2SERVER = "49535343-8841-43f4-a8d4-ecbe34729bb3";
    private  static String SPP_DATA_SERVER2CLIENT = "49535343-1e4d-4bd9-ba61-23c647249616";

    public static final String SLICON_BLE_UUID_SERVICE      = "eca113dc-df06-468d-bfb9-1ac506bdb814";
    private  static String SLICON_BLE_DATA_CLIENT2SERVER    = "d83236ae-be71-4adf-9b3d-b5b8ac1ae195";
    private  static String SLICON_BLE_DATA_SERVER2CLIENT    = "d83236ae-be71-4adf-9b3d-b5b8ac1ae195";
    private  static String SLICON_BLE_MTU_CLIENT2SERVER     = "c9c556d7-222b-42da-b671-9342d130001d";
    private  static String SLICON_BLE_MTU_SERVER2CLIENT     = "c9c556d7-222b-42da-b671-9342d130001d";


    //public static UUID DEVICE_INFORMATION_UUID = UUID.fromString(DEVICE_INFORMATION);

    public static UUID TELINK_READ_UUID = UUID.fromString(TELINK_SPP_DATA_SERVER2CLIENT);
    public static UUID TELINK_WRITE_UUID = UUID.fromString(TELINK_SPP_DATA_CLIENT2SERVER);

    public static UUID READ_UUID = UUID.fromString(SPP_DATA_SERVER2CLIENT);
    public static UUID WRITE_UUID = UUID.fromString(SPP_DATA_CLIENT2SERVER);

    public static UUID SLICON_READ_UUID = UUID.fromString(SLICON_BLE_DATA_CLIENT2SERVER);
    public static UUID SLICON_WRITE_UUID = UUID.fromString(SLICON_BLE_DATA_SERVER2CLIENT);
    public static UUID SLICON_MTUW_UUID = UUID.fromString(SLICON_BLE_MTU_SERVER2CLIENT);
    public static UUID SLICON_MTUR_UUID = UUID.fromString(SLICON_BLE_MTU_CLIENT2SERVER);


    static {
        //attributes.put(DEVICE_INFORMATION, "DEVICE_INFORMATION");
        // TELINK Services.
        attributes.put(TELINK_SPP_UUID_SERVICE, "TELINK_SPP_UUID_SERVICE");
        // Standard Services.
        attributes.put(SPP_UUID_SERVICE, "SPP_UUID_SERVICE");
        // SLICON Services.
        attributes.put(SLICON_BLE_UUID_SERVICE, "SLICON_BLE_UUID_SERVICE");
    }

    public static boolean lookUpService(String uuid) {
        String name = attributes.get(uuid);
        return (name == null) ? false : true;
    }
}
