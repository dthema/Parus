package com.begletsov.parus.viewmodels.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;

public class SayCollectionData extends LiveData<HashMap<String, Object>> {

    private static final String TAG = "say collections data";
    private ListenerRegistration registration;
    private final DocumentReference docRef;

    public SayCollectionData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private final EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
            Log.i(TAG, "Listen failed.", e);
            return;
        }
        if (documentSnapshot != null) {
            HashMap<String, Object> collectionMap = (HashMap<String, Object>) documentSnapshot.get("Collections");
            if (collectionMap != null)
                setValue(collectionMap);
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
