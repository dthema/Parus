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

import java.util.Collections;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "ActivitySettings";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
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
                        default:
                            serviceViewModel.startHeartRateService();
                            break;
                    }
                });
            } else
                serviceViewModel.stopHeartRateService();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        healthViewModel.onRequestPermissionsResult(requestCode, permissions, grantResults).observe(this, result -> {
            switch (result) {
                case NO_PERMISSION:
                case NO_GOOGLE_ACCOUNT:
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
