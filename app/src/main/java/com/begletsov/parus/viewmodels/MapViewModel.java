package com.begletsov.parus.viewmodels;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.repositories.MapRepository;

public class MapViewModel extends ViewModel {

    private final MapRepository repository = new MapRepository();

    public MapViewModel() { super(); }

    public LiveData<Pair<Double, Double>> getLocationData(String linkUserId){
        return repository.getLocationData(linkUserId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopCheckLocation();
    }
}
