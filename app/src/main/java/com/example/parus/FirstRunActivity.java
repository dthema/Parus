package com.example.parus;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parus.databinding.ActivityFirstRunBinding;
import com.example.parus.viewmodels.UserViewModel;

public class FirstRunActivity extends AppCompatActivity {

    private int cnt = 0;
    private boolean isSupport = false;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFirstRunBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_first_run);
        setContentView(R.layout.activity_first_run);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        Button btn = binding.firstRunBtn;
        TextView text = binding.firstText;
        EditText editText = binding.firstEditText;
        Button support = binding.firstSupport;
        Button disabled = binding.firstDisabled;
        LinearLayout linearLayout = findViewById(R.id.firstLinear);
        disabled.setOnClickListener(l -> {
            editText.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
            editText.setHint(R.string.id_support);
            text.setText(R.string.if_desabled_registrated);
            linearLayout.setVisibility(View.GONE);
        });
        support.setOnClickListener(l ->
                userViewModel.setSupport().observe(this, success -> {
                    if (success) {
                        isSupport = true;
                        editText.setVisibility(View.VISIBLE);
                        btn.setVisibility(View.VISIBLE);
                        editText.setHint(R.string.id_desabled);
                        text.setText(R.string.if_support_registrated);
                        linearLayout.setVisibility(View.GONE);
                    } else Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
                }));
        btn.setOnClickListener(e -> {
            if (cnt == 0) {
                if (editText.getText().toString().length() > 0) {
                    userViewModel.setName(editText.getText().toString()).observe(this, success -> {
                        if (success) {
                            cnt++;
                            editText.setVisibility(View.GONE);
                            linearLayout.setVisibility(View.VISIBLE);
                            btn.setVisibility(View.GONE);
                            text.setText(R.string.choose_role);
                            editText.setText("");
                        } else Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
                    });
                } else
                    Toast.makeText(this, R.string.no_name, Toast.LENGTH_LONG).show();
            } else if (cnt == 1) {
                if (String.valueOf(editText.getText()).equals("")) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    userViewModel.setLinkUser(editText.getText().toString(), isSupport).observe(this, string -> {
                        if ("1".equals(string)) {
                            startActivity(new Intent(FirstRunActivity.this, MainActivity.class));
                            finish();
                        } else Toast.makeText(this, string, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

    }
}
