package com.begletsov.parus.viewmodels.repositories;

import androidx.lifecycle.LiveData;

import com.begletsov.parus.RequestTime;
import com.begletsov.parus.viewmodels.data.MessageData;
import com.begletsov.parus.viewmodels.data.models.Chat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ChatRepository {

    private final String userId;
    private String linkUserId;
    private CollectionReference reference;

    public ChatRepository() {
        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    public void setLinkUserId(String linkUserId, boolean isSupport){
        this.linkUserId = linkUserId;
        if (linkUserId == null)
            messageData = null;
        else if (isSupport)
            reference = FirebaseFirestore.getInstance().collection("chats").document(linkUserId + userId)
                    .collection("chat");
        else
            reference = FirebaseFirestore.getInstance().collection("chats").document(userId + linkUserId)
                    .collection("chat");
        if (reference != null)
            messageData = new MessageData(reference);
    }

    private MessageData messageData;

    public LiveData<List<Chat>> getMessageData() {
        return messageData;
    }

    public void sendMessage(String message, boolean fromSupport) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", userId);
        hashMap.put("receiver", linkUserId);
        hashMap.put("message", message);
        hashMap.put("fromSupport", fromSupport);
        hashMap.put("date", Calendar.getInstance().getTime());
        new Thread(() -> {
            try {
                Timestamp date = new RequestTime().execute().get();
                if (date != null) {
                    hashMap.remove("date");
                    hashMap.put("date", date.toDate());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (fromSupport)
                    reference.add(hashMap);
                else
                    reference.add(hashMap);
            }
        }).start();
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if (flag.get())
//                    FirebaseFirestore.getInstance().collection("chats").document(userId + linkUserId)
//                            .collection("chat").add(hashMap);
//            }
//        }, 2000);
    }
}
