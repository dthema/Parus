package com.example.parus.viewmodels;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.parus.viewmodels.repositories.SeeRepository;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

public class SeeViewModel extends ViewModel {

    private SeeRepository repository = new SeeRepository();

    public SeeViewModel(){
        super();
    }

    public LiveData<Pair<String, Float>> detectText(FirebaseVisionImage image){
        return repository.detectText(image);
    }

    public LiveData<Pair<String, Float>> detectObject(FirebaseVisionImage image){
        return repository.detectObject(image);
    }
}
