package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.data.User;
import com.example.parus.viewmodels.repositories.UserRepository;

public class UserModel extends AndroidViewModel {

    public UserModel(@NonNull Application application) {
        super(application);
    }

    UserRepository repository = new UserRepository();

    public LiveData<User> getUserData() {
        return repository.userListening();
    }

    LiveData<User> linkUserData;

    public LiveData<User> getLinkUserData(String userId) {
        linkUserData = repository.userListening(userId);
        return linkUserData;
    }

    public LiveData<User> getLinkUserData() {
        return linkUserData;
    }

    public void removeLinkObserver(LifecycleOwner owner){
        if (linkUserData != null){
            if (linkUserData.hasObservers()){
                linkUserData.removeObservers(owner);
            }
        }
    }

    public void startCheckOnline(){
        repository.startCheckOnline(getApplication());
    }

    public void callSupport(){
        repository.callSupport(getApplication());
    }

    public LiveData<Pair<Pair<String, String>, Boolean>> getShortUserData() {
        return repository.userLinkListening();
    }

    private SingleLiveEvent<User> uploadUser = new SingleLiveEvent<>();

    public SingleLiveEvent<User> getUploadLinkUser() {
        uploadUser = repository.getUploadUser();
        return uploadUser;
    }

    public void onUserToUpload(User user) {
        uploadUser.setValue(user);
    }
}
