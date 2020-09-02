package com.example.parus.ui.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.parus.R;
import com.example.parus.databinding.ActivitySettingsBinding;
import com.example.parus.ui.home.DialogChooseFastAction;
import com.example.parus.viewmodels.HealthViewModel;
import com.example.parus.viewmodels.ServiceViewModel;
import com.example.parus.viewmodels.UserViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.Collections;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "ActivitySettings";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
    private static final int SAMSUNG_NO_CONNECT = 3;
    private HealthDataStore mStore;
    private UserViewModel userViewModel;
    private HealthViewModel healthViewModel;
    private ServiceViewModel serviceViewModel;
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Настройки аккаунта");
        }
        if (Build.VERSION.SDK_INT >= 23)
            mStore = new HealthDataStore(this, mConnectionListener);
        initViewModels();
        initObserver();
        initDialogs();
        binding.checkHeart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // включение / отключение отслеживания сердцебиения
                healthViewModel.get().observe(SettingsActivity.this, result -> {
                    switch (result) {
                        case NO_PERMISSION:
                            binding.checkHeart.setChecked(false);
                            ActivityCompat.requestPermissions(SettingsActivity.this,
                                    new String[]{Manifest.permission.BODY_SENSORS}, 100);
                            break;
                        case NO_GOOGLE_ACCOUNT:
                            binding.checkHeart.setChecked(false);
                            FitnessOptions fitnessOptions = FitnessOptions.builder()
                                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                                    .build();
                            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(SettingsActivity.this, fitnessOptions);
                            GoogleSignIn.requestPermissions(
                                    this,
                                    1000,
                                    account,
                                    fitnessOptions);
                            break;
                        case SAMSUNG_NO_CONNECT:
                            binding.checkHeart.setChecked(false);
                            mStore.connectService();
                            break;
                        default:
                            serviceViewModel.startHeartRateService();
                            break;
                    }
                });
            } else
                serviceViewModel.stopWorkService();
        });
        binding.checkGeo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // включение / отключение отслеживания геоданных
            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                            100);
                    binding.checkGeo.setChecked(false);
                } else
                    serviceViewModel.startGeoLocationService();
            } else
                serviceViewModel.stopGeoLocationService();
        });
        Button delAcc = findViewById(R.id.btnDeleteAccount);
        delAcc.setOnClickListener(t -> {
            DialogDeleteAccount dialogDeleteAccount = new DialogDeleteAccount();
            dialogDeleteAccount.show(getSupportFragmentManager(), "DialogDeleteAccount");
        });
    }

    private void initViewModels() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        serviceViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ServiceViewModel.class);
        healthViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(HealthViewModel.class);
    }

    private void initObserver() {
        userViewModel.getSingleUserData().observe(this, user -> {
            if (user == null)
                return;
            //позволить включить ослеживание сердцебиения, если пользователь человек с ОВЗ
            if (!user.isSupport()) {
                binding.checkHeart.setVisibility(View.VISIBLE);
            }
            binding.checkHeart.setChecked(user.isCheckHeartBPM());
            binding.checkGeo.setChecked(user.isCheckGeoPosition());
        });
    }

    private void initDialogs() {
        binding.fastAction.setOnClickListener(l -> {
            DialogChooseFastAction dialogChooseFastAction = new DialogChooseFastAction();
            dialogChooseFastAction.show(getSupportFragmentManager(), "DialogFastAction");
        });
        binding.changePassword.setOnClickListener(l -> {
            DialogResetAccountPassword dialogResetAccountPassword = new DialogResetAccountPassword();
            dialogResetAccountPassword.show(getSupportFragmentManager(), "DialogChangePassword");
        });
        binding.changeEmail.setOnClickListener(l -> {
            DialogResetAccountEmail dialogResetAccountEmail = new DialogResetAccountEmail();
            dialogResetAccountEmail.show(getSupportFragmentManager(), "DialogChangeEmail");
        });
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (!healthViewModel.isPermissionAcquired(mStore))
                requestPermission();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);
            String message = getString(R.string.cannot_connect_to_shealth);
            if (error.hasResolution()) {
                switch (error.getErrorCode()) {
                    case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                        message = getString(R.string.download_shealth);
                        break;
                    case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                        message = getString(R.string.update_shealth);
                        break;
                    case HealthConnectionErrorResult.PLATFORM_DISABLED:
                        message = getString(R.string.enable_shealth);
                        break;
                    case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                        message = getString(R.string.agree_with_shealth_policy);
                        break;
                }
            }
            alert.setMessage(message);
            alert.setPositiveButton("OK", (dialog, id) -> {
                if (error.hasResolution()) {
                    error.resolve(SettingsActivity.this);
                }
            });
            if (error.hasResolution()) {
                alert.setNegativeButton("Cancel", null);
            }
            alert.show();
            mStore.disconnectService();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
        }
    };

    private void showPermissionAlarmDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.cannot_connect_to_shealth)
                .setMessage(R.string.shealth_dialog_message)
                .setPositiveButton("Ok", null)
                .show();
    }

    private void requestPermission() {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            pmsManager.requestPermissions(Collections.singleton(permKey), this)
                    .setResultListener(result -> {
                        Log.d(TAG, "Permission callback is received.");
                        Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();
                        if (resultMap.containsValue(Boolean.FALSE)) {
                            showPermissionAlarmDialog();
                        } else
                            serviceViewModel.startHeartRateService();
                        mStore.disconnectService();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
            mStore.disconnectService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        healthViewModel.onRequestPermissionsResult(requestCode, permissions, grantResults).observe(this, result -> {
            switch (result) {
                case NO_PERMISSION:
                case NO_GOOGLE_ACCOUNT:
                case SAMSUNG_NO_CONNECT:
                    break;
                default:
                    serviceViewModel.startHeartRateService();
                    break;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        healthViewModel.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
