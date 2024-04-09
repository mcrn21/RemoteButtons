package com.mcrn21.remotebuttons;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.lang.reflect.InvocationTargetException;

public class RemoteButtonsService  extends Service {
    private boolean mIsRunning = false;
    private DeviceConnection mDeviceConnection = null;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        initBroadcastReceiver();
        initDeviceConnection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRunning) {
            mIsRunning = true;
            NotificationsHelper.createNotificationChannel(this);
            startForeground(Common.SERVICE_ID, NotificationsHelper.buildNotification(this));
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        stopForeground(true);
        mIsRunning = false;
        super.onDestroy();
    }

    private void initBroadcastReceiver() {
        IntentFilter broadcastReceiverFilter = new IntentFilter();
        broadcastReceiverFilter.addAction(Common.INTENT_ACTION_GRANT_USB);
        broadcastReceiverFilter.addAction(Common.INTENT_ACTION_USB_ATTACHED);
        broadcastReceiverFilter.addAction(Common.INTENT_ACTION_USB_DETACHED);
        broadcastReceiverFilter.addAction(Common.INTENT_START_SERIAL);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Common.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    mDeviceConnection.setUsbPermission(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? DeviceConnection.UsbPermission.Granted : DeviceConnection.UsbPermission.Denied);
                    mDeviceConnection.connect();
                } else if (Common.INTENT_ACTION_USB_ATTACHED.equals(intent.getAction())) {
                    mDeviceConnection.connect();
                } else if (Common.INTENT_ACTION_USB_DETACHED.equals(intent.getAction())) {
                    mDeviceConnection.disconnect();
                } else if (Common.INTENT_START_SERIAL.equals(intent.getAction())) {
                    updateDeviceConnection();
                    mDeviceConnection.connect();
                }
            }
        };

        registerReceiver(mBroadcastReceiver, broadcastReceiverFilter, RECEIVER_EXPORTED);
    }

    private void initDeviceConnection() {
        mDeviceConnection = new DeviceConnection(this);

        mDeviceConnection.setOnStateChangedListener(new DeviceConnection.OnStateChangedListener() {
            @Override
            public void onChanged(boolean state, CharSequence text) {
                Toast.makeText(RemoteButtonsService.this, text, Toast.LENGTH_SHORT).show();
                sendSerialConnectionUpdated(state);
            }
        });

        mDeviceConnection.setOnCommandListener(new DeviceConnection.OnCommandListener() {
            @Override
            public void onCommand(String command) {
                sendRemoteButtonsCommand(command);
                try {
                    Commands.class.getMethod(command, RemoteButtonsService.class).invoke(null, RemoteButtonsService.this);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        updateDeviceConnection();
        mDeviceConnection.connect();
    }

    private void updateDeviceConnection() {
        SharedPreferences sharedPref = getSharedPreferences(Common.SETTINGS_FILE, Context.MODE_PRIVATE);
        Device device = new Gson().fromJson(sharedPref.getString("device", ""), Device.class);
        ConnectionParams params = new ConnectionParams();
        params.baudRate = Integer.parseInt(sharedPref.getString("baudRate", "9600"));
        params.dataBits = Integer.parseInt(sharedPref.getString("dataBits", "8"));
        params.parity = Integer.parseInt(sharedPref.getString("parity", "1"));
        params.stopBits = Integer.parseInt(sharedPref.getString("stopBits", "1"));

        mDeviceConnection.setDevice(device);
        mDeviceConnection.setParams(params);
    }

    private void sendSerialConnectionUpdated(boolean state) {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_SERIAL_CONNECTION_UPDATED);
        intent.putExtra("state", state);
        sendBroadcast(intent);
    }

    private void sendRemoteButtonsCommand(String command) {
        Intent intent = new Intent();
        intent.setAction(Common.INTENT_REMOTE_BUTTONS_COMMAND);
        intent.putExtra("command", command);
        sendBroadcast(intent);
    }

}
