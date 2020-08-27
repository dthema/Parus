package com.example.parus.viewmodels.repositories;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.LocationData;
import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.UserData;
import com.example.parus.viewmodels.data.UserShortData;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

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
