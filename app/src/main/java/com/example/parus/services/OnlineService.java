package com.example.parus.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;

import com.example.parus.RequestTime;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.InetAddress;
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
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
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
                    Log.d(TAG, String.valueOf(isNetworkAvailable()));
                    if (uid != null) {
                        try {
                            if (new RequestTime().execute("").get() != null)
                            db.collection("users").document(uid).update("lastOnline", new RequestTime().execute("").get().getDate())
                                    .addOnCompleteListener(t -> {
                                        if (t.isSuccessful())
                                            Log.d(TAG, "updated");
                                        else
                                            Log.d(TAG, "ERROR");
                                    });
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
            Log.d(TAG, "start");
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
    public boolean stopService(Intent name) {
        Log.d(TAG, "stop");
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroy");
        isServiceRunning = false;
        if (!checkOnline.isInterrupted())
            checkOnline.interrupt();
        this.stopSelf();
    }

    public static boolean isServiceRunning = false;

    void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }
}
