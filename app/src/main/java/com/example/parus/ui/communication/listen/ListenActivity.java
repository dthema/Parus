package com.example.parus.ui.communication.listen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.example.parus.R;
import com.example.parus.databinding.ActivityListenBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class ListenActivity extends AppCompatActivity implements RecognitionListener {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "ListenActivity";
    private ActivityListenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_listen);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Слушать");
        }
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "ru");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, "6000");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, "6000");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        binding.listenProgressBar.setVisibility(View.INVISIBLE);
        binding.listenStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // начать запись речи
                binding.listenProgressBar.setVisibility(View.VISIBLE);
                binding.listenProgressBar.setIndeterminate(true);
                binding.listenText.setText("");
                ActivityCompat.requestPermissions
                        (ListenActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                REQUEST_RECORD_PERMISSION);
            } else {
                // завершить запись речи
                binding.listenProgressBar.setIndeterminate(false);
                binding.listenProgressBar.setVisibility(View.INVISIBLE);
                speech.stopListening();
            }
        });
        // запуск с главной страницы
        if (getIntent().getBooleanExtra("fastAction", false)){
            binding.listenStart.toggle();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speech.startListening(recognizerIntent);
            } else {
                binding.listenStart.toggle();
                Toast.makeText(ListenActivity.this, "Нет прав на запись речи", Toast
                        .LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        binding.listenProgressBar.setIndeterminate(false);
        binding.listenProgressBar.setMax(10);
    }
    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }
    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        binding.listenProgressBar.setIndeterminate(true);
        binding.listenStart.setChecked(false);
    }
    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        binding.listenText.setText("Речь не распознана");
        binding.listenText.setTextSize(30);
        binding.listenStart.setChecked(false);
    }
    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }
    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }
    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }
    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            binding.listenText.setText(matches.get(0));
            float min = 500;
            for (String s : matches.get(0).split(" ")) {
                switch (s.length()) {
                    case 1:
                        binding.listenText.setTextSize(300);
                        if (300 < min)
                            min = 300;
                        break;
                    case 2:
                        binding.listenText.setTextSize(180);
                        if (180 < min)
                            min = 180;
                        break;
                    case 3:
                        binding.listenText.setTextSize(120);
                        if (120 < min)
                            min = 120;
                        break;
                    case 4:
                        binding.listenText.setTextSize(90);
                        if (90 < min)
                            min = 90;
                        break;
                    case 5:
                        binding.listenText.setTextSize(70);
                        if (70 < min)
                            min = 70;
                        break;
                    case 6:
                        binding.listenText.setTextSize(60);
                        if (60 < min) {
                            min = 60;
                        }
                        break;
                    case 7:
                        binding.listenText.setTextSize(50);
                        if (50 < min)
                            min = 50;
                        break;
                    case 8:
                        binding.listenText.setTextSize(48);
                        if (48 < min)
                            min = 48;
                        break;
                    case 9:
                        binding.listenText.setTextSize(40);
                        if (40 < min)
                            min = 40;
                        break;
                    default:
                        binding.listenText.setTextSize(30);
                        if (30 < min)
                            min = 30;
                        break;
                }
                binding.listenText.setTextSize(min);
            }
        }
    }
    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        binding.listenProgressBar.setProgress((int) rmsdB);
    }
    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}