package com.begletsov.parus.viewmodels;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.data.models.User;
import com.begletsov.parus.viewmodels.repositories.UserRepository;

public class UserViewModel extends ViewModel {

    public UserViewModel() {
        super();
    }

    private final UserRepository repository = UserRepository.getInstance();

    public LiveData<User> getUserData() {
        return repository.userListening();
    }

    public LiveData<User> getOtherUserData(String userId, boolean recreateData) {
        return repository.otherUserListening(userId, recreateData);
    }

    public LiveData<User> getOtherUserData() {
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

    public LiveData<Pair<Pair<String, String>, Boolean>> getSingleShortUserData() {
        return repository.userShortSingleData();
    }

    public LiveData<User> getSingleLinkUserData() {
        return repository.linkUserSingleData();
    }

    public LiveData<User> getSingleUserData() {
        return repository.userSingleData();
    }

    public void removeLinkUser() {
        repository.removeLinkUser();
    }

    public LiveData<String> setLinkUser(String linkUserId, boolean isSupport){
        return repository.setLinkUser(linkUserId, isSupport);
    }

    public LiveData<Boolean> setFastAction(int action, String text){
        return repository.setFastAction(action, text);
    }

    public LiveData<String> resetEmail(String password, String newEmail){
        return repository.resetEmail(password, newEmail);
    }

    public LiveData<String> resetPassword(String oldPassword, String newPassword){
        return repository.resetPassword(oldPassword, newPassword);
    }

    public void exit(){
        repository.exit();
    }

    public LiveData<String> delete(String email, String password){
        return repository.delete(email, password);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!repository.userListening().hasObservers()) {
            Log.d("TAGAA", "user clear");
            repository.stopListeningShortUser();
            repository.stopListeningOtherUser();
            repository.stopListeningUser();
            repository.destroy();
        }
    }

    public LiveData<Boolean> setSupport() {
        return repository.setSupport();
    }

    public LiveData<Boolean> setName(String name) {
        return repository.setName(name);
    }
}

