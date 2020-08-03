package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.parus.services.OnlineService;
import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.User;
import com.example.parus.viewmodels.data.UserData;
import com.example.parus.viewmodels.data.UserLinkData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class UserRepository {

    String userId;

    public UserRepository() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public LiveData<User> userListening() {
        return new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public LiveData<User> userListening(String userId) {
        return new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public void startCheckOnline(Context context) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    if (s != null) {
                        User user = s.toObject(User.class);
                        if (user == null)
                            return;
                        if (!user.getUserId().equals(user.getLinkUserId()) && !OnlineService.isServiceRunning) {
                            Intent intent = new Intent(context, OnlineService.class).setAction("action");
                            intent.putExtra("uid", userId);
                            context.startService(intent);

                        }
                    }
                });
    }

    private MutableLiveData<Boolean> callSupport = new MutableLiveData<>();

    public void callSupport(Context context) {
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
                        Toast.makeText(context, "Уведомление отправлено помощнику", Toast.LENGTH_LONG).show();
                    }
                });
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> userLinkListening() {
        return new UserLinkData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    private SingleLiveEvent<User> userSingleLiveEvent = new SingleLiveEvent<>();

    public SingleLiveEvent<User> getUploadUser() {
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
}
