package com.mcrn21.remotebuttons;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;

public class RemoteButtonsService  extends Service {
    private boolean mIsRunning = false;
    private DeviceConnection mDeviceConnection = null;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        Settings.getInstance().load(this);

        initBroadcastReceiver();
        initSerialUsbDeviceConnection();
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
        Settings.getInstance().save(this);
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
                    mDeviceConnection.setDevice(Settings.getInstance().device);
                    mDeviceConnection.setParams(Settings.getInstance().connectionParams);
                    mDeviceConnection.connect();
                }
            }
        };

        registerReceiver(mBroadcastReceiver, broadcastReceiverFilter, RECEIVER_EXPORTED);
    }

    private void initSerialUsbDeviceConnection() {
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

        mDeviceConnection.setDevice(Settings.getInstance().device);
        mDeviceConnection.setParams(Settings.getInstance().connectionParams);
        mDeviceConnection.connect();
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
