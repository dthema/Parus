package com.begletsov.parus.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.repositories.SayRepository;

import java.util.List;

public class SayViewModel extends ViewModel {

    private final SayRepository repository = SayRepository.getInstance();

    public SayViewModel() {
        super();
    }

    public LiveData<List<String>> getOftenWords() {
        return repository.getOftenWordsData();
    }

    public LiveData<List<String>> getCollections() {
        return repository.getCollectionData();
    }

    public void addOftenWord(String word) {
        repository.addOftenWord(word);
    }

    public void addCollection(String collectionName) {
        repository.addCollection(collectionName);
    }

    public void deleteCollections(String[] collectionName) {
        repository.deleteCollections(collectionName);
    }

    public void addCollectionWord(String collectionName, String word) {
        repository.addCollectionWord(collectionName, word);
    }

    public void deleteCollectionWord(String collectionName, String[] wordsArray) {
        repository.deleteCollectionWord(collectionName, wordsArray);
    }

    public List<String> getCollectionWords(String collectionName) {
        return repository.getCollectionWords(collectionName);
    }

    public String[] getCollectionsString() {
        return repository.getCollectionsString();
    }

    public void setSaySettings(Double TTS_Speed, Double TTS_Pitch, Long Column_Count) {
        repository.setSaySettings(TTS_Speed, TTS_Pitch, Column_Count);
    }

    public LiveData<Object[]> getSettingsLiveData(){
        return repository.getSettingsLiveData();
    }

    public Object[] getSettings(){
        return repository.getSettings();
    }

    private boolean hasObservers(){
        return repository.getCollectionData().hasObservers() && repository.getOftenWordsData().hasObservers() && repository.getSettingsLiveData().hasObservers();
    }

    @Override
    protected void onCleared() {
        if (!hasObservers())
            repository.destroy();
        super.onCleared();
    }
}