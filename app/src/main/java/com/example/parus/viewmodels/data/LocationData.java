package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class LocationData extends LiveData<Pair<Double, Double>> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private DocumentReference docRef;

    public LocationData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
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
