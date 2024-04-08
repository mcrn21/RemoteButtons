package com.mcrn21.remotebuttons;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.Locale;

public class SerialUsbDevice {
    static public class Info {
        int vendor = -1;
        int product = -1;

        public Info() {}

        public Info(int vendor, int product) {
            this.vendor = vendor;
            this.product = product;
        }

        public boolean equals(Info other) {
            return vendor == other.vendor && product == other.product;
        }
    }
    UsbDevice mDevice;
    int mPort;
    UsbSerialDriver mDriver;

    SerialUsbDevice(UsbDevice device, int port, UsbSerialDriver driver) {
        mDevice = device;
        mPort = port;
        mDriver = driver;
    }

    public UsbDevice getDevice() {
        return mDevice;
    }

    public int getPort() {
        return mPort;
    }

    public UsbSerialDriver getDriver() {
        return mDriver;
    }

    public Info getInfo() {
        if (mDevice == null)
            return null;
        return new Info(mDevice.getVendorId(), mDevice.getProductId());
    }

    @NonNull
    public String toString() {
        String result = "";

        if (mDriver == null)
            result += "<no driver>\n";
        else if (mDriver.getPorts().size() == 1)
            result += mDriver.getClass().getSimpleName().replace("SerialDriver","") + "\n";
        else
            result += mDriver.getClass().getSimpleName().replace("SerialDriver","")+", Port " + mPort + "\n";

        if (mDevice == null)
            result += "Vendor: <no vendor>, Product: <no product>";
        else
            result += String.format(Locale.US, "Vendor: %04X, Product: %04X", mDevice.getVendorId(), mDevice.getProductId());

        return result;
    }

    static public ArrayList<SerialUsbDevice> getSerialUsbDevices(Context context) {
        ArrayList<SerialUsbDevice> serialUsbDevices = new ArrayList<SerialUsbDevice>();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(device);
            if (driver != null) {
                for (int port = 0; port < driver.getPorts().size(); ++port)
                    serialUsbDevices.add(new SerialUsbDevice(device, port, driver));
            } else {
                serialUsbDevices.add(new SerialUsbDevice(device, 0, null));
            }
        }

        return serialUsbDevices;
    }

    static public SerialUsbDevice getSerialUsbDevice(Info deviceInfo, Context context) {
        if (deviceInfo == null)
            return null;

        ArrayList<SerialUsbDevice> availableSerialUsbDevices = SerialUsbDevice.getSerialUsbDevices(context);
        for (SerialUsbDevice serialUsbDevice : availableSerialUsbDevices) {
            if (serialUsbDevice.getInfo() != null && serialUsbDevice.getInfo().equals(deviceInfo))
                return serialUsbDevice;
        }

        return null;
    }
}
