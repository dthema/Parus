package com.begletsov.parus.viewmodels;

import android.graphics.Bitmap;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.repositories.SeeRepository;

public class SeeViewModel extends ViewModel {

    private final SeeRepository repository = new SeeRepository();

    public SeeViewModel(){
        super();
    }

    public LiveData<Pair<String, Float>> detectText(Bitmap bitmap){
        return repository.detectText(bitmap);
    }

    public LiveData<Pair<String, Float>> detectObject(Bitmap bitmap){
        return repository.detectObject(bitmap);
    }
}
