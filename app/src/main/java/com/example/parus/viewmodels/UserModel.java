package com.example.parus.viewmodels;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.repositories.UserRepository;

public class UserModel extends ViewModel {

    public UserModel() {
        super();
    }

    UserRepository repository = UserRepository.getInstance();

    public LiveData<User> getUserData() {
        return repository.userListening();
    }

    public LiveData<User> getUserDataById(String userId, boolean recreateData) {
        return repository.otherUserListening(userId, recreateData);
    }

    public LiveData<User> getUserDataById() {
        return repository.otherUserListening(null, false);
    }

    public void removeLinkObserver(LifecycleOwner owner){
        if (repository.otherUserListening(null, false) != null){
            if (repository.otherUserListening(null, false).hasObservers()){
                repository.otherUserListening(null, false).removeObservers(owner);
                repository.stopListeningOtherUser();
            }
        }
    }

    public LiveData<Boolean> callSupport(){
         return repository.callSupport();
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> getShortUserData() {
        return repository.userShortListening();
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> getSingleUserData() {
        return repository.userShortData();
    }


    public LiveData<User> getSingleLinkUserData() {
        return repository.linkUserSingleData();
    }

    public LiveData<Boolean> setFastAction(int action, String text){
        return repository.setFastAction(action, text);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!repository.userListening().hasObservers()) {
            Log.d("TAGAA", "user clear");
            repository.stopListeningLinkUser();
            repository.stopListeningOtherUser();
            repository.stopListeningUser();
            repository.destroy();
        }
    }
}

