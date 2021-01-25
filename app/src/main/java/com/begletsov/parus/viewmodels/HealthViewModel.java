package com.begletsov.parus.viewmodels;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.begletsov.parus.viewmodels.data.SingleLiveEvent;
import com.begletsov.parus.viewmodels.repositories.ServiceRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

public class HealthViewModel extends AndroidViewModel {

    private static final String TAG = "HealthData";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
    private static final int GOOGLE_CONNECT = 2;

    private final ServiceRepository serviceRepository = ServiceRepository.getInstance();

    public HealthViewModel(@NonNull Application application) {
        super(application);
    }

    private int checkHealthDataConnect() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED)
            return NO_PERMISSION;
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .build();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getApplication(), fitnessOptions);
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) return NO_GOOGLE_ACCOUNT;
        else return GOOGLE_CONNECT;
    }

    private SingleLiveEvent<Integer> healthLiveData;

    public LiveData<Integer> get() {
        healthLiveData = new SingleLiveEvent<>();
        healthLiveData.setValue(checkHealthDataConnect());
        return healthLiveData;
    }

    public SingleLiveEvent<Integer> onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        healthLiveData = new SingleLiveEvent<>();
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FitnessOptions fitnessOptions = FitnessOptions.builder()
                        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                        .build();
                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getApplication(), fitnessOptions);
                if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                    healthLiveData.setValue(GOOGLE_CONNECT);
                } else {
                    healthLiveData.setValue(NO_GOOGLE_ACCOUNT);
                }
            }
        } else
            healthLiveData.setValue(NO_PERMISSION);
        Log.d(TAG + "_request", String.valueOf(requestCode));
        return healthLiveData;
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                serviceRepository.startHeartRateService(getApplication());
            }
            Log.d(TAG + "_acResult", String.valueOf(resultCode));
        }
        Log.d(TAG + "_acRequest", String.valueOf(requestCode));

    }
}
