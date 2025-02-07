package com.mk.bluetoothchat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageHolder> {
    Context context;
    List<Message> messages;
    MessageListner listner;

    public MessageAdapter(Context context, List<Message> messages, MessageListner listner) {
        this.context = context;
        this.listner = listner;
        this.messages = messages;
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageHolder(LayoutInflater.from(context).inflate(R.layout.recyclerview, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
        Message msg = messages.get(position);
        holder.bind(msg, listner);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        if (getMessagePositionByKey(message.getMessageId()) == -1) {
            messages.add(message);
            notifyItemInserted(messages.size() - 1);
        }
    }

    public void addMessage(Message msg, int position) {
        int pos = getMessagePositionByKey(msg.getMessageId());
        if (pos == -1) {
            messages.add(position, msg);
            notifyItemInserted(position);
        }
    }

    public void updateMessage(Message message) {
        int position = getMessagePositionByKey(message.getMessageId());
        if (position != -1) {
            messages.set(position, message);
            notifyItemChanged(position);
        }
    }

    public void updateMessageByIndex(int index, Message message) {
        messages.set(index, message);
        notifyItemChanged(index);
    }

    public void removeMessage(String messageid) {
        int position = getMessagePositionByKey(messageid);
        if (position != -1) {
            messages.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        messages.clear();  // Clear all messages
        notifyDataSetChanged();  // Refresh UI
    }

    private int getMessagePositionByKey(String messageid) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId().equals(messageid)) {
                return i;
            }
        }
        return -1;
    }
}
