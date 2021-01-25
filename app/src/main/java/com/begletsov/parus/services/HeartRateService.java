package com.begletsov.parus.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.begletsov.parus.MainActivity;
import com.begletsov.parus.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartRateService extends Service {

    private final static String TAG = "HeartRateService";
    private OnDataPointListener mListener;
    private GoogleSignInAccount account;
    private FirebaseFirestore db;
    private Handler handler;
    private Runnable checkPulse;
    private String uid;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "start");
        if (isServiceRunning) return;
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(
                new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(false)
                        .build()
        );
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        handler = new Handler();
        // Обнуление данных о пульсе, если их нет минуту
        checkPulse = () -> new Thread(() -> db.collection("users").document(uid).update("pulse", 0).addOnCompleteListener(t -> {
            if (t.isSuccessful())
                Log.d(TAG, "pulse = 0");
        })).start();
        // запуск Google Fit
        account = GoogleSignIn
                .getLastSignedInAccount(getApplicationContext());
        String CHANNEL_ID = "channel_heartBPM";
        CharSequence CHANNEL_NAME = "Сердцебиение";
        String CHANNEL_SIREN_DESCRIPTION = "Heart rate BPM";
        NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setLightColor(Color.GRAY);
            mChannel.enableLights(true);
            mChannel.setDescription(CHANNEL_SIREN_DESCRIPTION);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            }
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder status = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        status.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_favorite_black_24dp)
                .setContentTitle(CHANNEL_NAME)
                .setContentText("Отслеживается")
                .setVibrate(new long[]{0, 500, 1000})
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSound(RingtoneManager.getDefaultUri(0))
                .setContentIntent(pendingIntent);
        Notification notification = status.build();
        startForeground(5, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isServiceRunning = true;
            if (intent.getStringExtra("uid") != null)
                uid = intent.getStringExtra("uid");
            new Thread(() -> {
                db.collection("users").document(uid).update("checkHeartBPM", true);
                mListener =
                        dataPoint -> {
                            for (Field field : dataPoint.getDataType().getFields()) {
                                Value val = dataPoint.getValue(field);
                                Log.d(TAG, "Detected DataPoint field: " + field.getName());
                                Log.d(TAG, "Detected DataPoint value: " + val.asInt());
                                // отправка последних полученных данных о пульсе в Firestore
                                sendPulse(val.asInt());
                            }
                        };
                Fitness.getSensorsClient(getApplicationContext(), account)
                        .add(new SensorRequest.Builder()
                                        .setDataType(DataType.TYPE_HEART_RATE_BPM)
                                        .setSamplingRate(1, TimeUnit.SECONDS)
                                        .build(),
                                mListener)
                        .addOnCompleteListener(
                                task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Listener registered!");
                                    } else {
                                        Log.d(TAG, "Listener not registered.", task.getException());
                                    }
                                });

            }).start();
        } else stopMyService();
        return START_STICKY;
    }

    private void sendPulse(int pulse) {
        db.collection("users").document(uid).update("pulse", pulse)
                .addOnCompleteListener(Executors.newSingleThreadExecutor(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Upload successful.");
                        handler.removeCallbacks(checkPulse);
                        handler.postDelayed(checkPulse, 300000);
                        if (pulse > 90 || pulse < 55) {
                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(s -> {
                                        if (!uid.equals(s.getString("linkUserId"))) {
                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            hashMap.put("type", "heart");
                                            hashMap.put("userId", s.getString("linkUserId"));
                                            if (s.getString("linkUserId") != null)
                                                db.collection("users").document(Objects.requireNonNull(s.getString("linkUserId"))).collection("Notifications").add(hashMap);
                                        }
                                    });
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            String CHANNEL_ID = "channel_heartBPM";
                            CharSequence CHANNEL_NAME = "Сердцебиение";
                            String CHANNEL_SIREN_DESCRIPTION = "Heart rate BPM";
                            NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            NotificationChannel mChannel;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                                mChannel.setLightColor(Color.GRAY);
                                mChannel.enableLights(true);
                                mChannel.setDescription(CHANNEL_SIREN_DESCRIPTION);
                                if (mNotificationManager != null) {
                                    mNotificationManager.createNotificationChannel(mChannel);
                                }
                            }

                            NotificationCompat.Builder status = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
                            status.setAutoCancel(true)
                                    .setSmallIcon(R.drawable.ic_warning_black_24dp)
                                    .setContentTitle("Предупреждение")
                                    .setContentText("Ваш пульс вне нормы")
                                    .setVibrate(new long[]{0, 500, 1000})
                                    .setDefaults(Notification.DEFAULT_LIGHTS)
                                    .setContentIntent(resultPendingIntent);
                            Notification notification = status.build();
                            assert mNotificationManager != null;
                            mNotificationManager.notify(10, notification);
                        }
                    } else {
                        Log.d(TAG, Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d(TAG, "stop");
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy");
        isServiceRunning = false;
        new Thread(() -> {
            Fitness.getSensorsClient(getApplicationContext(), account)
                    .remove(mListener)
                    .addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "Listener was removed!");
                                } else {
                                    Log.d(TAG, "Listener was not removed.");
                                }
                            });
            final String collection = "users";
            db.collection(collection).document(uid).update("pulse", 0);
        }).start();
        super.onDestroy();
    }

    public static boolean isServiceRunning = false;

    private void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }
}
