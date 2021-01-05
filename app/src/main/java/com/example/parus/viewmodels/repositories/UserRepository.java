package com.example.parus.viewmodels.repositories;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.data.UserData;
import com.example.parus.viewmodels.data.UserShortData;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class UserRepository {

    private static UserRepository repository;
    private final String userId;

    private UserRepository() {
        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    public synchronized static UserRepository getInstance() {
        if (repository == null) repository = new UserRepository();
        return repository;
    }

    private LiveData<User> userLiveData;

    public LiveData<User> userListening() {
        if (userLiveData == null)
            userLiveData = new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
        return userLiveData;
    }

    public void stopListeningUser() {
        userLiveData = null;
    }

    private LiveData<User> otherUserLiveData;

    public LiveData<User> otherUserListening(String userId, boolean recreateData) {
        if (otherUserLiveData == null && recreateData)
            otherUserLiveData = new UserData(FirebaseFirestore.getInstance().collection("users").document(userId));
        return otherUserLiveData;
    }

    public void stopListeningOtherUser() {
        otherUserLiveData = null;
    }

    private LiveData<Pair<Pair<String, String>, Boolean>> shortLiveData;

    public LiveData<Pair<Pair<String, String>, Boolean>> userShortListening() {
        if (shortLiveData == null)
            shortLiveData = new UserShortData(FirebaseFirestore.getInstance().collection("users").document(userId));
        return shortLiveData;
    }

    public void stopListeningShortUser() {
        shortLiveData = null;
    }

    public void destroy() {
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

    public LiveData<Pair<Pair<String, String>, Boolean>> userShortSingleData() {
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

    public LiveData<User> userSingleData() {
        SingleLiveEvent<User> userSingleLiveEvent = new SingleLiveEvent<>();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User currentUser = s.toObject(User.class);
                    if (currentUser != null)
                        userSingleLiveEvent.setValue(currentUser);
                });
        return userSingleLiveEvent;
    }

    public void removeLinkUser() {
        if (otherUserLiveData != null)
            if (otherUserLiveData.getValue() != null) {
                FirebaseFirestore.getInstance().collection("users").document(otherUserLiveData.getValue().getUserId())
                        .update("linkUserId", otherUserLiveData.getValue().getUserId());
                FirebaseFirestore.getInstance().collection("users").document(userId).update("linkUserId", userId);
            }
    }

    public LiveData<String> setLinkUser(String linkUserId, boolean isSupport) {
        SingleLiveEvent<String> liveEvent = new SingleLiveEvent<>();
        if (linkUserId.isEmpty()) {
            liveEvent.setValue("Вы не ввели ID");
            return liveEvent;
        } else {
            FirebaseFirestore.getInstance().collection("users").document(linkUserId).get()
                    .addOnSuccessListener(u -> {
                        if (u == null)
                            return;
                        User linkUser = u.toObject(User.class);
                        if (linkUser == null) {
                            liveEvent.setValue("Пользователь с таким ID не найден");
                            return;
                        }
                        if (isSupport != linkUser.isSupport()) {
                            if (linkUser.getLinkUserId().equals(linkUserId)) {
                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                        .update("linkUserId", linkUserId)
                                        .addOnSuccessListener(s ->
                                                FirebaseFirestore.getInstance().collection("users").document(linkUserId)
                                                        .update("linkUserId", userId)
                                                        .addOnSuccessListener(c -> liveEvent.setValue("1"))
                                                        .addOnFailureListener(e -> {
                                                            liveEvent.setValue("Произошла ошибка");
                                                            e.printStackTrace();
                                                        }))
                                        .addOnFailureListener(f -> {
                                            liveEvent.setValue("Произошла ошибка");
                                            f.printStackTrace();
                                        });
                            } else
                                liveEvent.setValue("Ошибка: У пользователя уже есть связь с другим пользователем");
                        } else
                            liveEvent.setValue("Ошибка: Ваша роль индентична роли пользователя");
                    });
        }
        return liveEvent;
    }

    public LiveData<String> resetEmail(String password, String newEmail) {
        SingleLiveEvent<String> liveEvent = new SingleLiveEvent<>();
        if (newEmail.isEmpty() || password.isEmpty())
            liveEvent.setValue("Поля не заполнены");
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                liveEvent.setValue("Произошла ошибка");
                return liveEvent;
            }
            AuthCredential credential = EmailAuthProvider
                    .getCredential(Objects.requireNonNull(user.getEmail()), password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updateEmail(newEmail)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful())
                                            liveEvent.setValue("Почта изменена");
                                        else
                                            liveEvent.setValue("Произошла ошибка");
                                    });
                        } else
                            liveEvent.setValue("Данные введены неверно");
                    });
        }
        return liveEvent;
    }

    public LiveData<String> resetPassword(String oldPassword, String newPassword) {
        SingleLiveEvent<String> liveEvent = new SingleLiveEvent<>();
        if (oldPassword.isEmpty() || newPassword.isEmpty())
            liveEvent.setValue("Поля не заполнены");
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                liveEvent.setValue("Произошла ошибка");
                return liveEvent;
            }
            AuthCredential credential = EmailAuthProvider
                    .getCredential(Objects.requireNonNull(user.getEmail()), oldPassword);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful())
                                            liveEvent.setValue("Пароль изменён");
                                        else
                                            liveEvent.setValue("Произошла ошибка");
                                    });
                        } else
                            liveEvent.setValue("Данные введены неверно");
                    });
        }
        return liveEvent;
    }

    public void exit() {
        FirebaseFirestore.getInstance().collection("users").document(userId).update("token", "");
        FirebaseAuth.getInstance().signOut();
    }

    public LiveData<String> delete(String email, String password) {
        SingleLiveEvent<String> liveEvent = new SingleLiveEvent<>();
        if (email.isEmpty() || password.isEmpty())
            liveEvent.setValue("Поля не заполнены");
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                liveEvent.setValue("Произошла ошибка");
                return liveEvent;
            }
            AuthCredential credential = EmailAuthProvider
                    .getCredential(email, password);
            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (userLiveData.getValue() != null) {
                                if (!userLiveData.getValue().getUserId().equals(userLiveData.getValue().getLinkUserId())) {
                                    FirebaseFirestore.getInstance().collection("users")
                                            .document(userLiveData.getValue().getLinkUserId())
                                            .update("linkUserId", userLiveData.getValue().getLinkUserId())
                                            .addOnSuccessListener(s -> delete(liveEvent, user))
                                            .addOnFailureListener(f -> liveEvent.setValue("Произошла ошибка"));
                                } else delete(liveEvent, user);
                            } else delete(liveEvent, user);
                        } else liveEvent.setValue("Данные введены неверно");
                    });
        }
        return liveEvent;
    }

    private void delete(SingleLiveEvent<String> liveEvent, FirebaseUser user) {
        FirebaseFirestore.getInstance().collection("users").document(userId).delete()
                .addOnSuccessListener(s -> user.delete()
                        .addOnSuccessListener(s1 -> liveEvent.setValue("1"))
                        .addOnFailureListener(f1 -> liveEvent.setValue("Произошла ошибка")))
                .addOnFailureListener(f -> liveEvent.setValue("Произошла ошибка"));
    }

    public LiveData<Boolean> setSupport() {
        SingleLiveEvent<Boolean> liveEvent = new SingleLiveEvent<>();
        FirebaseFirestore.getInstance().collection("users").document(userId).update("support", true)
                .addOnSuccessListener(s -> liveEvent.setValue(true))
                .addOnFailureListener(f -> liveEvent.setValue(false));
        return liveEvent;
    }

    public LiveData<Boolean> setFastAction(int action, String text) {
        SingleLiveEvent<Boolean> userSingleLiveEvent = new SingleLiveEvent<>();
        if (action <= 3)
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fastAction", String.valueOf(action))
                    .addOnSuccessListener(s -> userSingleLiveEvent.setValue(true));
        else if (text.length() > 0)
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fastAction", text)
                    .addOnSuccessListener(s -> userSingleLiveEvent.setValue(true));
        else
            userSingleLiveEvent.setValue(false);
        return userSingleLiveEvent;
    }

    public LiveData<Boolean> setName(String name) {
        SingleLiveEvent<Boolean> liveEvent = new SingleLiveEvent<>();
        FirebaseFirestore.getInstance().collection("users").document(userId).update("name", name)
                .addOnSuccessListener(s -> liveEvent.setValue(true))
                .addOnFailureListener(f -> liveEvent.setValue(false));
        return liveEvent;
    }
}
