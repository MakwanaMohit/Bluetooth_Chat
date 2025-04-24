package com.mk.securechat;

public class ServerStatus {
    private static ServerStatus instance;
    private boolean isRunning = false;

    private ServerStatus() {}

    public static synchronized ServerStatus getInstance() {
        if (instance == null) {
            instance = new ServerStatus();
        }
        return instance;
    }

    public synchronized boolean isServerRunning() {
        return isRunning;
    }

    public synchronized void setServerRunning(boolean running) {
        isRunning = running;
    }
}
