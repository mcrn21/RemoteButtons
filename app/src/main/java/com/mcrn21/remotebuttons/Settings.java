package com.mcrn21.remotebuttons;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class Settings {
    public Device device = new Device();
    public ConnectionParams connectionParams = new ConnectionParams();
    public String launchAppPackageName = "";
    public boolean launchAppEnable = false;

    private Settings() {
    }

    private static final class InstanceHolder {
        static final Settings INSTANCE = new Settings();
    }

    public static Settings getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void load(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);

        String deviceJson = sharedPref.getString("device", "");
        if (!deviceJson.isEmpty())
            device = new Gson().fromJson(deviceJson, Device.class);

        String connectionParamsJson = sharedPref.getString("connectionParams", "");
        if (!connectionParamsJson.isEmpty())
            connectionParams = new Gson().fromJson(connectionParamsJson, ConnectionParams.class);

        launchAppPackageName = sharedPref.getString("launchAppPackageName", "");
        launchAppEnable = sharedPref.getBoolean("launchAppEnable", false);
    }

    public void save(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("device", new Gson().toJson(device));
        editor.putString("connectionParams", new Gson().toJson(connectionParams));
        editor.putString("launchAppPackageName", launchAppPackageName);
        editor.putBoolean("launchAppEnable", launchAppEnable);

        editor.apply();
    }
}
