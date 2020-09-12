package com.example.parus.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.parus.viewmodels.repositories.LoginRepository;
import com.example.parus.viewmodels.repositories.NetworkRepository;

public class LoginViewModel extends ViewModel {

    public LoginViewModel() {
        super();
    }

    private final LoginRepository repository = new LoginRepository();

    public LiveData<Boolean> register(String email, String password) {
        return repository.register(email, password);
    }

    public LiveData<Boolean> login(String email, String password) {
        return repository.login(email, password);
    }

    public LiveData<Pair<Boolean, Boolean>> checkActiveServices() {
        return repository.checkActiveServices();
    }

    public boolean isLogin() {
        return repository.isLogin();
    }

    public LiveData<Boolean> resetPassword(String email) {
        return repository.resetPassword(email);
    }
}
