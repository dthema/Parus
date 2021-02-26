package com.begletsov.parus.viewmodels.data;

import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;

public class UserData extends LiveData<User> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private final DocumentReference docRef;

    public UserData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private final EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
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
