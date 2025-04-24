package com.mk.securechat;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button send;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    DBHelper dbHelper;
    private List<Message> messages;
    private MessageAdapter adapter;
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
                if (!msg.getMessage().startsWith(Utility.ENCRYPTED)) {
                    File decreptedFile = Utility.decryptFile(this, msg.getPrivateKey(), msg.getFileUri());
                    Utility.openFileOrDownloads(this, decreptedFile);
                    msg.setFileUri(decreptedFile.getName());
                }
                Toast.makeText(this, "Message decrepted", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onCreate: decrypted", e);
            } finally {
                holder.update();
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
        loadingDialog.updateMessage("Starting Server");
        Intent intent = new Intent(this, SockerReceiver.class);
        startService(intent);
        new Thread(() -> {
            while (!ServerStatus.getInstance().isServerRunning()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                }
            }
            runOnUiThread(() -> {
                loadingDialog.dismiss();
            });

        }).start();
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
            if (message != null && message.equals("Server Error")) {
                loadingDialog.dismiss();
                Toast.makeText(MainActivity.this, "Error in starting server closing the application", Toast.LENGTH_SHORT).show();
                finish();
            } else loadingDialog.dismiss();
        }

    };

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
    protected void onDestroy() {
        super.onDestroy();
        // Clear adapter and detach RecyclerView
        if (adapter != null) {
            adapter.clear(); // Ensure you have a clear method in your adapter
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Prevent memory leaks
        }
        Intent intent = new Intent(this, SockerReceiver.class);
        stopService(intent);
    }

}