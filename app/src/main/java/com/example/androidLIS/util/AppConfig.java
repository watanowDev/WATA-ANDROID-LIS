package com.example.androidLIS.util;

public class AppConfig {
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String WATA_PLATFORM_URL = "https://dev-lms-api.watalbs.com/monitoring/geofence/addition-info/logistics/heavy-equipment/";

    public static String WORK_LOCATION_ID = "INCHEON_CALT_001";
    public static String VEHICLE_ID = "FORK_LEFT_01";

    public static String GET_IN = "IN";
    public static String GET_OUT = "OUT";

//    public static String RFID_MAC = "34:86:5D:71:98:7A";
//    public static String RFID_MAC = "00:05:C4:C1:01:2C";
//    public static String RFID_MAC = "00:05:C4:C1:01:32";
    public static String RFID_MAC = "00:05:C4:C1:01:33";

    public static final int RFID_SCAN_RESULT = 10047;

    public static int RFID_SCAN_INTERVAL = 50;
    public static int RFID_SCAN_CNT_THRESHOLD = 5;
    public static int FLOOR_HEIGHT = 1900;


    public static int FORK_LENGTH = 1300;
    public static int PICK_THRESHOLD = 1000;


    public static int TOF_WIDTH = 320;
    public static int TOF_RESAMPLING_WIDTH_MIN = 32; //37
    public static int TOF_RESAMPLING_WIDTH_MAX = 69;


    public static int TOF_HEIGHT = 240;
    public static int TOF_RESAMPLING_HEIGHT_MIN = 102; //37
    public static int TOF_RESAMPLING_HEIGHT_MAX = 139;

    public static int TOF_FORK_LENGTH = 30;
    public static int TOF_FORK_THICKNESS = 20;
    public static int TOF_FORK_GAP = 40;

    public static int TOF_RESAMPLING_VOLUME_HEIGHT = 300;
    public static int TOF_RESAMPLING_VOLUME_WIDTH = 200;

    public static int MATRIX_X = 10;
    public static int MATRIX_Y = 1;
}
