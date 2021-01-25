package com.begletsov.parus.viewmodels.repositories;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.data.LocationData;
import com.google.firebase.firestore.FirebaseFirestore;

public class MapRepository {

    public MapRepository() { }

    private LocationData locationData;

    public LiveData<Pair<Double, Double>> getLocationData(String linkUserId){
        if (locationData == null)
            locationData = new LocationData(FirebaseFirestore.getInstance().collection("users")
                    .document(linkUserId).collection("GeoPosition").document("Location"));
        return locationData;
    }

    public void stopCheckLocation(){
        locationData = null;
    }

}
