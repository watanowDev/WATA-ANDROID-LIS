package com.example.androidLIS.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.WindowManager;

import com.orhanobut.hawk.Hawk;

public class AppUtil {

    private static AppUtil appUtil = null;

    public static AppUtil getInstance() {
        if (appUtil == null) {
            appUtil = new AppUtil();
        }

        return appUtil;
    }

    /**
     * 앱 재시작
     * @param activity
     */

    public void restartApp(Activity activity){

        PackageManager packageManager = activity.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(activity.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        activity.startActivity(mainIntent);
        System.exit(0);
    }



    /**
     * 바이트 배열을 헥사 문자열로 변경
     * @param a
     * @return
     */
    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b: a)
            sb.append(String.format("%02x ", b&0xff));
        return sb.toString();
    }

    public void initSettingParams(){
        if (Hawk.contains("WATA_PLATFORM_URL")) {
            AppConfig.WATA_PLATFORM_URL = Hawk.get("WATA_PLATFORM_URL");
        }
        if (Hawk.contains("WORK_LOCATION_ID")) {
            AppConfig.WORK_LOCATION_ID = Hawk.get("WORK_LOCATION_ID");
        }
        if (Hawk.contains("VEHICLE_ID")) {
            AppConfig.VEHICLE_ID = Hawk.get("VEHICLE_ID");
        }
        if (Hawk.contains("RFID_MAC")) {
            AppConfig.RFID_MAC = Hawk.get("RFID_MAC");
        }
        if (Hawk.contains("RFID_SCAN_CNT_THRESHOLD")) {
            AppConfig.RFID_SCAN_CNT_THRESHOLD = Integer.parseInt(Hawk.get("RFID_SCAN_CNT_THRESHOLD").toString());
        }
        if (Hawk.contains("RFID_SCAN_INTERVAL")) {
            AppConfig.RFID_SCAN_INTERVAL = Integer.parseInt(Hawk.get("RFID_SCAN_INTERVAL").toString());
        }
        if (Hawk.contains("FLOOR_HEIGHT")) {
            AppConfig.FLOOR_HEIGHT = Integer.parseInt(Hawk.get("FLOOR_HEIGHT").toString());
        }
        if (Hawk.contains("PICK_THRESHOLD")) {
            AppConfig.PICK_THRESHOLD = Integer.parseInt(Hawk.get("PICK_THRESHOLD").toString());
        }
        if (Hawk.contains("TOF_WIDTH")) {
            AppConfig.TOF_WIDTH = Integer.parseInt(Hawk.get("TOF_WIDTH").toString());
        }
        if (Hawk.contains("TOF_HEIGHT")) {
            AppConfig.TOF_HEIGHT = Integer.parseInt(Hawk.get("TOF_HEIGHT").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_WIDTH_MIN")) {
            AppConfig.TOF_RESAMPLING_WIDTH_MIN = Integer.parseInt(Hawk.get("TOF_RESAMPLING_WIDTH_MIN").toString());
        }

        if (Hawk.contains("TOF_RESAMPLING_WIDTH_MAX")) {
            AppConfig.TOF_RESAMPLING_WIDTH_MAX = Integer.parseInt(Hawk.get("TOF_RESAMPLING_WIDTH_MAX").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_HEIGHT_MIN")) {
            AppConfig.TOF_RESAMPLING_HEIGHT_MIN = Integer.parseInt(Hawk.get("TOF_RESAMPLING_HEIGHT_MIN").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_HEIGHT_MAX")) {
            AppConfig.TOF_RESAMPLING_HEIGHT_MAX = Integer.parseInt(Hawk.get("TOF_RESAMPLING_HEIGHT_MAX").toString());
        }
        if (Hawk.contains("MATRIX_X")) {
            AppConfig.MATRIX_X = Integer.parseInt(Hawk.get("MATRIX_X").toString());
        }
        if (Hawk.contains("MATRIX_Y")) {
            AppConfig.MATRIX_Y = Integer.parseInt(Hawk.get("MATRIX_Y").toString());
        }

        // 포크 샘플링
        if (Hawk.contains("TOF_FORK_LENGTH")) {
            AppConfig.TOF_FORK_LENGTH = Integer.parseInt(Hawk.get("TOF_FORK_LENGTH").toString());
        }
        if (Hawk.contains("TOF_FORK_THICKNESS")) {
            AppConfig.TOF_FORK_THICKNESS = Integer.parseInt(Hawk.get("TOF_FORK_THICKNESS").toString());
        }
        if (Hawk.contains("TOF_FORK_GAP")) {
            AppConfig.TOF_FORK_GAP = Integer.parseInt(Hawk.get("TOF_FORK_GAP").toString());
        }

        //부피 샘플링
        if (Hawk.contains("TOF_RESAMPLING_VOLUME_WIDTH")) {
            AppConfig.TOF_RESAMPLING_VOLUME_WIDTH = Integer.parseInt(Hawk.get("TOF_RESAMPLING_VOLUME_WIDTH").toString());
        }
        if (Hawk.contains("TOF_RESAMPLING_VOLUME_HEIGHT")) {
            AppConfig.TOF_RESAMPLING_VOLUME_HEIGHT = Integer.parseInt(Hawk.get("TOF_RESAMPLING_VOLUME_HEIGHT").toString());
        }

    }




}
