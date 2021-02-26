package com.begletsov.parus.viewmodels.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.atomic.AtomicReference;

public class NetworkRepository {

    private static NetworkRepository repository;
    private boolean isThreadActive;
    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback networkCallback;

    private NetworkRepository() {
    }

    public synchronized static NetworkRepository getInstance() {
        if (repository == null) repository = new NetworkRepository();
        return repository;
    }

    private final MutableLiveData<Boolean> internetConnected = new MutableLiveData<>();

    public LiveData<Boolean> connectionListener(Context context) {
        if (isThreadActive)
            return null;
        isThreadActive = true;
        AtomicReference<Boolean> internetConnect = new AtomicReference<>(null);
        new Thread(() -> {
            cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            assert cm != null;
            if (Build.VERSION.SDK_INT < 23) {
                while (isThreadActive) {
                    NetworkInfo netInfo = cm.getActiveNetworkInfo();
                    if (netInfo != null && netInfo.isConnected()) {
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
            } else {
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        internetConnected.postValue(true);
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        internetConnected.postValue(false);
                    }
                };
                cm.registerNetworkCallback(builder.build(), networkCallback);
            }
        }).start();
        return internetConnected;
    }

    public void stopCheckInternetConnection() {
        isThreadActive = false;
    }

}
