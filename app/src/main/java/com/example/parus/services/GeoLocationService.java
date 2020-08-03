package com.example.parus.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.parus.MainActivity;
import com.example.parus.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Objects;

public class GeoLocationService extends Service {

    private final static String TAG = "GeoLocationService";
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String CHANNEL_ID="channel_geoPosition";
        CharSequence CHANNEL_NAME = "Геопозиция";
        String CHANNEL_SIREN_DESCRIPTION = "Geo Position";
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
                .setSmallIcon(R.drawable.ic_room_black_24dp)
                .setContentTitle(CHANNEL_NAME)
                .setContentText("Отслеживается")
                .setVibrate(new long[]{0, 500, 1000})
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSound(RingtoneManager.getDefaultUri(0))
                .setContentIntent(pendingIntent);
        Notification notification = status.build();
        startForeground(12, notification);
        new Thread(()-> db.collection("users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).update("checkGeoPosition", true)).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            isServiceRunning = true;
            // считывание геоданных при их изменении и отправка в Firestore
            new Thread(()->{
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        Log.d(TAG, "last - " + wayLatitude + " " + wayLongitude);
                    }
                });
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                locationRequest.setInterval(5000);
                locationRequest.setSmallestDisplacement(10);
                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                wayLatitude = location.getLatitude();
                                wayLongitude = location.getLongitude();
                                Log.d(TAG, "new - " + wayLatitude + " " + wayLongitude);
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("Latitude", wayLatitude);
                                hashMap.put("Longitude", wayLongitude);
                                db.collection("users").document(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).collection("GeoPosition").document("Location").set(hashMap)
                                        .addOnSuccessListener(l-> Log.d(TAG, "upload"))
                                        .addOnFailureListener(l-> Log.d(TAG, l.getMessage()));

                            }
                        }
                    }
                };
                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        Looper.getMainLooper());
            }).start();
        }
        else stopMyService();
        return START_STICKY;
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
        new Thread(()->{
            if (fusedLocationClient != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        }).start();
        this.stopSelf();
    }

    public static boolean isServiceRunning = false;

    void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }
}
