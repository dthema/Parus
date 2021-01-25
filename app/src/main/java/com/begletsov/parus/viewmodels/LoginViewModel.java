package com.begletsov.parus.viewmodels;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.begletsov.parus.viewmodels.repositories.LoginRepository;

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
