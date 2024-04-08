package com.mcrn21.remotebuttons;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class Settings {
    static public SerialUsbDevice.Info readDeviceInfo(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        String deviceJson = sharedPref.getString("device", "");
        if (!deviceJson.isEmpty())
            return new Gson().fromJson(deviceJson, SerialUsbDevice.Info.class);
        return null;
    }

    static public void writeDeviceInfo(SerialUsbDevice.Info info, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("device", info == null ? "" : new Gson().toJson(info));
        editor.apply();
    }

    static public SerialUsbDeviceConnection.Params readConnectionParams(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        String paramsJson = sharedPref.getString("connectionParams", "");
        return !paramsJson.isEmpty() ? new Gson().fromJson(paramsJson, SerialUsbDeviceConnection.Params.class) :
                new SerialUsbDeviceConnection.Params();
    }

    static public void writeConnectionParams(SerialUsbDeviceConnection.Params params, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("connectionParams", params == null ? "" : new Gson().toJson(params));
        editor.apply();
    }

    static public LaunchApp readLaunchApp(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        String launchAppJson = sharedPref.getString("launchApp", "");
        return !launchAppJson.isEmpty() ? new Gson().fromJson(launchAppJson, LaunchApp.class) :
                new LaunchApp();
    }

    static public void writeLaunchApp(LaunchApp launchApp, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("launchApp", launchApp == null ? "" : new Gson().toJson(launchApp));
        editor.apply();
    }
}
