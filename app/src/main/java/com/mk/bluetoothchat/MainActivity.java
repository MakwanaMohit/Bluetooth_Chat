package com.mk.bluetoothchat;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    Button send;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    DBHelper dbHelper;
    private List<Message> messages;
    private MessageAdapter adapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private ActivityResultLauncher<Intent> bluetoothEnableLauncher;
    private final String TAG = "MainActivity";
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(serviceMessageReceiver, new IntentFilter("SERVICE_UPDATE"));
        loadingDialog = new LoadingDialog(this);

        dbHelper = DBHelper.getInstance(MainActivity.this);
        send = findViewById(R.id.SendMessage);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messages = dbHelper.getAllMessages();
        adapter = new MessageAdapter(this, messages, holder -> {

            try {
                Message msg = holder.msg;
                msg.setMessage(Utility.decryptMessage(msg.getMessage(), msg.getPrivateKey()));
                holder.update();
                Toast.makeText(this, "Message decrepted", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: decrypted", e);
            }
        });

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothEnableLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Bluetooth has been enabled
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), BluetoothSockerReceiver.class);
                loadingDialog.updateMessage("Starting Server");
                startService(intent);
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Attach ItemTouchHelper
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // No move operation
                return false;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                try {
                    int position = viewHolder.getAdapterPosition();
                    MessageHolder holder = (MessageHolder) viewHolder;
                    dbHelper.removeMessage(holder.msg);
                    adapter.removeMessage(holder.msg.getMessageId());
                } catch (Exception e) {
                    Log.e(TAG, "onSwiped: ", e);
                }


            }
        }).attachToRecyclerView(recyclerView);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshAdapter();
            swipeRefreshLayout.setRefreshing(false);
        });
        send.setOnClickListener((View v) -> {
            Intent intent = new Intent(this, SendActivity.class);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        loadingDialog.show("initializing things!");
        checkBluetoothPermissions();

    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshAdapter();
    }


    private final BroadcastReceiver serviceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "onReceive: message received for closing: " + message);
            if (message != null) {
                if (message.equals("Server Started")) {
                    loadingDialog.dismiss();
                } else if (message.equals("Server Error")) {
                    loadingDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Error in starting server closing the application", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    // Function to check and request Bluetooth permissions
    private void checkBluetoothPermissions() {
        loadingDialog.updateMessage("Checking Permissions");
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

    private void getAdapter() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        } else if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled, prompt the user to enable it
            loadingDialog.updateMessage("Enabling Bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableLauncher.launch(enableBtIntent);
        } else {
            loadingDialog.updateMessage("Starting Server");
            Intent intent = new Intent(this, BluetoothSockerReceiver.class);
            startService(intent);


        }
    }

    private void refreshAdapter() {
        int adaptersize = adapter.messages.size();
        for (int i = 0; i < dbHelper.getSize(); i++) {
            Message newMessage = dbHelper.getMessageByIndex(i);
            if (i < adaptersize) {
                Message oldMessage = adapter.messages.get(i);

                // Check if the new and old messages are the same using equals
                if (!newMessage.equals(oldMessage)) {
                    adapter.updateMessageByIndex(i, newMessage);
                }
            } else {
                // Old index is over, add the new message
                adapter.addMessage(newMessage);
            }
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
                loadingDialog.dismiss();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ;
        // Clear adapter and detach RecyclerView
        if (adapter != null) {
            adapter.clear(); // Ensure you have a clear method in your adapter
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Prevent memory leaks
        }
        Intent intent = new Intent(this, BluetoothSockerReceiver.class);
        stopService(intent);
    }

}