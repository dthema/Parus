package com.example.parus.viewmodels.repositories;

import android.content.Context;
import android.content.Intent;

import androidx.work.WorkManager;

import com.example.parus.services.GeoLocationService;
import com.example.parus.services.HeartRateService;
import com.example.parus.services.OnlineService;
import com.example.parus.services.WorkService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ServiceRepository {

    private static ServiceRepository repository;
    private String userId;

    private ServiceRepository() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public synchronized static ServiceRepository getInstance() {
        if (repository == null) repository = new ServiceRepository();
        return repository;
    }

    public void startWorkService(Context context) {
        if (!WorkService.isServiceRunning)
            context.startService(new Intent(context, WorkService.class));
    }

    public void stopWorkService(Context context) {
        context.stopService(new Intent(context, WorkService.class));
        WorkManager.getInstance(context).cancelAllWork();
    }

    public void startOnlineService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || OnlineService.isServiceRunning)
            return;
        if (userId == null)
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Intent intent = new Intent(context, OnlineService.class);
        intent.putExtra("uid", userId);
        context.startService(intent);
    }

    public void stopOnlineService(Context context) {
        context.stopService(new Intent(context, OnlineService.class));
    }

    public void startGeoLocationService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || GeoLocationService.isServiceRunning)
            return;
        if (userId == null)
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Intent intent = new Intent(context, GeoLocationService.class);
        intent.putExtra("uid", userId);
        context.startService(intent);
        FirebaseFirestore.getInstance().collection("users").document(userId).update("checkGeoPosition", true);
    }

    public void stopGeoLocationService(Context context) {
        context.stopService(new Intent(context, GeoLocationService.class));
        FirebaseFirestore.getInstance().collection("users").document(userId).update("checkGeoPosition", false);
    }

    public void startHeartRateService(Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || HeartRateService.isServiceRunning)
            return;
        if (userId == null)
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Intent intent = new Intent(context, HeartRateService.class);
        intent.putExtra("uid", userId);
        context.startService(intent);
        FirebaseFirestore.getInstance().collection("users").document(userId).update("checkHeartBPM", true);
    }

    public void stopHeartRateService(Context context) {
        context.stopService(new Intent(context, HeartRateService.class));
        FirebaseFirestore.getInstance().collection("users").document(userId).update("checkHeartBPM", false);
    }

    public void stopAllServices(Context context) {
        stopOnlineService(context);
        stopGeoLocationService(context);
        stopHeartRateService(context);
        stopWorkService(context);
    }

    public void destroy() {
        repository = null;
    }
}
