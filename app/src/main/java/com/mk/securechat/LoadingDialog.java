package com.mk.securechat;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

public class LoadingDialog {
    private Dialog dialog;
    private TextView messageTextView;
    private final String TAG = "LoadingDialog";

    public LoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_loding);
        dialog.setCancelable(false); // Prevents user from closing it manually

        messageTextView = dialog.findViewById(R.id.loadingMessage);
    }

    public void show(String message) {
        messageTextView.setText(message);
        dialog.show();
    }

    public void updateMessage(String message) {
        messageTextView.setText(message);
    }

    public void dismiss() {
        Log.d(TAG, "dismiss: get closed");
        dialog.dismiss();
    }
}
