package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;

public class LocationData extends LiveData<Pair<Double, Double>> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private final DocumentReference docRef;

    public LocationData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private final EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
            Log.i(TAG, "Listen failed.", e);
            return;
        }
        if (documentSnapshot != null) {
            Double latitude = documentSnapshot.getDouble("Latitude");
            Double longitude = documentSnapshot.getDouble("Longitude");
            setValue(Pair.create(latitude, longitude));
        }
    };

    @Override
    protected void onActive() {
        super.onActive();
        registration = docRef.addSnapshotListener(eventListener);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (!hasActiveObservers()) {
            registration.remove();
            registration = null;
        }
    }
}
