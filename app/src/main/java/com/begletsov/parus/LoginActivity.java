package com.begletsov.parus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.begletsov.parus.databinding.ActivityLoginBinding;
import com.begletsov.parus.viewmodels.HealthViewModel;
import com.begletsov.parus.viewmodels.LoginViewModel;
import com.begletsov.parus.viewmodels.ServiceViewModel;
import com.google.firebase.analytics.FirebaseAnalytics;

public class LoginActivity extends AppCompatActivity {

    private static final int GOOGLE_CONNECT = 2;
    private ActivityLoginBinding binding;
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
        if (binding.loginEmail.getEditText() != null)
            binding.loginEmail.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    binding.loginEmail.setError(null);
                }
            });
        if (binding.loginPassword.getEditText() != null)
            binding.loginPassword.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    binding.loginPassword.setError(null);
                }
            });
        binding.loginRegisterBtn.setOnClickListener(v -> {
            if (binding.loginProgressBar.getVisibility() == View.GONE) {
                if (binding.loginEmail.getEditText() == null || binding.loginPassword.getEditText() == null)
                    return;
                final String email = binding.loginEmail.getEditText().getText().toString().trim();
                final String password = binding.loginPassword.getEditText().getText().toString().trim();
                if (email.isEmpty()) {
                    failRegistration();
                    binding.loginEmail.setError(getString(R.string.enter_email));
                    return;
                }
                if (password.isEmpty()) {
                    failRegistration();
                    binding.loginPassword.setError(getString(R.string.enter_password));
                    return;
                }
                if (password.length() < 6) {
                    failRegistration();
                    binding.loginPassword.setError(getString(R.string.short_password_error));
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
                if (binding.loginEmail.getEditText() == null || binding.loginPassword.getEditText() == null)
                    return;
                final String email = binding.loginEmail.getEditText().getText().toString().trim();
                final String password = binding.loginPassword.getEditText().getText().toString();
                if (email.isEmpty()) {
                    failLogin();
                    binding.loginEmail.setError(getString(R.string.enter_email));
                    return;
                }
                if (password.isEmpty()) {
                    failLogin();
                    binding.loginPassword.setError(getString(R.string.enter_password));
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
        healthViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(HealthViewModel.class);
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

