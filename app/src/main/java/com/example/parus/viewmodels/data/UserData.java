package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class UserData extends LiveData<User> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private DocumentReference docRef;

    public UserData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
            Log.i(TAG, "Listen failed.", e);
            return;
        }
        if (documentSnapshot != null) {
            User user = documentSnapshot.toObject(User.class);
            if (user != null) {
                setValue(user);
            }
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
