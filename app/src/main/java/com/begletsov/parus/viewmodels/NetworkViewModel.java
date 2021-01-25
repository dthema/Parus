package com.begletsov.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.repositories.NetworkRepository;

public class NetworkViewModel extends AndroidViewModel {

    public NetworkViewModel(@NonNull Application application) {
        super(application);
    }

    private final NetworkRepository repository = NetworkRepository.getInstance();

    public LiveData<Boolean> getInternetConnection(){
        return repository.connectionListener(getApplication());
    }

    @Override
    protected void onCleared() {
        stopCheckInternetConnection();
        super.onCleared();
    }

    public void stopCheckInternetConnection(){
        repository.stopCheckInternetConnection();
    }
}
