package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.models.Reminder;
import com.example.parus.viewmodels.data.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReminderData extends LiveData<List<Reminder>> {

    private static final String TAG = "user data";

    private ListenerRegistration registration;

    private CollectionReference colRef;
    private String userId;
    private String linkUserId;
    private boolean isSupport;
    private List<Reminder> reminders;

    public ReminderData(String userId, String linkUserId, boolean isSupport) {
        this.userId = userId;
        this.linkUserId = linkUserId;
        this.isSupport = isSupport;
        this.reminders = new ArrayList<>();
        chooseRef();
        setEventListener();
    }

    public ReminderData(String userId) {
        this.userId = userId;
        this.reminders = new ArrayList<>();
        setRef().addOnSuccessListener(s -> {
            setEventListener();
            onActive();
        });
    }

    Task<DocumentSnapshot> setRef() {
        return FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(s -> {
                    User user = s.toObject(User.class);
                    if (user == null)
                        return;
                    this.linkUserId = user.getLinkUserId();
                    this.isSupport = user.isSupport();
                    chooseRef();
                });
    }

    private void chooseRef(){
        colRef = null;
        if (!isSupport)
            colRef = FirebaseFirestore.getInstance().collection("users").document(userId).collection("alerts");
        else if (!userId.equals(linkUserId))
            colRef = FirebaseFirestore.getInstance().collection("users").document(linkUserId).collection("alerts");
    }

    EventListener<QuerySnapshot> eventListener;

    private void setEventListener() {
        eventListener = (queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.i(TAG, "Listen failed.", e);
                return;
            }
            if (queryDocumentSnapshots != null)
                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    Reminder reminder = dc.getDocument().toObject(Reminder.class);
                    switch (dc.getType()) {
                        case ADDED:
                            reminder.setId(dc.getDocument().getId());
                            for (Reminder rem : reminders) {
                                if (rem.getId().equals(reminder.getId()))
                                    return;
                            }
                            Log.d(TAG, "add snapshot");
                            if (reminder.getTimers() != null || reminder.getTimeInterval() != null) {
                                reminders.add(reminder);
                                Log.d(TAG, reminder.getId());
                                Collections.sort(reminders, (c1, c2) -> c1.getTimeCreate().compareTo(c2.getTimeCreate()));
                            }
                            break;
                        case MODIFIED:
                            reminder.setId(dc.getDocument().getId());
                            for (Reminder rem : reminders) {
                                if (rem.getId().equals(reminder.getId())) {
                                    reminders.set(reminders.indexOf(rem), reminder);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            if (reminders.size() > 0) {
                                for (Reminder rem : reminders) {
                                    Log.d(TAG, rem.getName());
                                    if (rem.getId().equals(dc.getDocument().getId())) {
                                        reminders.remove(rem);
                                        break;
                                    }
                                }
                            }
                            break;
                    }
                }
            setValue(reminders);
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (colRef != null)
            registration = colRef.addSnapshotListener(eventListener);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (!hasActiveObservers() && registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
