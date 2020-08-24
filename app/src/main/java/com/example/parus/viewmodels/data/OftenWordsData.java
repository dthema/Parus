package com.example.parus.viewmodels.data;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OftenWordsData extends LiveData<HashMap<String, Object>> {

    private static final String TAG = "say collections data";
    private ListenerRegistration registration;
    private DocumentReference docRef;

    public OftenWordsData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
            Log.i(TAG, "Listen failed.", e);
            return;
        }
        if (documentSnapshot != null) {
            HashMap<String, Object> collectionMap = (HashMap<String, Object>) documentSnapshot.get("OftenWords");
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
