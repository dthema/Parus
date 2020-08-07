package com.example.parus.viewmodels;

import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.models.User;
import com.example.parus.viewmodels.repositories.UserRepository;

public class UserModel extends ViewModel {

    public UserModel() {
        super();
    }

    UserRepository repository = new UserRepository();

    public LiveData<User> getUserData() {
        return repository.userListening();
    }

    LiveData<User> linkUserData;

    public LiveData<User> getUserDataById(String userId) {
        linkUserData = repository.userListening(userId);
        return linkUserData;
    }

    public LiveData<User> getUserDataById() {
        return linkUserData;
    }

    public void removeLinkObserver(LifecycleOwner owner){
        if (linkUserData != null){
            if (linkUserData.hasObservers()){
                linkUserData.removeObservers(owner);
            }
        }
    }

    public SingleLiveEvent<Boolean> callSupport(){
         return repository.callSupport();
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> getShortLinkUserData() {
        return repository.userLinkListening();
    }

    public SingleLiveEvent<Pair<Pair<String, String>, Boolean>> getSingleUserData() {
        return repository.userShortData();
    }


    public SingleLiveEvent<User> getUploadLinkUser() {
        return repository.getUploadUser();
    }

}

