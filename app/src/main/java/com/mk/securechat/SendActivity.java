package com.mk.securechat;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.Set;


public class SendActivity extends AppCompatActivity {
    private final static String RSA = "RSA";
    private ArrayList<String> deviceList;
    private ArrayList<String> deviceIP;
    private ListView listView;
    private final String TAG = "MultiConnectionSender";
    private UdpDiscovery udpDiscovery;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        listView = findViewById(R.id.pairedDevice);
        deviceList = new ArrayList<>();
        deviceIP = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the MAC address of the clicked device
                String fileuristring = null;
                Intent intent = getIntent();
                String action = intent.getAction();
                String type = intent.getType();

                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (fileUri != null) {
                        fileuristring = fileUri.toString();
                    }
                }

                String ipAddress = deviceIP.get(position);
                intent = new Intent(getApplicationContext(), SendChat.class);
                intent.putExtra("ip", ipAddress);
                intent.putExtra("fileUri", fileuristring);
                startActivity(intent);

            }
        });
        getPairedDevices();


    }


    private void getPairedDevices() {
        udpDiscovery = new UdpDiscovery();
        udpDiscovery.startDiscovery(device -> {
            runOnUiThread(() -> {
                String display = device.getDeviceName() + "\n" + device.getIp();

                if (!deviceList.contains(display)) {
                    deviceIP.add(device.getIp());
                    deviceList.add(display);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpDiscovery.stopDiscovery();
    }
}