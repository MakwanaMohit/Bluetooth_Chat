package com.mk.securechat;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class UdpBroadcaster {
    private boolean isBroadcasting = false;
    private Thread broadcastThread;
    private final Context context;
    private final int port = Utility.UDP_PORT;

    public UdpBroadcaster(Context context) {
        this.context = context;
    }

    public void startBroadcasting() {
        isBroadcasting = true;
        broadcastThread = new Thread(() -> {
            try {
                // Get SSID as device name
                String deviceName = Build.MANUFACTURER + " : " + Build.MODEL;

                JSONObject json = new JSONObject();
                Pair<String,InetAddress> ip = getIpAndBroadcastFromInterfaces();

                json.put("device_name", deviceName);
                json.put("ip", ip.first);
                byte[] data = json.toString().getBytes();

                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(data, data.length, ip.second, port);

                while (isBroadcasting) {
                    socket.send(packet);
                    Thread.sleep(1200);
                }

                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        broadcastThread.start();
    }

    public void stopBroadcasting() {
        isBroadcasting = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    // Reverse IP bytes because Android gives IP in little-endian
    private byte[] reverseBytes(byte[] input) {
        byte[] reversed = new byte[4];
        for (int i = 0; i < 4; i++) {
            reversed[i] = input[3 - i];
        }
        return reversed;
    }


    private Pair<String, InetAddress> getIpAndBroadcastFromInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;

                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    InetAddress inetAddress = address.getAddress();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        String ip = inetAddress.getHostAddress();
                        InetAddress broadcast = address.getBroadcast();
                        if (ip != null && broadcast != null) {
                            Log.d("hello", "startBroadcasting: "+ip+" brod: "+broadcast.getHostAddress());

                            return new Pair<>(ip, broadcast);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Pair<>(null, null);
    }


}
