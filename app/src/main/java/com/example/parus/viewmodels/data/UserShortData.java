package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.core.util.Pair;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

public class UserShortData extends LiveData<Pair<Pair<String, String>, Boolean>> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private DocumentReference docRef;
    private User user;
    private Pair<Pair<String, String>, Boolean> pair;

    public UserShortData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    EventListener<DocumentSnapshot> eventListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
            if (e != null) {
                Log.i(TAG, "Listen failed.", e);
                return;
            }
            if (documentSnapshot != null) {
                user = documentSnapshot.toObject(User.class);
                if (user != null)
                    if (pair == null) {
                        pair = Pair.create(Pair.create(user.getUserId(), user.getLinkUserId()), user.isSupport());
                        setValue(pair);
                        Log.d("TAGAAA", String.valueOf(pair.first.first));
                        Log.d("TAGAAA", String.valueOf(pair.first.second));
                        Log.d("TAGAAA", String.valueOf(pair.second));
                        Log.d("TAGAAA", "-----------------------");
                    } else if (!pair.equals(Pair.create(Pair.create(user.getUserId(), user.getLinkUserId()), user.isSupport()))) {
                        pair = Pair.create(Pair.create(user.getUserId(), user.getLinkUserId()), user.isSupport());
                        Log.d("TAGAAA", String.valueOf(pair.first.first));
                        Log.d("TAGAAA", String.valueOf(pair.first.second));
                        Log.d("TAGAAA", String.valueOf(pair.second));
                        Log.d("TAGAAA", "-----------------------");
                        setValue(pair);
                    }
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
