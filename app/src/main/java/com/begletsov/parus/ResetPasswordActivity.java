package com.begletsov.parus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.begletsov.parus.databinding.ActivityResetPasswordBinding;
import com.begletsov.parus.viewmodels.LoginViewModel;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Восстановление пароля");
        }
        final ActivityResetPasswordBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_reset_password);
        if (binding.resetEmailText.getEditText() != null)
            binding.resetEmailText.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    binding.resetEmailText.setError(null);
                    binding.resetEmailText.setHelperText(null);
                }
            });
        LoginViewModel loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.resetPasswordBtn.setOnClickListener(v -> {
            if (binding.resetEmailText.getEditText() == null)
                return;
            final String email = binding.resetEmailText.getEditText().getText().toString().trim();
            if (email.isEmpty()) {
                binding.resetEmailText.setError(getString(R.string.enter_registration_email));
                return;
            }
            binding.resetProgressBar.setVisibility(View.VISIBLE);
            loginViewModel.resetPassword(email).observe(this, success -> {
                if (success)
                    binding.resetEmailText.setHelperText(getString(R.string.send_reset_password_success));
                else
                    binding.resetEmailText.setError(getString(R.string.send_reset_password_error));
                binding.resetProgressBar.setVisibility(View.GONE);
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}

