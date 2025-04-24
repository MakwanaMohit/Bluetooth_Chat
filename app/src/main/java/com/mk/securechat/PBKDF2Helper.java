package com.mk.securechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PBKDF2Helper {
    private static final String PREF_NAME = "SecurePrefs";
    private static final String PASSWORD_KEY = "hashed_password";
    private static final String SALT_KEY = "salt";
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256; // 256-bit key

    // Generate Salt
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 16 bytes salt
        random.nextBytes(salt);
        return salt;
    }

    // Hash Password
    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.DEFAULT);
    }

    // Save Hashed Password & Salt
    public static void savePassword(Context context, String password) {
        try {
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password.toCharArray(), salt);

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PASSWORD_KEY, hashedPassword);
            editor.putString(SALT_KEY, Base64.encodeToString(salt, Base64.DEFAULT));
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean verifyPassword(Context context, String enteredPassword) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String storedHash = prefs.getString(PASSWORD_KEY, null);
            String storedSalt = prefs.getString(SALT_KEY, null);

            if (storedHash == null || storedSalt == null) return false;

            byte[] salt = Base64.decode(storedSalt.trim(), Base64.DEFAULT); // Trim salt
            String enteredHash = hashPassword(enteredPassword.toCharArray(), salt);

            // Compare after trimming potential whitespace issues
            return storedHash.trim().equals(enteredHash.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Clear Stored Password
    public static void clearPassword(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PASSWORD_KEY);
        editor.remove(SALT_KEY);
        editor.apply();
    }


    // Reset Password
    public static void resetPassword(Context context, String newPassword) {
        savePassword(context, newPassword);
    }

    // Check if Password is Set
    public static boolean isPasswordSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(PASSWORD_KEY) && prefs.contains(SALT_KEY);
    }
}
