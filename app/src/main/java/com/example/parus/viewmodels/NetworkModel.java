package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.parus.viewmodels.repositories.NetworkRepository;

public class NetworkModel extends AndroidViewModel {

    public NetworkModel(@NonNull Application application) {
        super(application);
    }

    NetworkRepository repository = new NetworkRepository();

    public MutableLiveData<Boolean> getInternetConnection(){
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
