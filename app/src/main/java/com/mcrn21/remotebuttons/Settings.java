package com.mcrn21.remotebuttons;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    static public class Connection {
        public Connection() {}
        public Connection(int baudRate, int dataBits, int parity, int stopBits) {
            this.baudRate = baudRate;
            this.dataBits = dataBits;
            this.parity = parity;
            this.stopBits = stopBits;
        }

        public int baudRate = 9600;
        public int dataBits = 8;
        public int parity = 0;
        public int stopBits = 1;
    }

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

    static public Connection readConnectionSettings(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        return new Connection(sharedPref.getInt("baudRate", 9600),
                sharedPref.getInt("dataBits", 8),
                sharedPref.getInt("parity", 0),
                sharedPref.getInt("stopBits", 1));
    }

    static public void writeConnectionSettings(Connection conn, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("baudRate", conn.baudRate);
        editor.putInt("dataBits", conn.dataBits);
        editor.putInt("parity", conn.parity);
        editor.putInt("stopBits", conn.stopBits);
        editor.apply();
    }
}
