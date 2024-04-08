package com.mcrn21.remotebuttons;

public class LaunchApp {
    public LaunchApp() {}
    public LaunchApp(String packageName, boolean enable) {
        this.packageName = packageName;
        this.enable = enable;
    }
    String packageName = "";
    boolean enable = false;
}
