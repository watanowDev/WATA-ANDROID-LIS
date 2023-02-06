/*
 * Copyright (C) Apulsetech,co.ltd
 * Apulsetech, Shenzhen, China
 *
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice is
 * included in all copies of any software which is or includes a copy or
 * modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY. IN PARTICULAR, NEITHER THE AUTHOR NOR APULSETECH MAKES ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY OF
 * THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 *
 * Project: ⍺811
 *
 * File: Setting.java
 * Date: 2020.09.29
 * Author: Tony Park, tonypark@apulsetech.com
 *
 ****************************************************************************
 */

package com.example.androidLIS.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.apulsetech.lib.util.LogUtil;

public class Setting {

    @SuppressWarnings("unused")
    private static final String TAG = "Setting";
    @SuppressWarnings("unused")
    private static final boolean D = false;

    private static final String PREF_DEMO_SETTING = "demo_setting";
    private static final String VALUE_BARCODE_VOLUME = "barcode_volume";
    private static final String VALUE_RFID_VOLUME = "rfid_volume";

    private final int DEFAULT_VOLUME = 15;

    private final SharedPreferences mSettingPreferences;
    private final SharedPreferences.Editor mPreferencesEditor;

    @SuppressLint("ApplySharedPref")
    public Setting(Context context) throws NullPointerException {
        try {
            mSettingPreferences = context.getSharedPreferences(PREF_DEMO_SETTING,
                    Context.MODE_MULTI_PROCESS);
            mPreferencesEditor = mSettingPreferences.edit();

            int volume = mSettingPreferences.getInt(VALUE_BARCODE_VOLUME, DEFAULT_VOLUME);
            mPreferencesEditor.putInt(VALUE_BARCODE_VOLUME, volume);

            volume = mSettingPreferences.getInt(VALUE_RFID_VOLUME, DEFAULT_VOLUME);
            mPreferencesEditor.putInt(VALUE_RFID_VOLUME, volume);

            mPreferencesEditor.commit();
        } catch (NullPointerException e) {
            throw new NullPointerException(e.getMessage());
        }
    }

    public int getBarcodeVolumeLevel() {
        int volumLevel = mSettingPreferences.getInt(VALUE_BARCODE_VOLUME, DEFAULT_VOLUME);

        LogUtil.log(LogUtil.LV_D, D, TAG, "getBarcodeVolumeLevel() volumLevel=" + volumLevel);

        return volumLevel;
    }

    public void setBarcodeVolumeLevel(int volumLevel) {
        LogUtil.log(LogUtil.LV_D, D, TAG, "setBarcodeVolumeLevel() volumLevel=" + volumLevel);

        mPreferencesEditor.putInt(VALUE_BARCODE_VOLUME, volumLevel);
        mPreferencesEditor.commit();
    }

    public int getRfidVolumeLevel() {
        int volumLevel = mSettingPreferences.getInt(VALUE_RFID_VOLUME, DEFAULT_VOLUME);

        LogUtil.log(LogUtil.LV_D, D, TAG, "getRfidVolumeLevel() volumLevel=" + volumLevel);

        return volumLevel;
    }

    public void setRfidVolumeLevel(int volumLevel) {
        LogUtil.log(LogUtil.LV_D, D, TAG, "setRfidVolumeLevel() volumLevel=" + volumLevel);

        mPreferencesEditor.putInt(VALUE_RFID_VOLUME, volumLevel);
        mPreferencesEditor.commit();
    }
}