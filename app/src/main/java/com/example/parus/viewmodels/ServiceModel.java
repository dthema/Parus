package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.parus.viewmodels.repositories.ServiceRepository;

public class ServiceModel extends AndroidViewModel {

    ServiceRepository repository = ServiceRepository.getInstance();

    public ServiceModel(@NonNull Application application) {
        super(application);
    }

    public void startWorkService(){
        repository.startWorkService(getApplication());
    }

    public void stopWorkService(){
        repository.stopWorkService(getApplication());
    }

    public void startOnlineService(){
        repository.startOnlineService(getApplication());
    }

    public void stopOnlineService(){
        repository.stopOnlineService(getApplication());
    }
}
