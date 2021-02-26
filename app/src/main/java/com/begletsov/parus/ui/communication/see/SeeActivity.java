package com.begletsov.parus.ui.communication.see;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.begletsov.parus.R;
import com.begletsov.parus.databinding.ActivitySeeBinding;
import com.begletsov.parus.viewmodels.SeeViewModel;
import com.begletsov.parus.viewmodels.TTSViewModel;
import com.google.firebase.analytics.FirebaseAnalytics;

public class SeeActivity extends AppCompatActivity {

    private static final String TAG = SeeActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private final String[] types = {"Распознавание текста", "Распознавание объекта"};
    private SeeViewModel seeViewModel;
    private TTSViewModel TTS;
    private ActivitySeeBinding binding;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_see);
        seeViewModel = new ViewModelProvider(this).get(SeeViewModel.class);
        TTS = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(TTSViewModel.class);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.see));
        }
        binding.openSeeCamera.setOnClickListener(v -> startCameraActivity());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item2, types);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        binding.seeTypeSpinner.setAdapter(adapter);
        binding.seeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    binding.seeWarning.setVisibility(View.VISIBLE);
                else
                    binding.seeWarning.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.seeSay.setOnClickListener(click -> TTS.speak(binding.seeText.getText().toString()));
        // запуск с главной страницы
        if (getIntent().getIntExtra("fastAction", 0) == 1) {
            binding.seeTypeSpinner.setSelection(0);
            binding.openSeeCamera.callOnClick();
        }
        if (getIntent().getIntExtra("fastAction", 0) == 2) {
            binding.seeTypeSpinner.setSelection(1);
            binding.openSeeCamera.callOnClick();
        }
        binding.seeText.setMovementMethod(new ScrollingMovementMethod());
    }

    private void startCameraActivity() {
        binding.seeSay.setVisibility(View.GONE);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Bundle extras = data.getExtras();
            if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
                if (extras != null) {
                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap != null) {
                        switch (binding.seeTypeSpinner.getSelectedItemPosition()) {
                            case 0:
                                detectText(bitmap);
                                break;
                            case 1:
                                binding.seeText.setText("Результат переводится...");
                                binding.seeText.setTextSize(40);
                                detectObject(bitmap);
                                break;
                        }
                    }
                }
            }
        } else if (getIntent().getIntExtra("fastAction", 0) != 0) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        }
    }

    private void detectText(Bitmap bitmap) {
        seeViewModel.detectText(bitmap).observe(this, pair -> {
            String text = pair.first;
            Float textSize = pair.second;
            if (textSize == null || text == null)
                return;
            if (textSize == 0f) {
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                binding.seeSay.setVisibility(View.GONE);
                binding.seeText.setText("");
                seeTextActionFail();
            } else {
                binding.seeSay.setVisibility(View.VISIBLE);
                binding.seeText.setText(text);
                binding.seeText.setTextSize(pair.second);
                seeTextAction();
            }
        });
    }

    private void detectObject(Bitmap bitmap) {
        seeViewModel.detectObject(bitmap).observe(this, pair -> {
            String text = pair.first;
            Float textSize = pair.second;
            if (textSize == null || text == null)
                return;
            if (textSize == 0f) {
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                binding.seeSay.setVisibility(View.GONE);
                binding.seeText.setText("");
                seeObjectActionFail();
            } else {
                binding.seeSay.setVisibility(View.VISIBLE);
                binding.seeText.setText(text);
                binding.seeText.setTextSize(pair.second);
                seeObjectAction();
            }
        });
    }

    @Override
    protected void onPause() {
        TTS.stopSpeech();
        super.onPause();
    }

    private void seeObjectAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "see-object");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void seeTextAction() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "see-text");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void seeObjectActionFail() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "see-object-fail");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void seeTextActionFail() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "see-text-fail");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}


