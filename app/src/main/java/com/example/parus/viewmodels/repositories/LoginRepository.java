package com.example.parus.viewmodels.repositories;

import androidx.core.util.Pair;

import androidx.lifecycle.LiveData;

import com.example.parus.services.MyFirebaseMessagingService;
import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirebaseMessagingService firebaseMessagingService;

    public LoginRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        firebaseMessagingService = new MyFirebaseMessagingService();
    }

    public LiveData<Boolean> register(String email, String password) {
        SingleLiveEvent<Boolean> liveEvent = new SingleLiveEvent<>();
        //create user
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(success -> {
                    HashMap<String, Object> userData = new HashMap<>();
                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                        String deviceToken = instanceIdResult.getToken();
                        firebaseMessagingService.onNewToken(deviceToken);
                        userData.put("userId", Objects.requireNonNull(auth.getCurrentUser()).getUid());
                        userData.put("linkUserId", auth.getCurrentUser().getUid());
                        userData.put("support", false);
                        userData.put("checkHeartBPM", false);
                        userData.put("checkGeoPosition", false);
                        userData.put("fastAction", "0");
                        Map<String, Object> map = new HashMap<>();
                        map.put("TTS_Speed", 1f);
                        map.put("TTS_Pitch", 1f);
                        map.put("Column_Count", 2);
                        userData.put("SaySettings", map);
                        db.collection("users").document(auth.getCurrentUser().getUid()).set(userData)
                                .addOnSuccessListener(l -> liveEvent.setValue(true))
                                .addOnFailureListener(f -> liveEvent.setValue(false));
                    }).addOnFailureListener(f -> liveEvent.setValue(false));
                })
                .addOnFailureListener(fail -> liveEvent.setValue(false));
        return liveEvent;
    }

    public LiveData<Boolean> login(String email, String password) {
        SingleLiveEvent<Boolean> liveEvent = new SingleLiveEvent<>();
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(success ->
                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                            String deviceToken = instanceIdResult.getToken();
                            firebaseMessagingService.onNewToken(deviceToken);
                            liveEvent.setValue(true);
                        }))
                .addOnFailureListener(fail -> liveEvent.setValue(false));
        return liveEvent;
    }

    public LiveData<Pair<Boolean, Boolean>> checkActiveServices() {
        SingleLiveEvent<Pair<Boolean, Boolean>> liveEvent = new SingleLiveEvent<>();
        db.collection("users").document(Objects.requireNonNull(auth.getCurrentUser()).getUid()).get()
                .addOnSuccessListener(usr -> {
                    User user = usr.toObject(User.class);
                    if (user != null)
                        liveEvent.setValue(Pair.create(user.isCheckHeartBPM(), user.isCheckGeoPosition()));
                })
                .addOnFailureListener(fail -> liveEvent.setValue(Pair.create(false, false)));
        return liveEvent;
    }

    public boolean isLogin() {
        if (auth.getCurrentUser() != null)
            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                String deviceToken = instanceIdResult.getToken();
                firebaseMessagingService.onNewToken(deviceToken);
            });
        return auth.getCurrentUser() != null;
    }

    public LiveData<Boolean> resetPassword(String email) {
        SingleLiveEvent<Boolean> liveEvent = new SingleLiveEvent<>();
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(success ->
                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
                            String deviceToken = instanceIdResult.getToken();
                            firebaseMessagingService.onNewToken(deviceToken);
                            liveEvent.setValue(true);
                        }))
                .addOnFailureListener(fail -> liveEvent.setValue(false));
        return liveEvent;
    }

}
