package com.example.parus.viewmodels.repositories;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.parus.viewmodels.data.binding.HomeData;
import com.example.parus.viewmodels.data.models.Reminder;
import com.example.parus.viewmodels.data.ReminderData;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ReminderRepository {

    private String userId;

    public ReminderRepository() {
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    ReminderData reminderData;

    public LiveData<List<Reminder>> productListening(String userId, String linkUserId, boolean isSupport) {
        reminderData = new ReminderData(userId, linkUserId, isSupport);
        return reminderData;
    }

    public LiveData<List<Reminder>> productListening() {
        reminderData = new ReminderData(userId);
        return reminderData;
    }

    private MutableLiveData<Boolean> delete = new MutableLiveData<>();

    public MutableLiveData<Boolean> deleteReminders(List<Reminder> deletingReminders) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s->{
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    CollectionReference colRef = null;
                    if (user.isSupport()){
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        for (Reminder reminder : deletingReminders) {
                            colRef.document(reminder.getId()).delete();
                        }
                        delete.setValue(true);
                    }
                });
        return delete;
    }

    private MutableLiveData<Boolean> add = new MutableLiveData<>();

    public MutableLiveData<Boolean> addReminder(HashMap<String, Object> hashMap){
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s->{
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    CollectionReference colRef = null;
                    if (user.isSupport()){
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        colRef.add(hashMap).addOnSuccessListener(t->add.setValue(true));
                    }
                });
        return add;
    }

    private MutableLiveData<Boolean> change = new MutableLiveData<>();

    public MutableLiveData<Boolean> changeReminder(String docId, HashMap<String, Object> hashMap){
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s->{
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    CollectionReference colRef = null;
                    if (user.isSupport()){
                        if (!userId.equals(user.getLinkUserId()))
                            colRef = FirebaseFirestore.getInstance().collection("users").document(user.getLinkUserId()).collection("alerts");
                    } else {
                        colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
                    }
                    if (colRef != null) {
                        colRef.document(docId).update(hashMap).addOnSuccessListener(t->change.setValue(true));
                    }
                });
        return change;
    }

}


