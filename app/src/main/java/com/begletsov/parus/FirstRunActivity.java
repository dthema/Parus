package com.begletsov.parus;

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

import com.begletsov.parus.databinding.ActivityFirstRunBinding;
import com.begletsov.parus.viewmodels.UserViewModel;
import com.google.firebase.analytics.FirebaseAnalytics;

public class FirstRunActivity extends AppCompatActivity {

    private int cnt = 0;
    private boolean isSupport = false;
    private FirebaseAnalytics mFirebaseAnalytics;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFirstRunBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_first_run);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Button btn = binding.firstRunBtn;
        TextView text = binding.firstText;
        EditText editText = binding.firstEditText;
        Button support = binding.firstSupport;
        Button disabled = binding.firstDisabled;
        LinearLayout linearLayout = findViewById(R.id.firstLinear);
        disabled.setOnClickListener(l -> {
            setDisabled();
            editText.setVisibility(View.VISIBLE);
            btn.setVisibility(View.VISIBLE);
            editText.setHint(R.string.id_support);
            text.setText(R.string.if_disabled_registrated);
            linearLayout.setVisibility(View.GONE);
        });
        support.setOnClickListener(l ->
                userViewModel.setSupport().observe(this, success -> {
                    setSupport();
                    if (success) {
                        isSupport = true;
                        editText.setVisibility(View.VISIBLE);
                        btn.setVisibility(View.VISIBLE);
                        editText.setHint(R.string.id_disabled);
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
                    withoutLinkUser();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    userViewModel.setLinkUser(editText.getText().toString(), isSupport).observe(this, string -> {
                        if ("1".equals(string)) {
                            successLinkUser();
                            startActivity(new Intent(FirstRunActivity.this, MainActivity.class));
                            finish();
                        } else {
                            failLinkUser();
                            Toast.makeText(this, string, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void successLinkUser(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "first-link-success");
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, true);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundle);
    }

    private void failLinkUser(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "first-link-fail");
        bundle.putBoolean(FirebaseAnalytics.Param.SUCCESS, false);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundle);
    }

    private void withoutLinkUser(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "first-link-no");
        bundle.putString(FirebaseAnalytics.Param.VALUE, "Didn't");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundle);
    }

    private void setDisabled(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "user-role-disabled");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, getString(R.string.disabled));
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundle);
    }

    private void setSupport(){
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "user-role-support");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, getString(R.string.support));
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, bundle);
    }

    @Override
    public void onBackPressed() {

    }
}
