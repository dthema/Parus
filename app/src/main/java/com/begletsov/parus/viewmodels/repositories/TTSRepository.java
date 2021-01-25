package com.begletsov.parus.viewmodels.repositories;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Locale;

public class TTSRepository {

    private static TTSRepository repository;
    private TextToSpeech tts;
    private final MutableLiveData<Boolean> liveData;

    private TTSRepository(Context context) {
        liveData = new MutableLiveData<>(true);
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.getDefault());
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        liveData.postValue(false);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        liveData.postValue(true);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        liveData.postValue(true);
                    }
                });
        });
    }

    public synchronized static TTSRepository getInstance(Context context){
        if (repository == null) repository = new TTSRepository(context);
        return repository;
    }

    public void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text);
        else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    public void stop(){
        if (tts != null)
            tts.stop();
    }

    public void destroy(){
        if (tts != null)
            tts.shutdown();
        repository = null;
    }

    public void setSpeed(Double speed) {
        tts.setSpeechRate(Float.parseFloat(String.valueOf(speed)));
    }

    public void setPitch(Double pitch) {
        tts.setPitch(Float.parseFloat(String.valueOf(pitch)));
    }

    public LiveData<Boolean> getSayListenLiveData(){
        return liveData;
    }
}
