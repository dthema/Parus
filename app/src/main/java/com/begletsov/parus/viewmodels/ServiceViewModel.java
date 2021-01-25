package com.begletsov.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.begletsov.parus.viewmodels.repositories.ServiceRepository;

public class ServiceViewModel extends AndroidViewModel {

    private final ServiceRepository repository = ServiceRepository.getInstance();

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

    public void stopGeoLocationService(boolean stop){
        repository.stopGeoLocationService(getApplication(), stop);
    }

    public void startHeartRateService(){
        repository.startHeartRateService(getApplication());
    }

    public void stopHeartRateService(boolean stop){
        repository.stopHeartRateService(getApplication(), stop);
    }

    public void stopAllServices(){
        repository.stopAllServices(getApplication());
    }

    public void destroy() {
        repository.destroy();
    }
}
