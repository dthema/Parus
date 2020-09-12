package com.example.parus.viewmodels;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.parus.viewmodels.data.SingleLiveEvent;
import com.example.parus.viewmodels.repositories.ServiceRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;

import java.util.Collections;
import java.util.Map;

public class HealthViewModel extends AndroidViewModel {

    private static final String TAG = "HealthData";
    private static final int NO_PERMISSION = 0;
    private static final int NO_GOOGLE_ACCOUNT = 1;
    private static final int GOOGLE_CONNECT = 2;
    private static final int SAMSUNG_NO_CONNECT = 3;
    private static final int SAMSUNG_CONNECT = 4;
    private HealthDataStore mStore;

    private final ServiceRepository serviceRepository = ServiceRepository.getInstance();

    public HealthViewModel(@NonNull Application application) {
        super(application);
    }

    private int checkHealthDataConnect(){
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED)
            return NO_PERMISSION;
        if (Build.VERSION.SDK_INT < 23){
            FitnessOptions fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                    .build();
            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getApplication(), fitnessOptions);
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) return NO_GOOGLE_ACCOUNT;
            else return GOOGLE_CONNECT;
        } else {
            mStore = new HealthDataStore(getApplication(), mConnectionListener);
            mStore.connectService();
            return SAMSUNG_NO_CONNECT;
        }
    }

    private SingleLiveEvent<Integer> healthLiveData;

    public LiveData<Integer> get(){
        healthLiveData = new SingleLiveEvent<>();
        int result = checkHealthDataConnect();
        if (result != SAMSUNG_NO_CONNECT) {
            healthLiveData.setValue(result);
        }
        return healthLiveData;
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @SuppressLint("SetTextI18n")
        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (isPermissionAcquired(mStore)) {
                Log.d(TAG, "+");
                healthLiveData.postValue(SAMSUNG_CONNECT);
            } else {
                Log.d(TAG, "-");
                healthLiveData.postValue(SAMSUNG_NO_CONNECT);
            }
            mStore.disconnectService();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
        }
    };

    public boolean isPermissionAcquired(HealthDataStore mStore) {
        HealthPermissionManager.PermissionKey permKey = new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ);
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(Collections.singleton(permKey));
            return resultMap.get(permKey);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    public SingleLiveEvent<Integer> onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        healthLiveData = new SingleLiveEvent<>();
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT < 23) {
                    FitnessOptions fitnessOptions = FitnessOptions.builder()
                            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                            .build();
                    GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getApplication(), fitnessOptions);
                    if (GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                        healthLiveData.setValue(GOOGLE_CONNECT);
                    } else {
                        healthLiveData.setValue(NO_GOOGLE_ACCOUNT);
                    }
                } else {
                    mStore = new HealthDataStore(getApplication().getApplicationContext(), mConnectionListener);
                    mStore.connectService();
                }
            } else
                healthLiveData.setValue(NO_PERMISSION);
        }
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

    @Override
    protected void onCleared() {
        if (mStore != null)
            mStore.disconnectService();
        super.onCleared();
    }
}
