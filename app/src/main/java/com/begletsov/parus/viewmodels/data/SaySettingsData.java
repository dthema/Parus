package com.begletsov.parus.viewmodels.data;

import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;

public class SaySettingsData extends LiveData<Object[]> {

    private static final String TAG = "say settings data";
    private ListenerRegistration registration;
    private final DocumentReference docRef;
    private Object[] settings;

    public SaySettingsData(DocumentReference docRef) {
        this.docRef = docRef;
    }

    private final EventListener<DocumentSnapshot> eventListener = (documentSnapshot, e) -> {
        if (e != null) {
            return;
        }
        if (documentSnapshot != null) {
            User user = documentSnapshot.toObject(User.class);
            if (user == null)
                return;
            Object[] arr = new Object[3];
            arr[0] = user.getSaySettings().get("TTS_Speed");
            arr[1] = user.getSaySettings().get("TTS_Pitch");
            arr[2] = user.getSaySettings().get("Column_Count");
            if (settings != arr){
                settings = arr;
                setValue(settings);
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
