package com.example.parus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.parus.databinding.ActivityResetPasswordBinding;
import com.example.parus.viewmodels.LoginViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityResetPasswordBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_reset_password);
        LoginViewModel loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.resetBack.setOnClickListener(v -> finish());
        binding.resetPasswordBtn.setOnClickListener(v -> {
            final String email = binding.resetBack.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getApplication(), R.string.enter_registration_email, Toast.LENGTH_SHORT).show();
                return;
            }
            binding.resetProgressBar.setVisibility(View.VISIBLE);
            loginViewModel.resetPassword(email).observe(this, success -> {
                if (success) {
                    Toast.makeText(this, R.string.send_reset_password_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.send_reset_password_error, Toast.LENGTH_SHORT).show();
                }
                binding.resetProgressBar.setVisibility(View.GONE);
            });
        });
    }

}

