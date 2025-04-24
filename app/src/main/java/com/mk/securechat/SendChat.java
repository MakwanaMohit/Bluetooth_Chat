package com.mk.securechat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;


public class SendChat extends AppCompatActivity {
    private MessageAdapter adapter;
    private Button insertbtn, fileUriView;
    private EditText message;
    private Socket socket;
    private RecyclerView recyclerView;
    private static String TAG = "SendChat";
    private ActivityResultLauncher<Intent> fileSelectLauncher;
    private LoadingDialog loadingDialog;
    private PublicKey publickey;
    private String ip;
    Uri fileUri;
    private String deviceName = Build.MANUFACTURER + " : " + Build.MODEL;


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
        fileUriView = findViewById(R.id.fileUri);
        insertbtn = findViewById(R.id.InsertMessage);
        message = findViewById(R.id.data);
        recyclerView = findViewById(R.id.SendrecyclerView);
        loadingDialog = new LoadingDialog(this);
        String uriString = getIntent().getStringExtra("fileUri");
        ip = getIntent().getStringExtra("ip");
        if (uriString != null) {
            fileUri = Uri.parse(uriString);
            fileUriView.setText(getFileName(fileUri));
        }

        fileUriView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");  // Accept any file type

            fileSelectLauncher.launch(intent);
        });

        adapter = new MessageAdapter(getApplicationContext(), null, holder -> {
            Message msg = holder.msg;
            new Thread(() -> {
                try {
                    if (!msg.getMessage().startsWith(Utility.ENCRYPTED)) {
                        runOnUiThread(() -> {
                            loadingDialog.show("message is encryption");
                        });
                        if (!msg.getFileUri().equals("file not added")) {
                            File f = Utility.encryptFile(this, msg.getPrivateKey(), Uri.parse(msg.getFileUri()));
                            msg.setFileUri(f.getAbsolutePath());
                        }
                    }
                    String message = msg.getMessage();
                    String encrypted = Utility.encryptMessage(message, msg.getPrivateKey());
                    msg.setMessage(Utility.ENCRYPTED + encrypted);

                    runOnUiThread(() -> {
                        holder.update();
                        loadingDialog.dismiss();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error encrypting your message please try again", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fileSelectLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                fileUri = result.getData().getData();
                if (fileUri != null) {
                    fileUriView.setText(getFileName(fileUri));
                }
            }
        });


        insertbtn.setOnClickListener(view -> {
            try {
                String msg = message.getText().toString();
                if (msg == null | msg.length() < 1) {
                    return;
                }
                String fileuri = "file not added";
                if (fileUri != null) {
                    fileuri = fileUri.toString();
                }
                adapter.addMessage(new Message(deviceName, ip, msg, fileuri, Utility.generateKey()));
                message.setText(null);
                fileUriView.setText("Click to add file");
                fileuri = null;
            } catch (Exception e) {
                Toast.makeText(this, "There is an error in inserting your message please try again", Toast.LENGTH_SHORT).show();
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
                String message = msg.getMessage();
                if (!message.startsWith(Utility.ENCRYPTED)) {
                    Toast.makeText(SendChat.this, "Please encrypt the data first by clicking on it", Toast.LENGTH_SHORT).show();
                    adapter.addMessage(msg, position);
                    return;
                }

                new Thread(() -> {
                    try {
                        if (!getPublickey()) {
                            adapter.addMessage(holder.msg, position);
                            toast("Error getting the public key please try again");
                            Log.d(TAG, "onSwiped: public key not found");
                            adapter.addMessage(msg, position);
                            return;
                        }
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("Message", message);
                        jsonObject.put("PublicKey", Base64.getEncoder().encodeToString(publickey.getEncoded()));
                        jsonObject.put("AESKey", Utility.encryptKey(msg.getPrivateKey(), publickey));
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            loadingDialog.show("Sending Message");
                        });

                        boolean sent = true;
                        try {
                            if (!msg.getFileUri().equals("file not added")) {
                                sent = sendfile(msg, jsonObject);
                            }
                        } catch (Exception e) {
                            toast("some error occur in file sending");
                            Log.e(TAG, "onSwiped: ", e);
                        }
                        boolean finalSent = sent;
                        if (finalSent) {
                            sendmsg(jsonObject, msg, position);
                            Log.d(TAG, "onSwiped: sending the messege");
                        } else {
                            runOnUiThread(() -> {
                                loadingDialog.dismiss();
                            });
                            toast("Error in sending message! Please try again after reconnecting.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onSwiped: send error", e);
                        sendMessage(Utility.STOP_CONNECTION);
                        toast("Error in sending message! Please try again after reconnecting.");
                        getDevice();
                        adapter.addMessage(msg, position); // Restore message if failed
                    }

                }).start();

            }
        }).attachToRecyclerView(recyclerView);

        getDevice();

    }


    private void sendmsg(JSONObject jsonObject, Message msg, int position) {
        if (sendMessage(jsonObject.toString())) {
            String response = getMessage();
            if (response != null && response.equals(Utility.REQUEST_OK)) {
                Log.d(TAG, "onSwiped: Message has been sent sucessfully");
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                });
                toast("Message sent successfully");
            } else {
                sendMessage(Utility.STOP_CONNECTION);
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                });
                toast("Error in sending message! Please try again after reconnecting.");
                Log.d(TAG, "onSwiped: there is an error sending the message ");
                getDevice();
                adapter.addMessage(msg, position); // Restore message if failed
            }
        } else {
            sendMessage(Utility.STOP_CONNECTION);
            toast("Error in sending message! Please try again after reconnecting.");
            Log.d(TAG, "onSwiped: there is an error sending the message ");
            getDevice();
            adapter.addMessage(msg, position); // Restore message if failed
            runOnUiThread(() -> {
                loadingDialog.dismiss();
            });
        }
    }


    private void getDevice() {
        loadingDialog.show("Connecting to Device");
        closeSocket();
        loadingDialog.updateMessage("Connecting to Device");
        publickey = null;

        new Thread(() -> {
            try {
                runOnUiThread(() -> {
                    loadingDialog.updateMessage("Connecting to Device");
                });
                socket = new Socket(ip, Utility.TCP_PORT);
                Log.d(TAG, "Connected to " + deviceName);
                sendMessage(Utility.DEVICE_NAME + deviceName);
                int a = 1;

                runOnUiThread(() -> {
                    Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    loadingDialog.updateMessage("Connected");
                    loadingDialog.dismiss();
                });

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    runOnUiThread(() -> {
                        loadingDialog.dismiss();
                    });
                }, 1000);

            } catch (Exception e) {

                runOnUiThread(() -> {
                    loadingDialog.updateMessage("Error in Connection");
                    loadingDialog.dismiss();
                });
                Log.e(TAG, "getDevice: error in connection ", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error connecting to " + deviceName, Toast.LENGTH_SHORT).show();
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

    private boolean sendfile(Message msg, JSONObject jsonObject) {
        File file = new File(msg.getFileUri());
        if (socket == null | !socket.isConnected()) return false;
        try {
            if (!(sendMessage(Utility.REQUEST_RECEIVE) && sendMessage(msg.getMessageId() + "=" + file.getName())))
                return false;
            // Bluetooth OutputStream
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            // Send file size first
            long fileSize = file.length();
            long bytes_sent = 0;
            dataOutputStream.writeLong(fileSize);
            dataOutputStream.flush(); // Ensure file size is sent properly

            Log.d(TAG, "Sending file of size: " + fileSize + " bytes");

            // Read and send the file in chunks
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            byte[] buffer = new byte[Utility.BUFFER_SIZE]; // 8KB buffer
            int bytesRead;
            int i = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
                bytes_sent += bytesRead;
                int percent = (int) ((int) bytes_sent * 100 / fileSize);
                runOnUiThread(() -> {
                    loadingDialog.updateMessage("Sendinng Message: " + percent + "%");
                    Log.d(TAG, "sendfile: dismiss called dismiss called");
                });
                Log.d(TAG, "sendfile: percent: " + percent + "i: " + i);
                i++;
            }
            dataOutputStream.flush(); // Ensure all data is sent
            Thread.sleep(500);
            Log.d(TAG, "sendfile: data sent sucessfully");
            String response = getMessage();
            if (response == null | !response.startsWith(Utility.REQUEST_OK)) return false;
            jsonObject.put("FileUri", response.substring(Utility.REQUEST_OK.length()));
            file.delete();
            Log.d(TAG, "sendfile: file uri data received suecessfully");
            Thread.sleep(500);
            runOnUiThread(() -> {
                loadingDialog.dismiss();
                Log.d(TAG, "sendfile: hello this disable is called hello hii");
            });
            return true;

        } catch (Exception e) {
            Log.e(TAG, "send file: error", e);
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

    private void toast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void closeSocket() {
        loadingDialog.updateMessage("Closing The Old Socket");
        // Close Bluetooth socket if open
        if (socket != null) {
            try {
                sendMessage(Utility.STOP_CONNECTION);
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
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

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }

        // If the file scheme is "file", get the last segment from the path
        if (result == null) {
            result = uri.getLastPathSegment();
        }

        return result;
    }

}