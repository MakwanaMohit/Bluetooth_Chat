package com.mk.bluetoothchat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class ReceiverThread extends Thread {
    private static final String TAG = "ReceiverThread";
    private final BluetoothSocket socket;
    private Context context;
    private DBHelper dbHelper;
    private final BluetoothDevice device;
    private final BufferedReader inputReader;
    private final OutputStreamWriter outputWriter;

    private String publicKey;
    private PrivateKey privateKey;
    private boolean running = true;

    public ReceiverThread(BluetoothSocket socket, Context context) throws IOException {
        this.socket = socket;
        this.context = context.getApplicationContext();
        this.dbHelper = DBHelper.getInstance(context);
        this.device = socket.getRemoteDevice();
        this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.outputWriter = new OutputStreamWriter(socket.getOutputStream());
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            this.publicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            this.privateKey = keyPair.getPrivate();
            Log.d(TAG, "Key pair generated for socket: " + socket.getRemoteDevice().getName());
        } catch (Exception e) {
            Log.e(TAG, "Error generating key pair", e);
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                String message = inputReader.readLine();
                if (message == null) continue;

                Log.d(TAG, "Received message: " + message);

                if (message.equals(Utility.REQUEST_PUBLIC_KEY)) {
                    sendPublicKey();
                } else if (message.equals(Utility.STOP_CONNECTION)) {
                    stopThread();
                } else {
                    insertMessage(message);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error in communication", e);
        } finally {
            closeResources();
        }
    }

    private void sendPublicKey() throws IOException, JSONException {
        // Create JSON object
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("PublicKey", this.publicKey);

        outputWriter.write(jsonObject.toString() + "\n");
        outputWriter.flush();
        Log.d(TAG, "Public key sent to " + socket.getRemoteDevice().getName());
    }

    private void insertMessage(String encryptedMessage) {
        try {
            JSONObject data = new JSONObject(encryptedMessage);
            String pubkey = data.optString("PublicKey");
            String messsage = data.optString("Message");
            String cipherKey = data.optString("AESKey", null);
            String AESKey = Utility.decryptKey(cipherKey, privateKey);

            if (pubkey != null && cipherKey != null && AESKey != null && messsage != null && this.publicKey.equals(pubkey)) {
                dbHelper.insertMessage(new Message(device.getName(), device.getAddress(), messsage, AESKey));
                outputWriter.write(Utility.REQUEST_OK + "\n");
                outputWriter.flush();
                Log.d(TAG, "insertMessage: message has been received sucessfully and ok response transfered");
            } else {
                outputWriter.write(Utility.REQUEST_ERR + "\n");
                outputWriter.flush();
                Log.d(TAG, "insertMessage: Condition failed" + (pubkey != null) + (cipherKey != null) + (AESKey != null) + (messsage != null) + (this.publicKey.equals(pubkey)));
            }
        } catch (Exception e) {
            Log.e(TAG, "insertMessage: ", e);
            try {
                outputWriter.write(Utility.REQUEST_ERR + "\n");
                outputWriter.flush();
            } catch (IOException ex) {
            }
        }
    }

    void stopThread() {
        running = false;
        Log.d(TAG, "Stopping thread for socket: " + socket.getRemoteDevice().getName());
        closeResources();
    }

    private void closeResources() {
        try {
            inputReader.close();
            outputWriter.close();
            socket.close();
            Log.d(TAG, "Resources closed for socket: " + socket.getRemoteDevice().getName());
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources", e);
        }
    }
}

