package com.example.parus.ui.communication.see;

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

import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parus.R;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.ArrayList;
import java.util.List;

public class SeeActivity extends AppCompatActivity {

    private static final String TAG = SeeActivity.class.getSimpleName();
    private TextView textView;
    private TextView warning;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private Spinner typeOfRecognize;
    private String[] types = {"Распознавание текста", "Распознавание объекта"};

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Смотреть");
        }
        Button captureImg = findViewById(R.id.openSeeCamera);
        captureImg.setOnClickListener(v -> startCameraActivity());
        textView = findViewById(R.id.seeText);
        warning = findViewById(R.id.seeWarning);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item2, types);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        typeOfRecognize = findViewById(R.id.seeTypeSpinner);
        typeOfRecognize.setAdapter(adapter);
        typeOfRecognize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    warning.setVisibility(View.VISIBLE);
                } else {
                    warning.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // запуск с главной страницы
        if (getIntent().getIntExtra("fastAction", 0) == 1) {
            typeOfRecognize.setSelection(0);
            captureImg.callOnClick();
        }
        if (getIntent().getIntExtra("fastAction", 0) == 2) {
            typeOfRecognize.setSelection(1);
            captureImg.callOnClick();
        }

    }

    private void startCameraActivity() {
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
            Log.d(TAG, e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Bundle extras = data.getExtras();
            if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
                FirebaseVisionImage image = null;
                if (extras != null) {
                    Bitmap bitmap = (Bitmap) extras.get("data");
                    if (bitmap != null)
                        image = FirebaseVisionImage.fromBitmap(bitmap);
                }
                if (image != null)
                    switch (typeOfRecognize.getSelectedItemPosition()) {
                        case 0:
                            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                                    .getOnDeviceTextRecognizer();
                            // распознавание текста с фото
                            detector.processImage(image)
                                    .addOnSuccessListener(firebaseVisionText -> {
                                        textView.setText(firebaseVisionText.getText());
                                        if (textView.getText().toString().equals("")) {
                                            Toast.makeText(SeeActivity.this, "Не удалось распознать текст", Toast.LENGTH_LONG).show();
                                        } else
                                            for (String string : textView.getText().toString().split(" "))
                                                neededTextSize(string);
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(SeeActivity.this, "Произошла ошибка", Toast.LENGTH_LONG).show());
                            break;
                        case 1:
                            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                                    .getOnDeviceImageLabeler();
                            // распознавание объекта
                            labeler.processImage(image)
                                    .addOnSuccessListener(labels -> {
                                        List<Pair<String, Float>> list = new ArrayList<>();
                                        for (FirebaseVisionImageLabel label : labels) {
                                            if (label.getConfidence() >= 0.7)
                                                list.add(Pair.create(label.getText(), label.getConfidence()));
                                        }
                                        if (list.isEmpty()) {
                                            Toast.makeText(SeeActivity.this, "Не удалось распознать объект(-ы)", Toast.LENGTH_LONG).show();
                                        } else {
                                            textView.setText("Результат переводится...");
                                            textView.setTextSize(40);
                                            for (Pair<String, Float> pair : list) {
                                                FirebaseTranslatorOptions options =
                                                        new FirebaseTranslatorOptions.Builder()
                                                                .setSourceLanguage(FirebaseTranslateLanguage.EN)
                                                                .setTargetLanguage(FirebaseTranslateLanguage.RU)
                                                                .build();
                                                final FirebaseTranslator translator =
                                                        FirebaseNaturalLanguage.getInstance().getTranslator(options);
                                                String text = pair.first;
                                                // перевод результата
                                                translator.downloadModelIfNeeded()
                                                        .addOnCompleteListener(task -> {
                                                            textView.setText("");
                                                            textView.setTextSize(501);
                                                        })
                                                        .addOnSuccessListener(v ->
                                                                translator.translate(text).addOnSuccessListener(
                                                                        translatedText -> {
                                                                            // показать, если шанс "угадывания" объекта более 70%
                                                                            float confidence = pair.second;
                                                                            Log.d("ABCd", text);
                                                                            switch (text) {
                                                                                case "Nail":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Ноготь - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                case "Skin":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Кожа - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                case "Flash":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Вспышка - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                case "Pattern":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Узор - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                case "Room":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Комната - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                case "Ear":
                                                                                    textView.setText(textView.getText().toString() +
                                                                                            "Ухо - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                                default:
                                                                                    textView.setText(textView.getText().toString() + translatedText +
                                                                                            " - " + Math.round(confidence * 100) + "%" + "\n");
                                                                                    break;
                                                                            }
                                                                            neededTextSize(translatedText);
                                                                        })
                                                                        .addOnFailureListener(
                                                                                e -> Toast.makeText(SeeActivity.this, "Не удалось загрузить языковой пакет", Toast.LENGTH_LONG).show()));
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(SeeActivity.this, "Произошла ошибка. Попробуйте еще раз", Toast.LENGTH_LONG).show());
                            break;
                    }
            }
        } else {
            if (getIntent().getIntExtra("fastAction", 0) != 0) {
                finish();
            }
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

    private void neededTextSize(String text) {
        // выбор оптимального размера шрифта
        float min;
        switch (text.length()) {
            case 1:
                min = 300;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 2:
                min = 180;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 3:
                min = 120;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 4:
                min = 90;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 5:
                min = 70;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 6:
                min = 60;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 7:
                min = 50;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 8:
                min = 48;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            case 9:
                min = 40;
                if (textView.getTextSize() < min * 2.75)
                    min = 500;
                break;
            default:
                min = 30;
                break;
        }
        if (min != 500)
            textView.setTextSize(min);
    }
}


