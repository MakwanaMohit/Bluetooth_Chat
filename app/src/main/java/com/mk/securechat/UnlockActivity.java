package com.mk.securechat;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.Executor;

public class UnlockActivity extends AppCompatActivity {
    private StringBuilder enteredPin = new StringBuilder();
    private static final int PIN_LENGTH = 6;
    private Button use_bio, reset_pass, forgot_pass;
    private TextView display;
    private boolean set_pass = false, verified_user = false;
    private String pass1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_unlock);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.unlockactivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        display = findViewById(R.id.display);
        use_bio = findViewById(R.id.btn_fingerprint);
        forgot_pass = findViewById(R.id.btn_forgot);
        reset_pass = findViewById(R.id.btn_reset);
        use_bio.setOnClickListener(v -> authenticate());
        forgot_pass.setOnClickListener(v -> {
            showClearDataDialog();
        });
        reset_pass.setOnClickListener(v -> {
            set_pass = true;
            verified_user = false;
            enteredPin.setLength(0);
            pass1 = null;
            updatePinBoxes();
            display.setText("Verify your PIN");
            Toast.makeText(this, "Verify your current PIN", Toast.LENGTH_SHORT).show();
        });
        if (!PBKDF2Helper.isPasswordSet(getApplicationContext())) {
            set_pass = true;
            verified_user = true;
            display.setText("Enter new PIN");
        } else authenticate();
        setupKeyboard();
    }


    private void setupKeyboard() {
        ViewGroup keyboard = findViewById(R.id.keyboard);
        for (int i = 0; i < keyboard.getChildCount(); i++) {
            View key = keyboard.getChildAt(i);
            if (key instanceof Button) {
                key.setOnClickListener(v -> handleKeyPress(((Button) v).getText().toString()));
            }
        }
    }

    private void handleKeyPress(String key) {
        if ("⌫".equals(key)) {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
            }
        } else {
            if (enteredPin.length() < PIN_LENGTH) {
                enteredPin.append(key);
            }
        }

        updatePinBoxes();
        if (enteredPin.length() == PIN_LENGTH) {
            verifyPin();
        }
    }


    private void updatePinBoxes() {
        for (int i = 0; i < PIN_LENGTH; i++) {
            int boxId = getResources().getIdentifier("box" + (i + 1), "id", getPackageName());
            View box = findViewById(boxId);
            ((View) box).setBackgroundColor(i < enteredPin.length() ? getColor(R.color.green) : getColor(android.R.color.darker_gray));
        }
    }

    private void verifyPin() {
        if (set_pass) {
            if (verified_user) {
                if (pass1 != null) {
                    if (pass1.equals(enteredPin.toString())) {
                        PBKDF2Helper.savePassword(getApplicationContext(), pass1);
                        Toast.makeText(getApplicationContext(), "Password set successfully", Toast.LENGTH_SHORT).show();
                        set_pass = false;
                        display.setText("Enter your PIN");
                        enteredPin.setLength(0);
                        pass1 = null;
                        updatePinBoxes();
                        verified_user = false;
                    } else {
                        Toast.makeText(getApplicationContext(), "Password and confirm password are not matched please try again", Toast.LENGTH_SHORT).show();
                        display.setText("Enter new PIN");
                        pass1 = null;
                        enteredPin.setLength(0);
                        updatePinBoxes();
                    }
                } else {
                    pass1 = enteredPin.toString();
                    display.setText("Confirm new pin");
                    enteredPin.setLength(0);
                    updatePinBoxes();
                }
            } else {
                verified_user = PBKDF2Helper.verifyPassword(getApplicationContext(), enteredPin.toString());
                if (!verified_user) {
                    Toast.makeText(getApplicationContext(), "Incorrect password please try again", Toast.LENGTH_SHORT).show();

                } else display.setText("Enter new PIN");
                enteredPin.setLength(0);
                updatePinBoxes();
            }
        } else {
            verified_user = PBKDF2Helper.verifyPassword(getApplicationContext(), enteredPin.toString());
            if (!verified_user) {
                Toast.makeText(getApplicationContext(), "Incorrect password please try again", Toast.LENGTH_SHORT).show();
                enteredPin.setLength(0);
                updatePinBoxes();
            } else {
                unlock();
            }
        }
    }

    private void unlock() {
        verified_user = false;
        startActivity(new Intent(this, MainActivity.class));
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this).setTitle("Forgot Password?").setMessage("If you forgot your password, all stored messages will be deleted. Do you want to proceed?").setPositiveButton("Yes", (dialog, which) -> {
            // Clear the database
            DBHelper dbHelper = DBHelper.getInstance(this);
            dbHelper.clearDatabase();
            PBKDF2Helper.clearPassword(this);
            Toast.makeText(this, "All messages have been deleted.", Toast.LENGTH_SHORT).show();
            display.setText("Enter new PIN");
            set_pass = true;
            verified_user = true;
        }).setNegativeButton("No", (dialog, which) -> dialog.dismiss()).show();
    }

    public void authenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {  // API 28+
            authenticateWithBiometricPrompt();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // API 23-27
            authenticateWithFingerprintManager();
        } else {
            Toast.makeText(this, "Fingerprint Unlock not supported", Toast.LENGTH_SHORT).show();
        }
    }

    // BiometricPrompt for API 28+
    private void authenticateWithBiometricPrompt() {

        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt((androidx.fragment.app.FragmentActivity) this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    unlock();
                }

                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(UnlockActivity.this, "Failed to unlock using Fingerprints use PIN", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_CANCELED) {
                        Toast.makeText(UnlockActivity.this, "Use PIN to unlock", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock With Fingerprint ").setNegativeButtonText("Use App Password").build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "Biometric not available, use app password", Toast.LENGTH_SHORT).show();
        }
    }

    // FingerprintManager for API 23-27
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void authenticateWithFingerprintManager() {
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

        if (fingerprintManager != null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
            CancellationSignal cancellationSignal = new CancellationSignal();
            fingerprintManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    unlock();
                }

                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(UnlockActivity.this, "Failed to unlock using Fingerprints use PIN", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    Toast.makeText(UnlockActivity.this, "Use PIN to unlock", Toast.LENGTH_SHORT).show();

                }
            }, null);
        } else {
            Toast.makeText(this, "Biometric not available, use app password", Toast.LENGTH_SHORT).show();
        }
    }
}
