package com.example.parus.viewmodels.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.models.Chat;
import com.example.parus.viewmodels.data.models.User;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MessageData extends LiveData<List<Chat>> {

    private static final String TAG = "user data";
    private ListenerRegistration registration;
    private CollectionReference collectionReference;
    private List<Chat> chats;
    private List<String> times;

    public MessageData(CollectionReference docRef) {
        this.collectionReference = docRef;
        chats = new ArrayList<>();
        times = new ArrayList<>();
    }

    private EventListener<QuerySnapshot> eventListener = (queryDocumentSnapshots, e) -> {
        if (e != null) {
            Log.i(TAG, "Listen failed.", e);
            return;
        }
        if (queryDocumentSnapshots != null) {
            for (DocumentChange document : queryDocumentSnapshots.getDocumentChanges()) {
                switch (document.getType()) {
                    case ADDED:
                        Chat chat = document.getDocument().toObject(Chat.class);
                        chat.setId(document.getDocument().getId());
                        for (Chat c : chats) {
                            if (c.getId().equals(chat.getId())) {
                                return;
                            }
                        }
                        chat.setCalendar(false);
//                        if (chat.getMessage().substring(chat.getMessage().length()-1).equals("\n"))
                            chat.setMessage(chat.getMessage().trim());
                        chats.add(chat);
                        if (chats.size() > 0) {
                            boolean flag = true;
                            for (String s : times) {
                                if (s.equals(chat.getDate().toString().substring(0, 11) + chat.getDate().toString().substring(30))) {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                times.add(chat.getDate().toString().substring(0, 11) + chat.getDate().toString().substring(30));
                                Chat time = new Chat();
                                time.setId(chat.getDate().toString().substring(0, 11) + chat.getDate().toString().substring(30));
                                time.setCalendar(true);
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(chat.getDate());
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                time.setDate(calendar.getTime());
                                chats.add(time);
                            }
                        }
                        Collections.sort(chats, (c1, c2) -> Long.compare(c1.getDate().getTime(), c2.getDate().getTime()));
                        break;
                    case REMOVED:
                        chats.clear();
                        break;
                }
            }
            setValue(chats);
        }
    };

    @Override
    protected void onActive() {
        super.onActive();
        registration = collectionReference.addSnapshotListener(eventListener);
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
