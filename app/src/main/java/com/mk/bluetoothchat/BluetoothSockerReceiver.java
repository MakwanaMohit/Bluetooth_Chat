package com.mk.bluetoothchat;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BluetoothSockerReceiver extends Service {
    private static final String TAG = "BluetoothSockerReceiver";
    private static final String SERVICE_NAME = "BluetoothChatApp";

    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;
    private final List<ReceiverThread> threads = new ArrayList<>();
    private boolean running = true;

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        startServer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void startServer() {
        new Thread(() -> {  // Run in a separate thread
            try {

                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, Utility.MY_UUID);

                while (true) {
                    Log.d(TAG, "Server is listening...");
                    sendMessage("Server Started");
                    BluetoothSocket socket = serverSocket.accept(); // Blocking call
                    if (socket != null) {
                        Log.d(TAG, "Accepted connection from " + socket.getRemoteDevice().getName());
                        ReceiverThread thread = new ReceiverThread(socket, this);
                        threads.add(thread);
                        thread.start();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error in server socket", e);
                sendMessage("Server Error");
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
    }

    private void sendMessage(String message) {
        Log.d(TAG, "sendMessage: Messqge sent for the closing");
        Intent intent = new Intent("SERVICE_UPDATE");
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothSockerReceiver getService() {
            return BluetoothSockerReceiver.this;
        }
    }
}
