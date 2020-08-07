package com.example.parus.viewmodels.repositories;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.data.UserData;
import com.example.parus.viewmodels.data.UserLinkData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class UserRepository {

    public UserRepository() { }

    public LiveData<User> userListening() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public LiveData<User> userListening(String userId) {
        return new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public SingleLiveEvent<Boolean> callSupport() {
        SingleLiveEvent<Boolean> callSupport = new SingleLiveEvent<>();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    if (s != null) {
                        User user = s.toObject(User.class);
                        if (user == null)
                            return;
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("type", "call");
                        hashMap.put("userId", user.getLinkUserId());
                        hashMap.put("name", user.getName());
                        FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("Notifications").add(hashMap)
                                .addOnSuccessListener(t -> callSupport.setValue(true));
                    }
                });
        return callSupport;
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> userLinkListening() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return new UserLinkData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public SingleLiveEvent<User> getUploadUser() {
        SingleLiveEvent<User> userSingleLiveEvent = new SingleLiveEvent<>();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User currentUser = s.toObject(User.class);
                    if (currentUser == null)
                        return;
                    String linkUserId = currentUser.getLinkUserId();
                    if (linkUserId == null)
                        return;
                    FirebaseFirestore.getInstance().collection("users").document(linkUserId).get()
                            .addOnSuccessListener(l -> {
                                User linkUser = l.toObject(User.class);
                                if (linkUser != null)
                                    userSingleLiveEvent.setValue(linkUser);
                            });
                });
        return userSingleLiveEvent;
    }

    public SingleLiveEvent<Pair<Pair<String, String>, Boolean>> userShortData() {
        SingleLiveEvent<Pair<Pair<String, String>, Boolean>> userSingleLiveEvent = new SingleLiveEvent<>();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User currentUser = s.toObject(User.class);
                    if (currentUser == null)
                        return;
                    userSingleLiveEvent.setValue(Pair.create(Pair.create(currentUser.getUserId(),
                            currentUser.getLinkUserId()), currentUser.isSupport()));
                });
        return userSingleLiveEvent;
    }
}
