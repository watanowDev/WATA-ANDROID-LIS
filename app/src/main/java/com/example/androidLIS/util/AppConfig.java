package com.example.androidLIS.util;

public class AppConfig {
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

//    public static String WATA_PLATFORM_URL = "https://dev-lms-api.watalbs.com/monitoring/geofence/addition-info/logistics/heavy-equipment/";
    public static String WATA_PLATFORM_URL = "https://test-lms-api.watalbs.com/monitoring/geofence/addition-info/logistics/heavy-equipment/";

    public static String WORK_LOCATION_ID = "INCHEON_CALT_001";
    public static String VEHICLE_ID = "FORK_LEFT_01";
    public static String GET_IN = "IN";
    public static String GET_OUT = "OUT";

    //RFID 파라미터
//    public static String RFID_MAC = "34:86:5D:71:98:7A";
//    public static String RFID_MAC = "00:05:C4:C1:01:2C";
//    public static String RFID_MAC = "00:05:C4:C1:01:32";
    public static String RFID_MAC = "00:05:C4:C1:01:33";
    public static final int RFID_SCAN_RESULT = 10047;
    public static final int RFID_CONN_FAIL = 10048;
    public static final int RFID_CONN_SUCCESS = 10049;
    public static final int RFID_CONN_READY = 10050;

    public static int RFID_SCAN_INTERVAL = 100;
    public static int RFID_SCAN_CNT_THRESHOLD = 5;
    public static int ON_RACK_THRESHOLD = 10000;
    public static double ON_RACK_RSSI = -70.0;
    public static int RFID_SCAN_TIMEOUT = 40000;


    //환경 파라미터
    public static int FLOOR_HEIGHT = 1900;
    public static int FORK_LENGTH = 1300;
    public static int PICK_THRESHOLD = 800;

    //TOF 카메라 파라미터
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


    //운행 인지 파라미터
    public static int ACC_ACTION_TIMEOUT = 60000;
    public static double ACC_ACTION_THRESHOLD = 1.0;


    //부피 업로드 메트릭스
    public static int MATRIX_X = 10;
    public static int MATRIX_Y = 1;

    //에러코드
    public static String ALIVE_ERROR_NORMAL = "0000";
    public static String ALIVE_ERROR_QR = "0021";
    public static String ALIVE_ERROR_TOF = "0022";
    public static String ALIVE_ERROR_DISTANCE = "0023";
    public static String ALIVE_ERROR_RFID = "0024";
    public static String ALIVE_ERROR_NETWORK = "0025";
    public static String ALIVE_ERROR_BATTERY = "0026";
    public static String ALIVE_ERROR_NOTNORMAL = "0027";


}
