package com.mk.securechat;

import android.content.ContentResolver;
import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class Utility {
    public static final String REQUEST_PUBLIC_KEY = "111111111111111111111111111111111111111111111111111111asdfghjkl;";
    public static final String STOP_CONNECTION = "asdfghjkl;222222222222222222222222222222222222222222222222222222";
    public static final String REQUEST_OK = "REQUEST_OK_RESPONSE";
    public static final String REQUEST_RECEIVE = "333333333333333333333333333333333RECEIEVERMESSEGECODE";
    public static final String REQUEST_ERR = "REQUEST_ERROR_OCCURED";
    public static final int UDP_PORT = 43567;
    public static final int TCP_PORT = 43578;
    public static final String MY_UUID = "RSA";
    public static final String RSA = "RSA";
    public static final String ENCRYPTED = "<ENCRYPTED>";
    public static final String DEVICE_NAME = "device_name_sent:";
    public static final int BUFFER_SIZE = 8192;
    private static final String TAG = "Utility";


    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        // Decode the base64 encoded public key string
        byte[] decodedKey = Base64.getDecoder().decode(key);

        // Generate a public key using the X509EncodedKeySpec
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public static String decryptKey(String cipherKey, PrivateKey privateKey) {
        try {
            Cipher decryptCipher = Cipher.getInstance(Utility.RSA);
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(cipherKey));
            return new String(decryptedBytes);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encryptKey(String plainKey, PublicKey publicKey) {
        try {
            Cipher encryptCipher = Cipher.getInstance(RSA);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = encryptCipher.doFinal(plainKey.getBytes());
            String encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);
            return encryptedMessage;
        } catch (Exception e) {
            return null;
        }
    }

    public static String encryptMessage(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Method to decrypt an encrypted text message using the AES key
    public static String decryptMessage(String encryptedText, String key) throws Exception {
        if (!encryptedText.startsWith(ENCRYPTED))
            throw new Exception("Message is already decrypted");
        encryptedText = encryptedText.substring(ENCRYPTED.length());
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }


    /**
     * Encrypts a file from a URI using AES and saves it in the app's private storage.
     *
     * @param context  Application context
     * @param key   AES key (16, 24, or 32 bytes)
     * @param fileUri  File URI (e.g., from Storage Access Framework)
     * @return Encrypted file object (stored in private storage)
     * @throws Exception If an error occurs during encryption
     */
    public static File encryptFile(Context context, String key, Uri fileUri) throws Exception {
        ContentResolver resolver = context.getContentResolver();

        // Open the input stream from the URI
        InputStream inputStream = resolver.openInputStream(fileUri);
        if (inputStream == null) {
            throw new FileNotFoundException("Cannot open file: " + fileUri);
        }

        // Get the original file name (if available)
        String fileName = "encrypted_file.enc";
        Cursor cursor = ((ContentResolver) resolver).query(fileUri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex) + ".enc";
            }
            cursor.close();
        }

        // Store encrypted file privately in app's storage
        File encryptedFile = new File(context.getFilesDir(), fileName);
        if (encryptedFile.exists()){
            encryptedFile.delete();
        }
        // Setup AES cipher
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encrypt file in chunks
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             FileOutputStream fos = new FileOutputStream(encryptedFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] buffer = new byte[131072]; // 8KB Buffer
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        return encryptedFile;
    }

    public static File decryptFile(Context context, String key, String encryptedFilePath) throws Exception {
        File encryptedFile = new File(encryptedFilePath);
        if (!encryptedFile.exists()) {
            throw new FileNotFoundException("File not found: " + encryptedFilePath);
        }

        // Define public storage directory (e.g., Downloads folder)
        File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!publicDir.exists()) {
            publicDir.mkdirs();
        }

        // Extract original file name and extension properly
        String originalFileName = encryptedFile.getName();
        if (originalFileName.endsWith(".enc")) {
            originalFileName = "hello"+originalFileName.substring(0, originalFileName.length() - 4); // Remove ".enc"
        }

        File decryptedFile = new File(publicDir, originalFileName);

        // Setup AES cipher for decryption
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt file in chunks
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             CipherInputStream cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream(decryptedFile)) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        return decryptedFile;
    }



    // Method to generate a random AES key and return it as a base64 encoded string
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    public static void openFileOrDownloads(Context context, File file) throws Exception {
        try {
            Uri fileUri = getUriForFile(context, file);
            String mimeType = context.getContentResolver().getType(fileUri);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // No app found, open Downloads folder instead
                openDownloadsFolder(context);
            }
        } catch (Exception e) {
            Log.e("FileOpener", "Error opening file", e);
            Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show();
            openDownloadsFolder(context);
        }
    }

    private static Uri getUriForFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private static void openDownloadsFolder(Context context) throws Exception{
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri downloadsUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        intent.setDataAndType(downloadsUri, "resource/folder");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("FileOpener", "No file manager found", e);
            throw e;
        }
    }

}
