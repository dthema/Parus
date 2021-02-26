package com.begletsov.parus.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.begletsov.parus.RequestTime;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class OnlineService extends Service {

    private final static String TAG = "OnlineService";
    private FirebaseFirestore db;
    private Thread checkOnline;
    private String uid;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = Objects.requireNonNull(cm).getActiveNetworkInfo();
        assert netInfo != null;
        return netInfo.isConnected();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (isServiceRunning) return;
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // отправка в Firestore данные о активности пользователя в приложении кждую минуту (работает только если есть связанный пользователь)
        checkOnline = new Thread(()->{
            while (isServiceRunning) {
                if (isNetworkAvailable()) {
                    if (uid != null) {
                        try {
                            if (new RequestTime().execute("").get() != null)
                            db.collection("users").document(uid).update("lastOnline", new RequestTime().execute("").get());
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isServiceRunning = true;
            if (intent.getStringExtra("uid") != null)
                uid = intent.getStringExtra("uid");
            if (!checkOnline.isAlive())
                checkOnline.start();
        }
        else stopMyService();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        isServiceRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceRunning = false;
        if (!checkOnline.isInterrupted())
            checkOnline.interrupt();
        this.stopSelf();
    }

    public static boolean isServiceRunning = false;

    private void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }
}
