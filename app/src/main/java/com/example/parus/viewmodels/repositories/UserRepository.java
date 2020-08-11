package com.example.parus.viewmodels.repositories;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.data.UserData;
import com.example.parus.viewmodels.data.UserShortData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class UserRepository {

    private static UserRepository repository;
    private String userId;

    private UserRepository() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public synchronized static UserRepository getInstance(){
        if (repository == null) repository = new UserRepository();
        return repository;
    }

    private LiveData<User> userLiveData;

    public LiveData<User> userListening() {
        if (userLiveData == null) {
            userLiveData = new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
            Log.d("TAGAA", userId);
        }
        return userLiveData;
    }

    public void stopListeningUser(){
        userLiveData = null;
    }

    private LiveData<User> otherUserLiveData;

    public LiveData<User> otherUserListening(String userId, boolean recreateData) {
        if (otherUserLiveData == null && recreateData)
            otherUserLiveData = new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));;
        return otherUserLiveData;
    }

    public void stopListeningOtherUser(){
        otherUserLiveData = null;
    }

    private LiveData<Pair<Pair<String, String>, Boolean>> shortLiveData;

    public LiveData<Pair<Pair<String, String>, Boolean>> userShortListening() {
        if (shortLiveData == null)
            shortLiveData = new UserShortData(FirebaseFirestore.getInstance().collection("users").document(userId));
        return shortLiveData;
    }

    public void stopListeningLinkUser(){
        shortLiveData = null;
    }

    public void destroy(){
        if (shortLiveData == null && userLiveData == null && otherUserLiveData == null)
            repository = null;
    }

    public LiveData<Boolean> callSupport() {
        SingleLiveEvent<Boolean> callSupport = new SingleLiveEvent<>();
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


    public LiveData<User> linkUserSingleData() {
        SingleLiveEvent<User> userSingleLiveEvent = new SingleLiveEvent<>();
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

    public LiveData<Pair<Pair<String, String>, Boolean>> userShortData() {
        SingleLiveEvent<Pair<Pair<String, String>, Boolean>> userSingleLiveEvent = new SingleLiveEvent<>();
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

    public LiveData<Boolean> setFastAction(int action, String text){
        SingleLiveEvent<Boolean> userSingleLiveEvent = new SingleLiveEvent<>();
        if (action <= 3) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fastAction", String.valueOf(action))
            .addOnSuccessListener(s -> userSingleLiveEvent.setValue(true));
//            dialog.dismiss();
        } else if (text.length() > 0) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fastAction", text)
                    .addOnSuccessListener(s -> userSingleLiveEvent.setValue(true));
//            dialog.dismiss();
        } else {
            userSingleLiveEvent.setValue(false);
            //            Toast.makeText(dialogView.getContext(), "Вы не ввели фразу", Toast.LENGTH_LONG).show();
        }
        return userSingleLiveEvent;
    }
}
