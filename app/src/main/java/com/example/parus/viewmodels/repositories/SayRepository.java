package com.example.parus.viewmodels.repositories;

import android.util.Log;
import android.util.Pair;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.parus.data.User;
import com.example.parus.viewmodels.data.OftenWordsData;
import com.example.parus.viewmodels.data.SayCollectionData;
import com.example.parus.viewmodels.data.SaySettingsData;
import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SayRepository {

    private static SayRepository repository;
    private String userId;

    private SayRepository() {
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        wordsData = new OftenWordsData(FirebaseFirestore.getInstance().collection("users").
                document(userId).collection("Say").document("OftenWords"));
        wordsStringData = Transformations.map(wordsData, hashMap -> {
            Set<Map.Entry<String, Object>> entries = hashMap.entrySet();
            Comparator<Map.Entry<String, Object>> valueComparator = (o1, o2) ->
                    Integer.valueOf(o1.getValue().toString()).compareTo(Integer.valueOf(o2.getValue().toString()));
            List<Map.Entry<String, Object>> listOfEntries = new ArrayList<>(entries);
            Collections.sort(listOfEntries, valueComparator);
            List<String> sortedByValue = new ArrayList<>();
            for (Map.Entry<String, Object> entry : listOfEntries) {
                sortedByValue.add(entry.getKey());
            }
            return sortedByValue;
        });
        collectionData = new SayCollectionData(FirebaseFirestore.getInstance().collection("users")
                .document(userId).collection("Say").document("Collections"));
        collectionStringData = Transformations.map(collectionData, hashmap -> new ArrayList<>(hashmap.keySet()));
        settingsData = new SaySettingsData(FirebaseFirestore.getInstance().collection("users").document(userId));
    }

    public synchronized static SayRepository getInstance() {
        if (repository == null)
            repository = new SayRepository();
        return repository;
    }

    private SayCollectionData collectionData;
    private LiveData<List<String>> collectionStringData;

    public LiveData<List<String>> getCollectionData() {
        return collectionStringData;
    }

    private OftenWordsData wordsData;
    private LiveData<List<String>> wordsStringData;

    public LiveData<List<String>> getOftenWordsData() {
        return wordsStringData;
    }

    public void addOftenWord(String word) {
        HashMap<String, Object> words = wordsData.getValue();
//        int cnt = 1;
//        if (words != null) {
//            if (words.containsKey(word))
//                cnt += Integer.parseInt(Objects.requireNonNull(words.get(word)).toString());
//        } else
//            words = new HashMap<>();
//        words.put(word, cnt);
        if (words != null) {
            if (words.containsKey(word)) {
                words.put(word, Integer.parseInt(Objects.requireNonNull(words.get(word)).toString()) + 1);
            } else {
                words.put(word, 1);
            }
        } else {
            words = new HashMap<>();
            words.put(word, 1);
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("OftenWords", words);
        FirebaseFirestore.getInstance().collection("users").
                document(userId).collection("Say").document("OftenWords").set(userData);
    }

    public void addCollection(String collectionName) {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections != null) {
            collectionName = collectionName.trim().replace("\n", " ");
            if (!collections.containsKey(collectionName))
                collections.put(collectionName, new ArrayList<>());
        } else {
            collections = new HashMap<>();
            collections.put(collectionName, new ArrayList<>());
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("Collections", collections);
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("Say")
                .document("Collections").set(userData);
    }

    public void deleteCollections(String[] collectionName) {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections == null)
            return;
        for (String str : collectionName)
            collections.remove(str);
        Map<String, Object> userData = new HashMap<>();
        userData.put("Collections", collections);
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("Say")
                .document("Collections").update(userData);
    }

    public void addCollectionWord(String collectionName, String word) {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections == null)
            return;
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            assert collectionWords != null;
            collectionWords.add(word);
            collections.put(collectionName, collectionWords);
            Map<String, Object> userData = new HashMap<>();
            userData.put("Collections", collections);
            FirebaseFirestore.getInstance().collection("users").document(userId).collection("Say")
                    .document("Collections").update(userData);
        }
    }

    public void deleteCollectionWord(String collectionName, String[] wordsArray) {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections == null)
            return;
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            for (String word : wordsArray) {
                assert collectionWords != null;
                if (collectionWords.indexOf(word) >= 0)
                    collectionWords.remove(collectionWords.get(collectionWords.indexOf(word)));
                else
                    return;
            }
            collections.put(collectionName, collectionWords);
            Map<String, Object> userData = new HashMap<>();
            userData.put("Collections", collections);
            FirebaseFirestore.getInstance().collection("users").document(userId).collection("Say")
                    .document("Collections").update(userData);
        }
    }

    public List<String> getCollectionWords(String collectionName) {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections == null)
            return null;
        List<String> list = new ArrayList<>();
        ArrayList<Object> collectionWords;
        if (collections.containsKey(collectionName)) {
            collectionWords = (ArrayList<Object>) collections.get(collectionName);
            assert collectionWords != null;
            if (collectionWords.isEmpty())
                return null;
            for (Object s : Objects.requireNonNull(collectionWords.toArray()))
                list.add(s.toString());
            return list;
        } else {
            return null;
        }
    }

    public String[] getCollectionsString() {
        HashMap<String, Object> collections = collectionData.getValue();
        if (collections == null)
            return new String[0];
        List<String> list = new ArrayList<>(collections.keySet());
        String[] collectionsString = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            collectionsString[i] = list.get(i);
        }
        return collectionsString;
    }

    public void setSaySettings(Double TTS_Speed, Double TTS_Pitch, Long Column_Count) {
        Map<String, Object> userData = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        map.put("TTS_Speed", TTS_Speed);
        map.put("TTS_Pitch", TTS_Pitch);
        map.put("Column_Count", Column_Count);
        userData.put("SaySettings", map);
        FirebaseFirestore.getInstance().collection("users").document(userId).update(userData);
    }

    private SaySettingsData settingsData;

    public LiveData<Object[]> getSettingsLiveData() {
        return settingsData;
    }

    public Object[] getSettings(){
        return settingsData.getValue();
    }

    public void destroy() {
        repository = null;
    }
}
