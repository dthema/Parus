package com.example.parus.services;

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

import com.example.parus.MainActivity;
import com.example.parus.R;
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
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HeartRateService extends Service {

    private final static String TAG = "HeartRateService";
    private OnDataPointListener mListener;
    private GoogleSignInAccount account;
    private FirebaseFirestore db;
    private Handler handler;
    private Runnable checkPulse;
    private HealthDataStore mStore;
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
        checkPulse = () -> new Thread(()-> db.collection("users").document(uid).update("pulse", 0).addOnCompleteListener(t ->{
                    if (t.isSuccessful())
                        Log.d(TAG, "pulse = 0");
                })).start();
        // запуск Samsung Health if API > 23, else - Google Fit
        if (Build.VERSION.SDK_INT < 23) {
            account = GoogleSignIn
                    .getLastSignedInAccount(getApplicationContext());
        } else {
            mStore = new HealthDataStore(getApplicationContext(), mConnectionListener);
            mStore.connectService();
        }
        String CHANNEL_ID="channel_heartBPM";
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
                mNotificationManager.createNotificationChannel( mChannel );
            }
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder status = new NotificationCompat.Builder(getApplicationContext(),CHANNEL_ID);
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

    private boolean isPermissionAcquired() {
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

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            Log.d(TAG, "Health data service is connected.");
            if (!isPermissionAcquired()) {
                stopMyService();
            } else {
                HealthDataObserver.addObserver(mStore, HealthConstants.HeartRate.HEALTH_DATA_TYPE, mObserver);
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "Health data service is not available.");
            stopMyService();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "Health data service is disconnected.");
            HealthDataObserver.removeObserver(mStore, mObserver);
            stopMyService();
        }
    };

    final private HealthDataObserver mObserver = new HealthDataObserver(null) {

        @Override
        public void onChange(String dataTypeName) {
            if (isServiceRunning)
                readTodayHeartRate();
            else
                mStore.disconnectService();
        }
    };

    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;
    // чтение данных о пульсе за день
    private void readTodayHeartRate() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
                .setProperties(new String[] {HealthConstants.HeartRate.HEART_RATE})
                .setLocalTimeRange(HealthConstants.HeartRate.START_TIME, HealthConstants.HeartRate.TIME_OFFSET,
                        startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mListenerSamsung);
        } catch (Exception e) {
            Log.e("Home", "Getting step count fails.", e);
        }
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListenerSamsung = result -> {
        int count = 0;
        try {
            for (HealthData data : result) {
                count = data.getInt(HealthConstants.HeartRate.HEART_RATE);
            }
        } finally {
            result.close();
        }
        if (count != 0){
            // отправка последних полученных данных о пульсе в Firestore
            sendPulse(count);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isServiceRunning = true;
            if (intent.getStringExtra("uid") != null)
                uid = intent.getStringExtra("uid");
            new Thread(()->{
                db.collection("users").document(uid).update("checkHeartBPM", true);
                if (Build.VERSION.SDK_INT < 23) {
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
                }
            }).start();
        }
        else stopMyService();
        return START_STICKY;
    }

    private void sendPulse(int pulse){
        db.collection("users").document(uid).update("pulse", pulse)
                .addOnCompleteListener(Executors.newSingleThreadExecutor(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Upload successful.");
                        handler.removeCallbacks(checkPulse);
                        handler.postDelayed(checkPulse, 60000);
                        if (pulse > 90 || pulse < 55) {
                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(s->{
                                        if (!uid.equals(s.getString("linkUserId"))){
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
                            String CHANNEL_ID="channel_heartBPM";
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
                                    mNotificationManager.createNotificationChannel( mChannel );
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
        new Thread(()->{
            if (Build.VERSION.SDK_INT < 23) {
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
            } else {
                mConnectionListener.onDisconnected();
                mStore.disconnectService();
            }
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
