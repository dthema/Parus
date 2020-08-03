package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.parus.viewmodels.repositories.WorkRepository;

public class WorkModel extends AndroidViewModel {

    WorkRepository repository = new WorkRepository();

    public WorkModel(@NonNull Application application) {
        super(application);
    }

    public void startService(){
        repository.startService(getApplication());
    }

    public void stopService(){
        repository.stopService(getApplication());
    }
}
