package com.example.parus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.parus.databinding.ActivityLoginBinding;
import com.example.parus.services.HeartRateService;
import com.example.parus.services.GeoLocationService;
import com.example.parus.viewmodels.HealthViewModel;
import com.example.parus.viewmodels.LoginViewModel;
import com.example.parus.viewmodels.ServiceViewModel;
import com.example.parus.viewmodels.UserViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private static final int GOOGLE_CONNECT = 2;
    private static final int SAMSUNG_CONNECT = 4;
    private ActivityLoginBinding binding;
    private HealthDataStore mStore;
    private FirebaseAnalytics mFirebaseAnalytics;
    private LoginViewModel loginViewModel;
    private HealthViewModel healthViewModel;
    private ServiceViewModel serviceViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        initViewModels();
        if (loginViewModel.isLogin()) {
            binding.loginProgressBar.setVisibility(View.VISIBLE);
            startMainActivity();
        }
        binding.loginRegisterBtn.setOnClickListener(v -> {
            if (binding.loginProgressBar.getVisibility() == View.GONE) {
                final String email = binding.loginEmail.getText().toString().trim();
                final String password = binding.loginPassword.getText().toString().trim();
                if (email.isEmpty()) {
                    failRegistration();
                    Toast.makeText(this, R.string.enter_email, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.isEmpty()) {
                    failRegistration();
                    Toast.makeText(this, R.string.enter_password, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    failRegistration();
                    Toast.makeText(this, R.string.short_password_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                binding.loginProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.register(email, password).observe(this, success -> {
                    if (success) {
                        successRegistration();
                        startActivity(new Intent(this, FirstRunActivity.class));
                        finish();
                    } else
                        Toast.makeText(this, R.string.create_account_error, Toast.LENGTH_SHORT).show();
                    binding.loginProgressBar.setVisibility(View.GONE);
                });
            }
        });

        binding.loginResetPassword.setOnClickListener(v -> {
            binding.loginProgressBar.setVisibility(View.GONE);
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });

        binding.loginBtn.setOnClickListener(v -> {
            if (binding.loginProgressBar.getVisibility() == View.GONE) {
                final String email = binding.loginEmail.getText().toString();
                final String password = binding.loginPassword.getText().toString();
                if (email.isEmpty()) {
                    failLogin();
                    Toast.makeText(this, R.string.enter_email, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.isEmpty()) {
                    failLogin();
                    Toast.makeText(this, R.string.enter_password, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    failLogin();
                    binding.loginPassword.setError(getString(R.string.short_password_error));
                    return;
                }
                binding.loginProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(email, password).observe(this, success -> {
                    if (success)
                        startMainActivity();
                    else {
                        failLogin();
                        Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_LONG).show();
                    }
                    binding.loginProgressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void initViewModels() {
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        healthViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(HealthViewModel.class);
        serviceViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(ServiceViewModel.class);
    }

    private void failRegistration() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "signUp");
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }

    private void successRegistration() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "signUp");
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, true);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }

    private void failLogin() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false);
        bundle.putString(FirebaseAnalytics.Param.METHOD, "login");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
    }

    private void successLogin() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, true);
        bundle.putString(FirebaseAnalytics.Param.METHOD, "login");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            if (healthViewModel.isPermissionAcquired(mStore)) {
                serviceViewModel.startHeartRateService();
            }
            mStore.disconnectService();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
        }

        @Override
        public void onDisconnected() {
        }
    };

    private void startMainActivity() {
        loginViewModel.checkActiveServices().observe(this, pair -> {
            Boolean checkHeartBPM = pair.first;
            Boolean checkGeoPosition = pair.second;
            if (checkHeartBPM != null)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                        == PackageManager.PERMISSION_GRANTED && checkHeartBPM) {
                    healthViewModel.get().observe(this, result -> {
                        if (result == GOOGLE_CONNECT)
                            serviceViewModel.startHeartRateService();
                        else if (result == SAMSUNG_CONNECT) {
                            mStore = new HealthDataStore(getApplicationContext(), mConnectionListener);
                            mStore.connectService();
                        }
                    });
                }
            if (checkGeoPosition != null)
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && checkGeoPosition) {
                    serviceViewModel.startGeoLocationService();
                }
            successLogin();
            binding.loginProgressBar.setVisibility(View.GONE);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}

