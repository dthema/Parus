package com.example.parus.ui.account;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.example.parus.LoginActivity;
import com.example.parus.R;
import com.example.parus.data.User;
import com.example.parus.services.HeartRateService;
import com.example.parus.services.GeoLocationService;
import com.example.parus.services.OnlineService;
import com.example.parus.ui.home.DialogChooseFastAction;
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
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private Switch checkHeart;
    private Switch checkGeoposition;
    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Настройки аккаунта");
        }
        checkHeart = findViewById(R.id.checkHeart);
        checkHeart.setVisibility(View.GONE);
        checkGeoposition = findViewById(R.id.checkGeo);
        User user = new User();
        user.updateIsSupport()
                .addOnSuccessListener(l -> {
                    //позволить включить ослеживание сердцебиения, если пользователь человек с ОВЗ
                    if (!user.isSupport()) {
                        checkHeart.setVisibility(View.VISIBLE);
                    }
                });
        user.getDatabase().collection("users").document(user.getUser().getUid()).get()
                .addOnSuccessListener(l -> {
                    checkHeart.setChecked(l.getBoolean("checkHeartBPM"));
                    checkGeoposition.setChecked(l.getBoolean("checkGeoPosition"));
                });
        if (Build.VERSION.SDK_INT >= 23)
            mStore = new HealthDataStore(this, mConnectionListener);
        checkHeart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // включение / отключение отслеживания сердцебиения
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BODY_SENSORS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.BODY_SENSORS}, 100);
                    checkHeart.setChecked(false);
                } else {
                    if (Build.VERSION.SDK_INT < 23) {
                        FitnessOptions fitnessOptions = FitnessOptions.builder()
                                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                                .build();
                        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getApplicationContext(), fitnessOptions);
                        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                            GoogleSignIn.requestPermissions(
                                    SettingsActivity.this,
                                    1000,
                                    account,
                                    fitnessOptions);
                            checkHeart.setChecked(false);
                        } else if (!HeartRateService.isServiceRunning) {
                            Intent intent = new Intent(getBaseContext(), HeartRateService.class).setAction("action");
                            intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                            startService(intent);
                        }
                    } else {
                        mStore.connectService();
                    }
                }
            } else {
                stopService(new Intent(getApplicationContext(), HeartRateService.class));
                mStore.disconnectService();
                user.update("checkHeartBPM", false)
                        .addOnFailureListener(l -> Toast.makeText(SettingsActivity.this, "Произошла ошибка", Toast.LENGTH_LONG).show());
            }
        });
        checkGeoposition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // включение / отключение отслеживания геоданных
            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                    checkGeoposition.setChecked(false);
                } else
                    startService(new Intent(getBaseContext(), GeoLocationService.class).setAction("action"));
            } else {
                stopService(new Intent(getBaseContext(), GeoLocationService.class));
                user.update("checkGeoPosition", false)
                        .addOnFailureListener(l -> Toast.makeText(SettingsActivity.this, "Произошла ошибка", Toast.LENGTH_LONG).show());
            }
        });
        Button delAcc = findViewById(R.id.btnDeleteAccount);
        if (user.getUser() != null)
            delAcc.setOnClickListener(t -> {
                DialogDeleteAccount dialogDeleteAccount = new DialogDeleteAccount();
                dialogDeleteAccount.show(getSupportFragmentManager(), "DialogDeleteAccount");
                new Thread(() -> {
                    while (true) {
                        if (dialogDeleteAccount.isFlag()) {
                            // отключение уведомлений(token) и всех служб перед удалением аккаунта
                            stopService(new Intent(this, HeartRateService.class));
                            stopService(new Intent(this, GeoLocationService.class));
                            stopService(new Intent(this, OnlineService.class));
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.putExtra("del", true);
                            FirebaseAuth.getInstance().signOut();
                            startActivity(intent);
                            finish();
                            break;
                        } else if (dialogDeleteAccount.isClosed()) {
                            Log.d("TAGAA", "-");
                            break;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            });
        Button fastAction = findViewById(R.id.fastAction);
        fastAction.setOnClickListener(l -> {
            DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
            dialogChooseFastAction.show(getSupportFragmentManager(), "DialogFastAction");
        });
        Button changePassword = findViewById(R.id.changePassword);
        changePassword.setOnClickListener(l -> {
            DialogResetAccountPassword dialogResetAccountPassword = new DialogResetAccountPassword();
            dialogResetAccountPassword.show(getSupportFragmentManager(), "DialogChangePassword");
        });
        Button changeEmail = findViewById(R.id.changeEmail);
        changeEmail.setOnClickListener(l -> {
            DialogResetAccountEmail dialogResetAccountEmail = new DialogResetAccountEmail();
            dialogResetAccountEmail.show(getSupportFragmentManager(), "DialogChangeEmail");
        });
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (!isPermissionAcquired()) {
                requestPermission();
            } else if(!HeartRateService.isServiceRunning){
                Intent intent = new Intent(getBaseContext(), HeartRateService.class).setAction("action");
                intent.putExtra("uid", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
                startService(intent);
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
        }
    };

    private void showPermissionAlarmDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        alert.setTitle("Не удалось подключиться к Samsung Health")
                .setMessage("Для отслеживания пульса из Samsung Health необходимо выдать нужные разрешения\n" +
                        "На данный момент приложение не является партнёром Samsung Health, " +
                        "поэтому включите режим разработчика в Samsung Health, чтобы приложение могло считывать данные о пульсе\n" +
                        "Чтобы его включить перейдите во вкладку 'О Samsung Health' в настройках приложения и несколько раз нажите на версию приложения")
                .setPositiveButton("Ok", null)
                .show();
    }

    private boolean isPermissionAcquired() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(Collections.singleton(permKey));
            return resultMap.get(permKey);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            pmsManager.requestPermissions(Collections.singleton(permKey), SettingsActivity.this)
                    .setResultListener(result -> {
                        Log.d(TAG, "Permission callback is received.");
                        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();

                        if (resultMap.containsValue(Boolean.FALSE)) {
                            showPermissionAlarmDialog();
                        } else {
                            startService(new Intent(SettingsActivity.this, HeartRateService.class).setAction("action"));
                            FirebaseFirestore.getInstance().collection("users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).update("checkHeartBPM", true);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

        AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
        mConnError = error;
        String message = "Connection with Samsung Health is not available";

        if (mConnError.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    message = "Please install Samsung Health";
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    message = "Please upgrade Samsung Health";
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    message = "Please enable Samsung Health";
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    message = "Please agree with Samsung Health policy";
                    break;
                default:
                    message = "Please make Samsung Health available";
                    break;
            }
        }

        alert.setMessage(message);

        alert.setPositiveButton("OK", (dialog, id) -> {
            if (mConnError.hasResolution()) {
                mConnError.resolve(SettingsActivity.this);
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton("Cancel", null);
        }

        alert.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mStore != null)
        mStore.disconnectService();
    }

    private static final String TAG = "AccountSettings";

}
