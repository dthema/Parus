package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.parus.viewmodels.repositories.ServiceRepository;

public class ServiceViewModel extends AndroidViewModel {

    ServiceRepository repository = ServiceRepository.getInstance();

    public ServiceViewModel(@NonNull Application application) {
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

    public void startGeoLocationService(){
        repository.startGeoLocationService(getApplication());
    }

    public void stopGeoLocationService(){
        repository.stopGeoLocationService(getApplication());
    }

    public void startHeartRateService(){
        repository.startHeartRateService(getApplication());
    }

    public void stopHeartRateService(){
        repository.stopHeartRateService(getApplication());
    }

    public void stopAllServices(){
        repository.stopAllServices(getApplication());
    }

    public void destroy() {
        repository.destroy();
    }
}
