package com.mk.bluetoothchat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.PublicKey;
import java.util.Base64;


public class SendChat extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private MessageAdapter adapter;
    private BluetoothDevice device;
    private Button insertbtn;
    private EditText message;
    private RecyclerView recyclerView;
    private static String TAG = "SendChat";
    private ActivityResultLauncher<Intent> bluetoothEnableLauncher;
    private LoadingDialog loadingDialog;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 7;
    private BluetoothSocket socket;
    private PublicKey publickey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        insertbtn = findViewById(R.id.InsertMessage);
        message = findViewById(R.id.data);
        recyclerView = findViewById(R.id.SendrecyclerView);
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        loadingDialog = new LoadingDialog(this);
        adapter = new MessageAdapter(getApplicationContext(), null, holder -> {
            Message msg = holder.msg;
            try {
                String message = msg.getMessage();
                String cncrypted = Utility.encryptMessage(message, msg.getPrivateKey());
                msg.setMessage(Utility.ENCRYPTED + cncrypted);
                holder.update();
            } catch (Exception e) {
                Toast.makeText(this, "Error encrepting your message please try again", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        bluetoothEnableLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Bluetooth has been enabled
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                getDevice();
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
            }
        });
        checkBluetoothPermissions();

        insertbtn.setOnClickListener(view -> {

            try {
                String msg = message.getText().toString();
                if (msg == null | msg.length() < 1) {
                    return;
                }
                adapter.addMessage(new Message(device.getName(), device.getAddress(), msg, Utility.generateKey()));
                message.setText(null);
            } catch (Exception e) {
                Toast.makeText(this, "There is an error in inserting your messge please try again", Toast.LENGTH_SHORT).show();
            }

        });


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // No move operation
                return false;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                MessageHolder holder = (MessageHolder) viewHolder;
                int position = viewHolder.getAdapterPosition(); // Get the position before removal\
                Message msg = holder.msg;
                adapter.removeMessage(msg.getMessageId());
                try {
                    String message = msg.getMessage();
                    if (!message.startsWith(Utility.ENCRYPTED)) {
                        Toast.makeText(SendChat.this, "Please encrypt the data first by clicking on it", Toast.LENGTH_SHORT).show();
                        adapter.addMessage(msg, position);
                        return;
                    }

                    if (!getPublickey()) {
                        adapter.addMessage(holder.msg, position);
                        Toast.makeText(SendChat.this, "Error getting the public key please try again", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onSwiped: public key not found");
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Message", message);
                    jsonObject.put("PublicKey", Base64.getEncoder().encodeToString(publickey.getEncoded()));
                    jsonObject.put("AESKey", Utility.encryptKey(msg.getPrivateKey(), publickey));

                    if (sendMessage(jsonObject.toString())) {
                        String response = getMessage();
                        if (response != null && response.equals(Utility.REQUEST_OK)) {
                            Toast.makeText(SendChat.this, "Message sent successfully", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "onSwiped: Message has been sent sucessfully");
                        } else {
                            sendMessage(Utility.STOP_CONNECTION);
                            Toast.makeText(SendChat.this, "Error in sending message! Please try again after reconnecting.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "onSwiped: there is an error sending the message ");
                            getDevice();
                            adapter.addMessage(msg, position); // Restore message if failed
                        }
                    } else {
                        sendMessage(Utility.STOP_CONNECTION);
                        Toast.makeText(SendChat.this, "Error in sending message! Please try again after reconnecting.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onSwiped: there is an error sending the message ");
                        getDevice();
                        adapter.addMessage(msg, position); // Restore message if failed
                    }

                } catch (Exception e) {
                    Log.e(TAG, "onSwiped: send error", e);
                    sendMessage(Utility.STOP_CONNECTION);
                    Toast.makeText(SendChat.this, "Error in sending message! Please try again after reconnecting.", Toast.LENGTH_SHORT).show();
                    getDevice();
                    adapter.addMessage(msg, position); // Restore message if failed
                }


            }
        }).attachToRecyclerView(recyclerView);
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                getAdapter();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                getAdapter();
            }
        } else {
            getAdapter();
        }
    }

    private void getDevice() {
        loadingDialog.show("Connecting to Device");
        closeSocket();
        loadingDialog.updateMessage("Connecting to Device");
        publickey = null;
        String devicemac = getIntent().getStringExtra("mac");
        device = bluetoothAdapter.getRemoteDevice(devicemac);

        new Thread(() -> {
            try {
                runOnUiThread(() -> {
                    loadingDialog.updateMessage("Connecting to Device");
                });
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
                socket = device.createRfcommSocketToServiceRecord(Utility.MY_UUID);
                socket.connect();
                Log.d(TAG, "Connected to " + device.getName());

                runOnUiThread(() -> {
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                    loadingDialog.updateMessage("Connected");
                    loadingDialog.dismiss();
                });

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                    });
                }, 1000);

            } catch (IOException e) {

                runOnUiThread(() -> {
                    loadingDialog.updateMessage("Error in Connection");
                    loadingDialog.dismiss();
                });
                Log.e(TAG, "getDevice: error in connection ", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
                });
                closeSocket();
                finish();
            }
        }).start();

    }

    private boolean getPublickey() {
        if (publickey != null) return true;
        try {
            if (!sendMessage(Utility.REQUEST_PUBLIC_KEY)) return false;
            String pubKeyString = getMessage();
            JSONObject parsedJson = new JSONObject(pubKeyString);
            if (parsedJson.optString("PublicKey", null) != null) {
                pubKeyString = parsedJson.optString("PublicKey", null);
                if (pubKeyString == null) return false;
                publickey = Utility.getPublicKeyFromString(pubKeyString);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendMessage(String message) {
        if (socket == null | !socket.isConnected()) return false;
        try {
            OutputStreamWriter outputWriter = new OutputStreamWriter(socket.getOutputStream());
            outputWriter.write(message + "\n");
            outputWriter.flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getMessage() {
        if (socket == null | !socket.isConnected()) return null;
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return inputReader.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private void getAdapter() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled, prompt the user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableLauncher.launch(enableBtIntent);
        } else {
            getDevice();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAdapter();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void closeSocket() {
        loadingDialog.updateMessage("Closing The Old Socket");
        // Close Bluetooth socket if open
        if (socket != null) {
            try {
                sendMessage(Utility.STOP_CONNECTION);
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Bluetooth socket", e);
            }
        }
        // Nullify resources that are no longer needed
        publickey = null;
        socket = null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeSocket();
        // Clear adapter and detach RecyclerView
        if (adapter != null) {
            adapter.clear(); // Ensure you have a clear method in your adapter
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Prevent memory leaks
        }

        // Nullify resources that are no longer needed
        publickey = null;
        socket = null;
    }
}