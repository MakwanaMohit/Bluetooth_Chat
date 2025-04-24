package com.mk.securechat;

public class DiscoveredDevice {
    private final String deviceName;
    private final String ip;

    public DiscoveredDevice(String deviceName, String ip) {
        this.deviceName = deviceName;
        this.ip = ip;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getIp() {
        return ip;
    }
}
