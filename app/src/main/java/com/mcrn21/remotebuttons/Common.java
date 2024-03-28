package com.mcrn21.remotebuttons;

public class Common {
    static String APPLICATION_ID = "com.zsashka21.remotebuttons";
    static String SETTINGS_FILE = APPLICATION_ID + ".settings";

    // Intent actions
    static String INTENT_ACTION_GRANT_USB = APPLICATION_ID + ".GRANT_USB";
    static String INTENT_START_SERIAL = APPLICATION_ID + ".START_SERIAL";
    static String INTENT_SERIAL_CONNECTION_UPDATED = APPLICATION_ID + ".SERIAL_CONNECTION_UPDATED";
    static String INTENT_REMOTE_BUTTONS_COMMAND = APPLICATION_ID + ".REMOTE_BUTTONS_COMMAND";
    static String INTENT_ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    static String INTENT_ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    // Service
    static String NOTIFICATIONS_CHANNEL_ID = APPLICATION_ID + ".NOTIFICATIONS_CHANNEL";
    static int SERVICE_ID = 1108;
}
