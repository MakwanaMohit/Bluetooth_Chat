package com.mk.securechat;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageHolder extends RecyclerView.ViewHolder {
    private static final String TAG = "MessageHolder";
    TextView deviceid, datetime, message,fileUri, privatekey;
    Message msg;
    View mainView;

    public MessageHolder(@NonNull View itemView) {
        super(itemView);
        deviceid = itemView.findViewById(R.id.Msgdeviceid);
        datetime = itemView.findViewById(R.id.Msgdatetime);
        message = itemView.findViewById(R.id.Msgmessage);
        fileUri = itemView.findViewById(R.id.MsgUri);
        privatekey = itemView.findViewById(R.id.Msgprivatekey);
        mainView = itemView;
    }

    public void bind(@NonNull Message msg, @NonNull MessageListner listner) {
        try {
            this.msg = msg;
            update();
            mainView.setOnClickListener(view -> {
                listner.onItemClick(this);
            });
        } catch (Exception e) {
            Log.e(TAG, "bind: error", e);
        }
    }

    public void update() {
        try {
            deviceid.setText(msg.getSenderName() + " | " + msg.getSenderId());
            datetime.setText(msg.getDateTime());
            privatekey.setText((msg.getPrivateKey().length() > 80) ? msg.getPrivateKey().substring(0, 80) : msg.getPrivateKey());
            message.setText(msg.getMessage());
            fileUri.setText(msg.getFileUri());
        } catch (Exception e) {
            Log.e(TAG, "update: error", e);
        }
    }
}
