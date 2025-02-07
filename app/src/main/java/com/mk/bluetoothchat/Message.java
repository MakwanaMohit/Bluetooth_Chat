package com.mk.bluetoothchat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {


    private String messageId;
    private String senderName;
    private String senderId;
    private String message;
    private String privateKey;

    // Default constructor
    public Message() {
    }

    // Constructor with all attributes
    public Message(String senderName, String senderId, String message, String privateKey) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmmssSSS");
        this.messageId = sdf.format(new Date());
        this.senderName = senderName;
        this.senderId = senderId;
        this.message = message;
        this.privateKey = privateKey;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getDateTime() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("ddMMyyHHmmssSSS");
            Date date = inputFormat.parse(getMessageId());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
