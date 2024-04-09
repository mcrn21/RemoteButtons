package com.mcrn21.remotebuttons;

public class ConnectionParams {
    public ConnectionParams() {}
    public ConnectionParams(int baudRate, int dataBits, int parity, int stopBits) {
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
