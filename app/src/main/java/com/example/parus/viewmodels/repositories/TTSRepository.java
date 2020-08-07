package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.atomic.AtomicReference;

public class TTSRepository {

    private Double speed = 1.;
    private Double pitch = 1.;
    private TextToSpeech tts;

    public TTSRepository() {
    }

    public void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

    public void stop(){
        if (tts != null)
            tts.stop();
    }

    public void destroy(){
        if (tts != null)
            tts.shutdown();
    }

    public Double getSpeed() {
        return speed;
    }

    public Double getPitch() {
        return pitch;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
        tts.setSpeechRate(Float.parseFloat(String.valueOf(speed)));
    }

    public void setPitch(Double pitch) {
        this.pitch = pitch;
        tts.setPitch(Float.parseFloat(String.valueOf(pitch)));
    }

    public void setTTS(TextToSpeech tts) {
        this.tts = tts;
    }
}
