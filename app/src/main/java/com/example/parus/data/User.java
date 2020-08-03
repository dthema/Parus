package com.example.parus.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class User implements Parcelable {

    private FirebaseUser user;
    private FirebaseFirestore db;
    private HashMap<String, Object> words;
    private HashMap<String, Object> collections;
    private HashMap<String, Object> SaySettings;
    private String TAG_O = "words";
    private String TAG_C = "collections";
    private String linkUserId;
    private boolean isSupport;


    protected User(Parcel in) {
        user = in.readParcelable(FirebaseUser.class.getClassLoader());
        TAG_O = in.readString();
        TAG_C = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> updateOftenWordsMap() {
        return db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.getId().equals(user.getUid())) {
                                words = (HashMap<String, Object>) document.get("WordsOften");
                                if (words != null)
                                    Log.d(TAG_O + "_update", words.toString());
                            }
                        }
                    } else {
                        Log.d(TAG_O, "Error getting documents: ", task.getException());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> updateCollectionsMap() {
        return db.collection("users").document(user.getUid()).collection("Say").document("Collections")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                                collections = (HashMap<String, Object>) Objects.requireNonNull(task.getResult()).get("Collections");
                                if (collections != null)
                                    Log.d(TAG_C + "_update", collections.toString());
                    } else {
                        Log.d(TAG_C, "Error getting documents: ", task.getException());
                    }
                });
    }

    public Task<DocumentSnapshot> updateCollection() {
        return updateCollectionsMap();
    }

    public Task<QuerySnapshot> updateOftenWords() {
        return updateOftenWordsMap();
    }

    public User() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        words = new HashMap<>();
        collections = new HashMap<>();
        Log.d(TAG_O + "_created", words.toString());
        Log.d(TAG_C + "_created", collections.toString());
    }

    public void addCollection(String collectionName) {
        if (collections != null) {
            collectionName = collectionName.trim().replace("\n", " ");
            if (collections.containsKey(collectionName)) {
                Log.d(TAG_C, "not new");
            } else {
                collections.put(collectionName, new ArrayList<>());
                Log.d(TAG_C, collections.toString());
                Log.d(TAG_C, collectionName);
            }
        } else {
            collections = new HashMap<>();
            collections.put(collectionName, new ArrayList<>());
            Log.d(TAG_C, collections.toString());
            Log.d(TAG_C, "new");
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("Collections", collections);
        db.collection("users").document(user.getUid()).collection("Say").document("Collections").set(userData);
    }

    public Task<Void> update(String field, boolean value) {
        return db.collection("users").document(user.getUid()).update(field, value);
    }

    public Task<Void> update(String field, int value) {
        return db.collection("users").document(user.getUid()).update(field, value);
    }

    public Task<Void> update(String field, Object value) {
        return db.collection("users").document(user.getUid()).update(field, value);
    }

    public Task<Void> update(Map<String, Object> value) {
        return db.collection("users").document(user.getUid()).update(value);
    }

    public Task<Void> update(String uId, String field, String value) {
        return db.collection("users").document(uId).update(field, value);
    }

    public Task<QuerySnapshot> getUsers() {
        return db.collection("users").get();
    }

    public void addWord(String word){
        updateOftenWordsMap();
        if (words != null) {
            if (words.containsKey(word)) {
                words.put(word, Integer.parseInt(Objects.requireNonNull(words.get(word)).toString())+1);
                Log.d(TAG_O, Objects.requireNonNull(words.get(word)).toString());
                Log.d(TAG_O, "not new");
            }
            else {
                words.put(word, 1);
            }
        } else {
            words = new HashMap<>();
            words.put(word, 1);
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("WordsOften", words);
        update(userData);
    }

    @SuppressWarnings("unchecked")
    public void addCollectionWord(String collectionName, String word){
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            assert collectionWords != null;
            Log.d(TAG_C+"_addWord", collectionWords.toString());
            collectionWords.add(word);
            collections.put(collectionName, collectionWords);
            Map<String, Object> userData = new HashMap<>();
            userData.put("Collections", collections);
            db.collection("users").document(user.getUid()).collection("Say").document("Collections").update(userData);
        }
    }

    @SuppressWarnings("unchecked")
    public void deleteCollectionWord(String collectionName, String[] wordsArray){
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            for (String word : wordsArray) {
                assert collectionWords != null;
                if (collectionWords.indexOf(word) >= 0) {
                    Log.d(TAG_C + "_deleteWord", collectionWords.toString());
                    collectionWords.remove(collectionWords.get(collectionWords.indexOf(word)));
                } else
                    return;
            }
            collections.put(collectionName, collectionWords);
            Map<String, Object> userData = new HashMap<>();
            userData.put("Collections", collections);
            db.collection("users").document(user.getUid()).collection("Say").document("Collections").update(userData);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getCollectionWords(String collectionName){
        List<String> list = new LinkedList<>();
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            assert collectionWords != null;
            if (collectionWords.isEmpty())
                return null;
            for (Object s : Objects.requireNonNull(collectionWords.toArray())){
                list.add(s.toString());
            }
            return list;
        } else {
            return null;
        }
    }

    public FirebaseUser getUser(){
        return user;
    }

    public void singOut(){
        FirebaseAuth.getInstance().signOut();
    }

    public List<String> getWords() {
        if (words == null)
            return new LinkedList<>();
        Set<Map.Entry<String, Object>> entries = words.entrySet();
        Comparator<Map.Entry<String, Object>> valueComparator = (o1, o2) -> Integer.valueOf(o1.getValue().toString()).compareTo(Integer.valueOf(o2.getValue().toString()));
        List<Map.Entry<String, Object>> listOfEntries = new ArrayList<>(entries);
        Collections.sort(listOfEntries, valueComparator);
        List<String> sortedByValue = new LinkedList<>();
        for(Map.Entry<String, Object> entry : listOfEntries) {
            sortedByValue.add(entry.getKey());
        }
        Log.d(TAG_O +"_list", sortedByValue.toString());
        return sortedByValue;
    }

    public void deleteCollections(String[] collectionName){
        for (String str : collectionName){
            if (collections.containsKey(str)){
                collections.remove(str);
                Log.d(TAG_C+"_delete", str);
            }
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("Collections", collections);
        db.collection("users").document(user.getUid()).collection("Say").document("Collections").set(userData);
    }

    public List<String> getCollections() {
        if (collections == null)
            return new LinkedList<>();
        List<String> list = new LinkedList<>(collections.keySet());
        Log.d(TAG_C +"_list", list.toString());
        return list;
    }

    public String[] getCollectionsString() {
        if (collections == null)
            return new String[0];
        List<String> list = new LinkedList<>(collections.keySet());
        String[] collectionsString = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            collectionsString[i] = list.get(i);
        }
        Log.d(TAG_C +"_string", list.toString());
        return collectionsString;
    }

    public boolean isSupport() {
        return isSupport;
    }

    public Task<QuerySnapshot> updateIsSupport(){
        return db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.getId().equals(user.getUid())) {
                                isSupport = (boolean) document.get("isSupport");
                            }
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public Task<QuerySnapshot> updateSaySettings(){
        return db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.getId().equals(user.getUid())) {
                                SaySettings = (HashMap<String, Object>) document.get("SaySettings");
                            }
                        }
                    }
                });
    }

    public FirebaseFirestore getDatabase() {
        return db;
    }

    public Task<DocumentReference> addMessage(HashMap<String, Object> messageMap, boolean fromSupport){
        if (fromSupport) {
            return db.collection("chats").document(linkUserId + user.getUid()).collection("chat").add(messageMap);
        } else {
            return db.collection("chats").document(user.getUid() + linkUserId).collection("chat").add(messageMap);
        }
    }

    public Task<QuerySnapshot> updateLinkUser(){
        return db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            if (document.getId().equals(user.getUid())) {
                                linkUserId = (String) document.get("linkUserId");
                            }
                        }
                    }
                });
    }

    public String getLinkUserId() {
        return linkUserId;
    }

    public Object[] getSettings(){
        Object[] arr = new Object[SaySettings.size()];
        arr[0] = SaySettings.get("TTS_Speed");
        arr[1] = SaySettings.get("TTS_Pitch");
        arr[2] = SaySettings.get("Column_Count");
        return arr;
    }

    public Task<Void> setSaySettings(Double TTS_Speed, Double TTS_Pitch, Long Column_Count){
        Map<String, Object> userData = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        map.put("TTS_Speed", TTS_Speed);
        map.put("TTS_Pitch", TTS_Pitch);
        map.put("Column_Count", Column_Count);
        userData.put("SaySettings", map);
        return update(userData);
    }

    @Override
    public int describeContents() {
        return CONTENTS_FILE_DESCRIPTOR;
    }

    @SuppressLint("Recycle")
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        HashMap[] maps = new HashMap[2];
        maps[0] = collections;
        maps[1] = SaySettings;
        Parcel.obtain().writeArray(maps);
    }
}
