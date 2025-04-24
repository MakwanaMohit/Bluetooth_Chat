package com.mk.securechat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper instance;

    private static final String DATABASE_NAME = "messages.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "Messages";

    private static final String COLUMN_MESSAGE_ID = "messageId";
    private static final String COLUMN_SENDER_NAME = "senderName";
    private static final String COLUMN_SENDER_ID = "senderId";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_FILE_URI = "fileuri";
    private static final String COLUMN_PRIVATE_KEY = "privateKey";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + COLUMN_MESSAGE_ID + " TEXT PRIMARY KEY, " + COLUMN_SENDER_NAME + " TEXT, " + COLUMN_SENDER_ID + " TEXT, " + COLUMN_MESSAGE + " TEXT, " + COLUMN_FILE_URI + " TEXT, " + COLUMN_PRIVATE_KEY + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public Message getMessageByIndex(int index) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_MESSAGE_ID + " DESC", index + ",1");
        if (cursor != null && cursor.moveToFirst()) {

            Message message = new Message(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME)), cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)), cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)),cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_URI)),cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIVATE_KEY)));
            message.setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)));
            cursor.close();
            return message;
        }
        return null;
    }

    public int getSize() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int size = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0); // Get the count from the first column
            }
            cursor.close();
        }
        return size;
    }


    public void removeMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (message != null) {
            db.delete(TABLE_NAME, COLUMN_MESSAGE_ID + " = ?", new String[]{message.getMessageId()});
        }
    }

    public void insertMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_ID, message.getMessageId());
        values.put(COLUMN_SENDER_NAME, message.getSenderName());
        values.put(COLUMN_SENDER_ID, message.getSenderId());
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_FILE_URI, message.getFileUri());
        values.put(COLUMN_PRIVATE_KEY, message.getPrivateKey());

        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_MESSAGE_ID + " DESC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Message message = new Message(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_NAME)), cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)), cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)),cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_URI)),cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIVATE_KEY)));
                message.setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)));
                messages.add(message);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return messages;
    }

    public void clearDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
