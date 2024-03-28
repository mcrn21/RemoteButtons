package com.mcrn21.remotebuttons;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    static public int readDeviceId(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        return sharedPref.getInt("deviceId", -1);
    }
    static public void writeDeviceId(int deviceId, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("deviceId", deviceId);
        editor.apply();
    }
}
