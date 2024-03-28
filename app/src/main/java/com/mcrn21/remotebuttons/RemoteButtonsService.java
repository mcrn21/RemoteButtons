package com.mcrn21.remotebuttons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class RemoteButtonsService  extends Service implements SerialInputOutputManager.Listener {
    private boolean mIsRunning = false;
    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private final HashMap<String, Method> mCommands = new HashMap<>();
    private BroadcastReceiver mBroadcastReceiver;
    private UsbPermission mUsbPermission = UsbPermission.Unknown;
    private SerialInputOutputManager mUsbIoManager = null;
    private UsbSerialPort mUsbSerialPort = null;
    private String mUsbSerialBuffer = "";
    private int mDeviceId = -1;

    public RemoteButtonsService() {
        try {
            mCommands.put(Common.LEFT_COMMAND, RemoteButtonsService.class.getMethod("leftCommand"));
            mCommands.put(Common.RIGHT_COMMAND, RemoteButtonsService.class.getMethod("rightCommand"));
            mCommands.put(Common.ENTER_COMMAND, RemoteButtonsService.class.getMethod("enterCommand"));
            mCommands.put(Common.LEFT_UP_COMMAND, RemoteButtonsService.class.getMethod("leftUpCommand"));
            mCommands.put(Common.LEFT_DOWN_COMMAND, RemoteButtonsService.class.getMethod("leftDownCommand"));
            mCommands.put(Common.RIGHT_UP_COMMAND, RemoteButtonsService.class.getMethod("rightUpCommand"));
            mCommands.put(Common.RIGHT_DOWN_COMMAND, RemoteButtonsService.class.getMethod("rightDownCommand"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initBroadcastReceiver();

        mDeviceId = Settings.readDeviceId(this);
        connectToSerialUsbDevice();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRunning) {
            startForeground();
            mIsRunning = true;
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

    @Override
    public void onNewData(byte[] data) {
        mUsbSerialBuffer += new String(data);

        while (true) {
            int p = mUsbSerialBuffer.indexOf("\r\n");
            if (p == -1)
                break;

            String command = mUsbSerialBuffer.substring(0, p);
            mUsbSerialBuffer = mUsbSerialBuffer.substring(p + 2);

            if (!command.isEmpty()) {
                sendRemoteButtonsCommand(command);

                try {
                    mCommands.get(command).invoke(this);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, Common.NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.remote_buttons_notify_title))
                .setContentText(getString(R.string.remote_buttons_notify_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(Common.SERVICE_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel serviceChannel = new NotificationChannel(
                    Common.NOTIFICATIONS_CHANNEL_ID,
                    appName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
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
                    mUsbPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            ? UsbPermission.Granted : UsbPermission.Denied;
                    connectToSerialUsbDevice();
                } else if (Common.INTENT_ACTION_USB_ATTACHED.equals(intent.getAction())) {
                    connectToSerialUsbDevice();
                } else if (Common.INTENT_ACTION_USB_DETACHED.equals(intent.getAction())) {
                    disconnectFromSerialUsbDevice();
                } else if (Common.INTENT_START_SERIAL.equals(intent.getAction())) {
                    mDeviceId = Settings.readDeviceId(RemoteButtonsService.this);
                    connectToSerialUsbDevice();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            registerReceiver(mBroadcastReceiver, broadcastReceiverFilter, RECEIVER_EXPORTED);
        else
            registerReceiver(mBroadcastReceiver, broadcastReceiverFilter);
    }

    private void connectToSerialUsbDevice() {
        disconnectFromSerialUsbDevice();

        SerialUsbDevice serialUsbDevice = SerialUsbDevice.getSerialUsbDevice(mDeviceId, this);

        if (serialUsbDevice == null)
            return;

        if (serialUsbDevice.getDriver() == null) {
            Toast.makeText(this, getString(R.string.driver_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbSerialPort = serialUsbDevice.getDriver().getPorts().get(serialUsbDevice.getPort());
        UsbDeviceConnection usbConnection = usbManager.openDevice(serialUsbDevice.getDevice());

        if(usbConnection == null && mUsbPermission == UsbPermission.Unknown && !usbManager.hasPermission(serialUsbDevice.getDevice())) {
            mUsbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            Intent intent = new Intent(Common.INTENT_ACTION_GRANT_USB);
            intent.setPackage(getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
            usbManager.requestPermission(serialUsbDevice.getDevice(), usbPermissionIntent);
            return;
        }

        if(usbConnection == null) {
            if (!usbManager.hasPermission(serialUsbDevice.getDevice()))
                Toast.makeText(this, getString(R.string.open_connection_permissions_denied), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.open_connection_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mUsbSerialPort.open(usbConnection);

            try{
                mUsbSerialPort.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE);
            }catch (UnsupportedOperationException e){
                Toast.makeText(this, getString(R.string.unsupported_serial_port_parameters), Toast.LENGTH_SHORT).show();
            }

            mUsbIoManager = new SerialInputOutputManager(mUsbSerialPort, this);
            mUsbIoManager.start();
            Toast.makeText(this, getString(R.string.successful_connect), Toast.LENGTH_SHORT).show();
            sendSerialConnectionUpdated(true);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.failed_connect) + e.getMessage(), Toast.LENGTH_SHORT).show();
            disconnectFromSerialUsbDevice();
        }
    }

    private void disconnectFromSerialUsbDevice() {
        if(mUsbIoManager != null) {
            mUsbIoManager.setListener(null);
            mUsbIoManager.stop();
        }

        try {
            if(mUsbSerialPort != null)
                mUsbSerialPort.close();
        } catch (IOException ignored) {}

        mUsbIoManager = null;
        mUsbSerialPort = null;
        mUsbSerialBuffer = "";
        mUsbPermission = UsbPermission.Unknown;
        sendSerialConnectionUpdated(false);
    }

    public void leftCommand() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public void rightCommand() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    public void enterCommand() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (audio.isMusicActive()) {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
            audio.dispatchMediaKeyEvent(event);
        } else {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
            audio.dispatchMediaKeyEvent(event);
        }
    }

    public void leftUpCommand() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audio.dispatchMediaKeyEvent(event);
    }

    public void leftDownCommand() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audio.isStreamMute(AudioManager.STREAM_MUSIC))
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        else
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
    }

    public void rightUpCommand() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        audio.dispatchMediaKeyEvent(event);
    }

    public void rightDownCommand() {

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
