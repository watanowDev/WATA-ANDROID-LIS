package com.example.androidLIS.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
     * 네트워크 상태 체크
     * @param context
     * @return
     */
    public boolean isNetworkConnected(Context context)
    {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo wimax = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        boolean bwimax = false;
        if (wimax != null)
        {
            bwimax = wimax.isConnected();
        }
        if (mobile != null)
        {
            if (mobile.isConnected() || wifi.isConnected() || bwimax)
            {
                return true;
            }
        }
        else
        {
            if (wifi.isConnected() || bwimax)
            {
                return true;
            }
        }
        return false;
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




}
