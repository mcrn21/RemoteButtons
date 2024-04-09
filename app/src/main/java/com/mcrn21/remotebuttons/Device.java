package com.mcrn21.remotebuttons;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.Locale;

public class Device implements Parcelable {
    public int vendorId = -1;
    public int productId = -1;
    public int port = -1;
    public String description = "";

    public Device() {}

    public Device(int vendorId, int productId, int port, String description) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.port = port;
        this.description = description;
    }

    private Device(Parcel in) {
        vendorId = in.readInt();
        productId = in.readInt();
        port = in.readInt();
        description = in.readString();
    }

    public boolean isValid() {
        return vendorId != -1 && productId != -1;
    }

    public void reset() {
        vendorId = -1;
        productId = -1;
        port = -1;
        description = "";
    }

    static public ArrayList<Device> getDevices(Context context) {
        ArrayList<Device> devices = new ArrayList<Device>();

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbSerialProber usbDefaultProber = UsbSerialProber.getDefaultProber();

        for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            UsbSerialDriver driver = usbDefaultProber.probeDevice(usbDevice);
            if (driver != null) {
                for (int port = 0; port < driver.getPorts().size(); ++port)
                    devices.add(new Device(usbDevice.getVendorId(), usbDevice.getProductId(),
                            port, createDescription(usbDevice, port, driver)));
            } else {
                devices.add(new Device(usbDevice.getVendorId(), usbDevice.getProductId(),
                        0, createDescription(usbDevice, 0, null)));
            }
        }

        return devices;
    }

    static private String createDescription(UsbDevice usbDevice, int port, UsbSerialDriver usbSerialDriver) {
        String description = "";

        if (usbSerialDriver == null)
            description += "<no driver>\n";
        else if (usbSerialDriver.getPorts().size() == 1)
            description += usbSerialDriver.getClass().getSimpleName().replace("SerialDriver","") + "\n";
        else
            description += usbSerialDriver.getClass().getSimpleName().replace("SerialDriver","") + ", Port: " + port + "\n";

        description += String.format(Locale.US, "Vendor: %04X, Product: %04X", usbDevice.getVendorId(), usbDevice.getProductId());

        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(vendorId);
        out.writeInt(productId);
        out.writeInt(port);
        out.writeString(description);
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

}
