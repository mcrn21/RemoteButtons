package com.mcrn21.remotebuttons;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

public class DeviceConnection implements SerialInputOutputManager.Listener {
    public interface OnStateChangedListener {
        void onChanged(boolean state, CharSequence text);
    }
    public interface OnCommandListener {
        void onCommand(String command);
    }
    public enum UsbPermission { Unknown, Requested, Granted, Denied }
    private Context mContext = null;
    private Device mDevice = new Device();
    private ConnectionParams mParams = new ConnectionParams();
    private UsbPermission mUsbPermission = UsbPermission.Unknown;
    private SerialInputOutputManager mUsbIoManager = null;
    private UsbSerialPort mUsbSerialPort = null;
    private String mUsbSerialBuffer = "";
    private OnStateChangedListener mOnStateChangedListener = null;
    private OnCommandListener mOnCommandListener = null;

    DeviceConnection(Context context) {
        mContext = context;
    }

    public Device getDevice() {
        return mDevice;
    }

    public void setDevice(Device device) {
        mDevice = device;
    }

    public ConnectionParams getParams() {
        return mParams;
    }

    public void setParams(ConnectionParams params) {
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

        if (mDevice == null)
            return;

        UsbDevice usbDevice = null;
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        for(UsbDevice v : usbManager.getDeviceList().values()) {
            if (v.getVendorId() == mDevice.vendorId && v.getProductId() == mDevice.productId)
                usbDevice = v;
        }

        if (usbDevice == null)
            return;

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(usbDevice);
        if (driver == null) {
            stateChanged(false, mContext.getText(R.string.driver_not_found));
            return;
        }

        if(driver.getPorts().size() < mDevice.port) {
            stateChanged(false, mContext.getText(R.string.not_enough_ports_at_device));
            return;
        }

        mUsbSerialPort = driver.getPorts().get(mDevice.port);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());

        if(usbConnection == null && mUsbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            mUsbPermission = UsbPermission.Requested;
            int flags = PendingIntent.FLAG_MUTABLE;
            Intent intent = new Intent(Common.INTENT_ACTION_GRANT_USB);
            intent.setPackage(mContext.getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, intent, flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }

        if(usbConnection == null) {
            stateChanged(false, !usbManager.hasPermission(driver.getDevice()) ?
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
