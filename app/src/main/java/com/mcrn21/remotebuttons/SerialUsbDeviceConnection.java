package com.mcrn21.remotebuttons;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SerialUsbDeviceConnection  implements SerialInputOutputManager.Listener {
    public interface OnStateChangedListener {
        void onChanged(boolean state, CharSequence text);
    }
    public interface OnCommandListener {
        void onCommand(String command);
    }
    static public class Params {
        public Params() {}
        public Params(int baudRate, int dataBits, int parity, int stopBits) {
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
    public enum UsbPermission { Unknown, Requested, Granted, Denied }
    private Context mContext = null;
    private SerialUsbDevice.Info mDeviceInfo = null;
    private Params mParams = new Params();
    private UsbPermission mUsbPermission = UsbPermission.Unknown;
    private SerialInputOutputManager mUsbIoManager = null;
    private UsbSerialPort mUsbSerialPort = null;
    private String mUsbSerialBuffer = "";
    private OnStateChangedListener mOnStateChangedListener = null;
    private OnCommandListener mOnCommandListener = null;

    SerialUsbDeviceConnection(Context context) {
        mContext = context;
    }

    public SerialUsbDevice.Info getDeviceInfo() {
        return mDeviceInfo;
    }

    public void setDeviceInfo(SerialUsbDevice.Info deviceInfo) {
        mDeviceInfo = deviceInfo;
    }

    public Params getParams() {
        return mParams;
    }

    public void setParams(Params params) {
        mParams = params;
    }

    public UsbPermission getUsbPermission() {
        return mUsbPermission;
    }

    public void setUsbPermission(UsbPermission usbPermission) {
        mUsbPermission = usbPermission;
    }

    public void connect() {
        if (mContext == null)
            throw new RuntimeException("Context is null");

        disconnect();

        SerialUsbDevice serialUsbDevice = SerialUsbDevice.getSerialUsbDevice(mDeviceInfo, mContext);

        if (serialUsbDevice == null)
            return;

        if (serialUsbDevice.getDriver() == null) {
            stateChanged(false, mContext.getText(R.string.driver_not_found));
            return;
        }

        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mUsbSerialPort = serialUsbDevice.getDriver().getPorts().get(serialUsbDevice.getPort());
        UsbDeviceConnection usbConnection = usbManager.openDevice(serialUsbDevice.getDevice());

        if(usbConnection == null && mUsbPermission == UsbPermission.Unknown && !usbManager.hasPermission(serialUsbDevice.getDevice())) {
            mUsbPermission = UsbPermission.Requested;
            int flags = PendingIntent.FLAG_MUTABLE;
            Intent intent = new Intent(Common.INTENT_ACTION_GRANT_USB);
            intent.setPackage(mContext.getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, intent, flags);
            usbManager.requestPermission(serialUsbDevice.getDevice(), usbPermissionIntent);
            return;
        }

        if(usbConnection == null) {
            stateChanged(false, !usbManager.hasPermission(serialUsbDevice.getDevice()) ?
                    mContext.getText(R.string.open_connection_permissions_denied) : mContext.getText(R.string.open_connection_failed));
            return;
        }

        try {
            mUsbSerialPort.open(usbConnection);

            try{
                mUsbSerialPort.setParameters(mParams.baudRate, mParams.dataBits, mParams.stopBits, mParams.parity);
            }catch (UnsupportedOperationException e){
                stateChanged(false, mContext.getText(R.string.unsupported_serial_port_parameters));
            }

            mUsbIoManager = new SerialInputOutputManager(mUsbSerialPort, this);
            mUsbIoManager.start();

            stateChanged(true, mContext.getText(R.string.successful_connect));
        } catch (Exception e) {
            stateChanged(false, mContext.getText(R.string.failed_connect) + e.getMessage());
        }
    }

    public void disconnect() {
        if (mContext == null)
            throw new RuntimeException("Context is null");

        if (mUsbSerialPort != null)
            stateChanged(false, mContext.getText(R.string.disconnect));

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
    }

    public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
        mOnStateChangedListener = onStateChangedListener;
    }

    public void setOnCommandListener(OnCommandListener onCommandListener) {
        mOnCommandListener = onCommandListener;
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

            if (!command.isEmpty())
                commandReceived(command);
        }
    }

    @Override
    public void onRunError(Exception e) {

    }

    private void stateChanged(boolean state, CharSequence text) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onChanged(state, text);
    }

    private void commandReceived(String command) {
        if (mOnCommandListener != null)
            mOnCommandListener.onCommand(command);
    }
}
