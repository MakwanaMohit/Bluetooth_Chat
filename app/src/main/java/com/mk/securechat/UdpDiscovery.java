package com.mk.securechat;

import android.net.Uri;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class UdpDiscovery {
    private boolean isDiscovering = false;
    private Thread discoveryThread;
    private final int port = Utility.UDP_PORT;
    private final List<DiscoveredDevice> discoveredDevices = new ArrayList<>();

    public interface OnDeviceFoundListener {
        void onDeviceFound(DiscoveredDevice device);
    }

    public void startDiscovery(OnDeviceFoundListener listener) {
        isDiscovering = true;
        discoveryThread = new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(port);
                byte[] buffer = new byte[1024];

                while (isDiscovering) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());

                    try {
                        JSONObject json = new JSONObject(message);
                        String name = json.getString("device_name");
                        String ip = json.getString("ip");

                        DiscoveredDevice device = new DiscoveredDevice(name, ip);

                        boolean alreadyFound = false;
                        for (DiscoveredDevice d : discoveredDevices) {
                            if (d.getIp().equals(ip)) {
                                alreadyFound = true;
                                break;
                            }
                        }

                        if (!alreadyFound) {
                            discoveredDevices.add(device);
                            listener.onDeviceFound(device);
                        }

                    } catch (Exception ignored) {}
                }
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        discoveryThread.start();
    }

    public void stopDiscovery() {
        isDiscovering = false;
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
    }
    public List<DiscoveredDevice> getDevice(){
        return discoveredDevices;
    }
}
