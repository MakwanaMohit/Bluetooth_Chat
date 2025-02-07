package com.mk.bluetoothchat;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Utility {
    public static final String REQUEST_PUBLIC_KEY = "111111111111111111111111111111111111111111111111111111asdfghjkl;";
    public static final String STOP_CONNECTION = "asdfghjkl;222222222222222222222222222222222222222222222222222222";
    public static final String REQUEST_OK = "2O0";
    public static final String REQUEST_ERR = "443";
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String RSA = "RSA";
    public static final String ENCRYPTED = "<ENCRYPTED>";
    private static final String TAG = "Utility";


    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        // Decode the base64 encoded public key string
        byte[] decodedKey = Base64.getDecoder().decode(key);

        // Generate a public key using the X509EncodedKeySpec
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public static String decryptKey(String cipherKey, PrivateKey privateKey){
        try {
            Cipher decryptCipher = Cipher.getInstance(Utility.RSA);
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(cipherKey));
            return new String(decryptedBytes);
        } catch (Exception e) {
            return null;
        }
    }
    public static String encryptKey(String plainKey, PublicKey publicKey){
        try {
            Cipher encryptCipher = Cipher.getInstance(RSA);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = encryptCipher.doFinal(plainKey.getBytes());
            String encryptedMessage = Base64.getEncoder().encodeToString(encryptedBytes);
            return encryptedMessage;
        }catch (Exception e){
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
        if (!encryptedText.startsWith(ENCRYPTED)) throw new Exception("Message is already decrypted");
        encryptedText = encryptedText.substring(ENCRYPTED.length());
        SecretKeySpec secretKey = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }

    // Method to generate a random AES key and return it as a base64 encoded string
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

}
