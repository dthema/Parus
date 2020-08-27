package com.example.parus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

import com.example.parus.services.HeartRateService;
import com.example.parus.services.GeoLocationService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private HealthDataStore mStore;

    private boolean isPermissionAcquired() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Check whether the permissions that this application needs are acquired
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(Collections.singleton(permKey));
            return resultMap.get(permKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            if (isPermissionAcquired()) {
                startService(new Intent(getBaseContext(), HeartRateService.class).setAction("action"));
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
        }

        @Override
        public void onDisconnected() {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get Firebase auth instance
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        progressBar = findViewById(R.id.loginProgressBar);
        boolean del = false;
        if (intent != null) {
            del = intent.getBooleanExtra("del", false);
            if (del) {
                WorkManager.getInstance(this).cancelAllWork();
                auth.signOut();
            }
        }
        if (auth.getCurrentUser() != null && !del) {
            Log.d("TAGAA", auth.getCurrentUser().getUid());
            progressBar.setVisibility(View.VISIBLE);
            startMainActivity();
        }

        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        Button btnSignup = findViewById(R.id.btn_signup);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnReset = findViewById(R.id.btn_reset_password);
        btnSignup.setOnClickListener(v -> {
            if (progressBar.getVisibility() == View.GONE) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Введите почту", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Введите пароль", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Пароль слишком короткий", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //create user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Не удалось создать аккаунт",
                                        Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            } else {
                                HashMap<String, Object> userData = new HashMap<>();
                                auth.getCurrentUser().getIdToken(true).addOnSuccessListener(t -> {
                                    db = FirebaseFirestore.getInstance();
                                    String token = t.getToken();
                                    userData.put("userId", auth.getCurrentUser().getUid());
                                    userData.put("linkUserId", auth.getCurrentUser().getUid());
                                    userData.put("token", token);
                                    userData.put("support", false);
                                    userData.put("checkHeartBPM", false);
                                    userData.put("checkGeoPosition", false);
                                    userData.put("fastAction", "0");
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("TTS_Speed", 1f);
                                    map.put("TTS_Pitch", 1f);
                                    map.put("Column_Count", 2);
                                    userData.put("SaySettings", map);
                                    db.collection("users").document(auth.getCurrentUser().getUid()).set(userData)
                                            .addOnSuccessListener(l -> {
                                                progressBar.setVisibility(View.GONE);
                                                startActivity(new Intent(LoginActivity.this, FirstRunActivity.class));
                                                finish();
                                            }).addOnFailureListener(f -> progressBar.setVisibility(View.GONE));
                                }).addOnFailureListener(f -> progressBar.setVisibility(View.GONE));
                            }
                        });

            }
        });

        btnReset.setOnClickListener(v -> {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            if (progressBar.getVisibility() == View.GONE) {
                String email = inputEmail.getText().toString();
                final String password = inputPassword.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Введите почту", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Введите пароль", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, task -> {
                            if (!task.isComplete()) {
                                progressBar.setVisibility(View.GONE);
                                if (password.length() < 6)
                                    inputPassword.setError("Пароль должен быть от 6 символов");
                            } else {
                                if (task.isSuccessful()) {
                                    startMainActivity();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Неверная почта или пароль", Toast.LENGTH_LONG).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                        });
            }
        });
    }

    private void startMainActivity() {
        db.collection("users").document(Objects.requireNonNull(auth.getCurrentUser()).getUid()).get()
                .addOnSuccessListener(l -> {
                    if (l.getString("userId") != null && l.getBoolean("checkHeartBPM") != null) {
                        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.BODY_SENSORS)
                                == PackageManager.PERMISSION_GRANTED && l.getBoolean("checkHeartBPM") && !HeartRateService.isServiceRunning) {
                            if (Build.VERSION.SDK_INT < 23) {
                                FitnessOptions fitnessOptions = FitnessOptions.builder()
                                        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                                        .build();
                                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(LoginActivity.this, fitnessOptions);
                                if (GoogleSignIn.hasPermissions(account, fitnessOptions))
                                    startService(new Intent(getBaseContext(), HeartRateService.class).setAction("action"));
                            } else {
                                mStore = new HealthDataStore(getApplicationContext(), mConnectionListener);
                                mStore.connectService();
                            }
                        }
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && l.getBoolean("checkGeoPosition")) {
                            startService(new Intent(getBaseContext(), GeoLocationService.class).setAction("action"));
                        }
                        progressBar.setVisibility(View.GONE);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(f -> progressBar.setVisibility(View.GONE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStore != null)
            mStore.disconnectService();
    }
}

