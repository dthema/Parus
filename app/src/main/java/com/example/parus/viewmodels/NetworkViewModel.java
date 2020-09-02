package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parus.viewmodels.repositories.NetworkRepository;

public class NetworkViewModel extends AndroidViewModel {

    public NetworkViewModel(@NonNull Application application) {
        super(application);
    }

    private NetworkRepository repository = NetworkRepository.getInstance();

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
