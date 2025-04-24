package com.mk.securechat;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SockerReceiver extends Service {
    private static final String TAG = "SockerReceiver";
    private static final String SERVICE_NAME = "SecureChatApp";

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private final List<ReceiverThread> threads = new ArrayList<>();
    private boolean running = true;
    UdpBroadcaster udpBroadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        udpBroadcaster = new UdpBroadcaster(getApplicationContext());
        startServer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void startServer() {
        new Thread(() -> {  // Run in a separate thread
            try {
                udpBroadcaster.startBroadcasting();
                ServerSocket serverSocket = new ServerSocket(Utility.TCP_PORT);
                while (true) {
                    ServerStatus.getInstance().setServerRunning(true);
                    Log.d(TAG, "Server is listening...");
                    Socket socket = serverSocket.accept(); // Blocking call
                    if (socket != null) {
                        Log.d(TAG, "Accepted connection from " + socket.getRemoteSocketAddress().toString());
                        ReceiverThread thread = new ReceiverThread(socket, this);
                        threads.add(thread);
                        thread.start();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in server socket", e);
            }
        }).start(); // Running on a new thread
    }

    public void stopAllThreads() {
        running = false;
        for (ReceiverThread thread : threads) {
            thread.stopThread();
        }
        threads.clear();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAllThreads();
        udpBroadcaster.stopBroadcasting();
    }

    private void sendMessage(String message) {
        Log.d(TAG, "sendMessage: Messqge sent for the closing");
        Intent intent = new Intent("SERVICE_UPDATE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public SockerReceiver getService() {
            return SockerReceiver.this;
        }
    }
}
