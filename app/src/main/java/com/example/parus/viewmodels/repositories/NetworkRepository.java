package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.atomic.AtomicReference;

public class NetworkRepository {

    private boolean isThreadActive;

    public NetworkRepository() {
    }

    private MutableLiveData<Boolean> internetConnected = new MutableLiveData<>();

    public MutableLiveData<Boolean> connectionListener(Context context) {
        if (isThreadActive)
            return null;
        isThreadActive = true;
        AtomicReference<Boolean> internetConnect = new AtomicReference<>(null);
        new Thread(() -> {
            while (isThreadActive) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    if (internetConnect.get() != null) {
                        if (!internetConnect.get()) {
                            internetConnect.set(true);
                            internetConnected.postValue(true);
                        }
                    } else {
                        internetConnect.set(true);
                        internetConnected.postValue(true);
                    }
                } else {
                    if (internetConnect.get() != null) {
                        if (internetConnect.get()) {
                            internetConnect.set(false);
                            internetConnected.postValue(false);
                        }
                    } else {
                        internetConnect.set(false);
                        internetConnected.postValue(false);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return internetConnected;
    }

    public void stopCheckInternetConnection() {
        Log.d("InternetLiveData", "stop");
        isThreadActive = false;
    }

}
