package com.begletsov.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.repositories.TTSRepository;

public class TTSViewModel extends AndroidViewModel {

    private final TTSRepository repository = TTSRepository.getInstance(getApplication());

    public TTSViewModel(@NonNull Application application) {
        super(application);
    }

    public void setSpeed(Double speed) {
        repository.setSpeed(speed);
    }

    public void setPitch(Double pitch) {
        repository.setPitch(pitch);
    }

    public void speak(String text){
        repository.speak(text);
    }

    public void stopSpeech(){
        repository.stop();
    }

    public LiveData<Boolean> getSayListenLiveData(){
        return repository.getSayListenLiveData();
    }

    public void destroy(){
        repository.destroy();
    }
}

