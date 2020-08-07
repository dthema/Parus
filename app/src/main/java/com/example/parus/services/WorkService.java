package com.example.parus.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.parus.viewmodels.UserModel;
import com.example.parus.viewmodels.data.models.Reminder;
import com.example.parus.workmanagers.OneWorker;
import com.example.parus.workmanagers.PereodicWorker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkService extends LifecycleService {

    private final static String TAG = "WorkService";
    private FirebaseFirestore db;
    private ListenerRegistration registration;
    private UserModel userModel;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (isServiceRunning) return;
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        userModel = new UserModel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            isServiceRunning = true;
            Log.d(TAG, "start");
            userModel.getShortLinkUserData().observe(this, pair -> {
                if (pair == null)
                    return;
                else if (pair.first == null)
                    return;
                String userId = pair.first.first;
                String linkUserId = pair.first.second;
                Boolean isSupport = pair.second;
                if (userId == null || linkUserId == null || isSupport == null)
                    return;
                if (registration != null){
                    if (isSupport && userId.equals(linkUserId)){
                        registration.remove();
                        registration = null;
                    }
                } else {
                    if (!isSupport){
                        registration = db.collection("users").document(userId).collection("alerts")
                                .addSnapshotListener(eventListener);
                    } else if (!userId.equals(linkUserId)){
                        registration = db.collection("users").document(linkUserId).collection("alerts")
                                .addSnapshotListener(eventListener);
                    }
                }
            });
        } else stopMyService();
        return START_STICKY;
    }

    EventListener<QuerySnapshot> eventListener = (queryDocumentSnapshots, e) -> {
            if (e != null) {
                Log.i(TAG, "Listen failed.", e);
                return;
            }
            Log.i(TAG, "+");
            if (queryDocumentSnapshots != null)
                for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                    Reminder reminder = dc.getDocument().toObject(Reminder.class);
                    switch (dc.getType()) {
                        case ADDED:
                            reminder.setId(dc.getDocument().getId());
                            Log.d(TAG, "add snapshot");
                            if (reminder.getType() == 0) {
                                Calendar s = Calendar.getInstance();
                                s.setTime(reminder.getTimeStart());
                                Calendar e1 = Calendar.getInstance();
                                e1.setTime(reminder.getTimeEnd());
                                Calendar i = Calendar.getInstance();
                                i.setTime(reminder.getTimeInterval());
                                @SuppressLint("RestrictedApi") Data data = new Data.Builder()
                                        .putInt("startTime_hour", s.get(Calendar.HOUR_OF_DAY))
                                        .putInt("startTime_minute", s.get(Calendar.MINUTE))
                                        .putInt("endTime_hour", e1.get(Calendar.HOUR_OF_DAY))
                                        .putInt("endTime_minute", e1.get(Calendar.MINUTE))
                                        .putInt("intervalTime_hour", i.get(Calendar.HOUR_OF_DAY))
                                        .putInt("intervalTime_minute", i.get(Calendar.MINUTE))
                                        .putString("name", reminder.getName())
                                        .putString("document_name", dc.getDocument().getId())
                                        .build();
                                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(PereodicWorker.class)
                                        .setInputData(data)
                                        .build();
                                WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.KEEP, work);
                            } else if (reminder.getType() == 1) {
                                List<Date> list = reminder.getTimers();
                                StringBuilder stringBuilder = new StringBuilder();
                                Collections.sort(list, Date::compareTo);
                                Date currentTime = new Date(System.currentTimeMillis());
                                Calendar c = Calendar.getInstance();
                                c.setTime(currentTime);
                                Date waitDate = currentTime;
                                for (Date date : list) {
                                    Log.d(TAG, date.toString());
                                    Calendar d = Calendar.getInstance();
                                    d.setTime(date);
                                    stringBuilder.append(d.get(Calendar.HOUR_OF_DAY)).append(":").append(d.get(Calendar.MINUTE)).append(" ");
                                }
                                for (Date date : list) {
                                    Calendar w = Calendar.getInstance();
                                    Calendar d = Calendar.getInstance();
                                    d.setTime(date);
                                    w.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), c.get(Calendar.SECOND));
                                    if (c.compareTo(w) <= 0) {
                                        waitDate = date;
                                        break;
                                    }
                                }
                                Log.d(TAG, waitDate.toString());
                                Log.d(TAG, stringBuilder.toString());
                                @SuppressLint("RestrictedApi") Data data = new Data.Builder()
                                        .putString("timers", stringBuilder.toString())
                                        .putString("name", reminder.getName())
                                        .putString("document_name", dc.getDocument().getId())
                                        .build();
                                if (waitDate.equals(currentTime)) {
                                    int delay = 1440 - (c.get(Calendar.MINUTE) + (c.get(Calendar.HOUR_OF_DAY) * 60));
                                    String start = stringBuilder.toString().split(" ")[0];
                                    delay += Integer.parseInt(start.split(":")[1]) + (Integer.parseInt(start.split(":")[0]) * 60);
                                    Log.d(TAG, String.valueOf(delay));
                                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                                            .setInitialDelay((delay * 60) - c.get(Calendar.SECOND), TimeUnit.SECONDS)
                                            .setInputData(data)
                                            .build();
                                    WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.KEEP, work);
                                } else {
                                    Calendar w = Calendar.getInstance();
                                    w.setTime(waitDate);
                                    int delay = (w.get(Calendar.MINUTE) + (w.get(Calendar.HOUR_OF_DAY) * 60)) - (c.get(Calendar.MINUTE) + (c.get(Calendar.HOUR_OF_DAY) * 60));
                                    if (delay < 0) {
                                        delay -= 1440;
                                        delay = -delay;
                                    }
                                    Log.d(TAG, String.valueOf(delay));
                                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                                            .setInitialDelay((delay * 60) - c.get(Calendar.SECOND), TimeUnit.SECONDS)
                                            .setInputData(data)
                                            .build();
                                    WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.KEEP, work);
                                }
                            }
                            break;
                        case MODIFIED:
                            reminder.setId(dc.getDocument().getId());
                            if (reminder.getType() == 0) {
                                Calendar s = Calendar.getInstance();
                                s.setTime(reminder.getTimeStart());
                                Calendar e1 = Calendar.getInstance();
                                e1.setTime(reminder.getTimeEnd());
                                Calendar i = Calendar.getInstance();
                                i.setTime(reminder.getTimeInterval());
                                @SuppressLint("RestrictedApi") Data data = new Data.Builder()
                                        .putInt("startTime_hour", s.get(Calendar.HOUR_OF_DAY))
                                        .putInt("startTime_minute", s.get(Calendar.MINUTE))
                                        .putInt("endTime_hour", e1.get(Calendar.HOUR_OF_DAY))
                                        .putInt("endTime_minute", e1.get(Calendar.MINUTE))
                                        .putInt("intervalTime_hour", i.get(Calendar.HOUR_OF_DAY))
                                        .putInt("intervalTime_minute", i.get(Calendar.MINUTE))
                                        .putString("name", reminder.getName())
                                        .putString("document_name", dc.getDocument().getId())
                                        .build();
                                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(PereodicWorker.class)
                                        .setInputData(data)
                                        .build();
                                WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.REPLACE, work);
                            } else if (reminder.getType() == 1) {
                                List<Date> list = reminder.getTimers();
                                StringBuilder stringBuilder = new StringBuilder();
                                Collections.sort(list, Date::compareTo);
                                Date currentTime = new Date(System.currentTimeMillis());
                                Calendar c = Calendar.getInstance();
                                c.setTime(currentTime);
                                Date waitDate = currentTime;
                                for (Date date : list) {
                                    Log.d(TAG, date.toString());
                                    Calendar d = Calendar.getInstance();
                                    d.setTime(date);
                                    stringBuilder.append(d.get(Calendar.HOUR_OF_DAY)).append(":").append(d.get(Calendar.MINUTE)).append(" ");
                                }
                                for (Date date : list) {
                                    Calendar w = Calendar.getInstance();
                                    Calendar d = Calendar.getInstance();
                                    d.setTime(date);
                                    w.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE), c.get(Calendar.SECOND));
                                    if (c.compareTo(w) <= 0) {
                                        waitDate = date;
                                        break;
                                    }
                                }
                                Log.d(TAG, waitDate.toString());
                                Log.d(TAG, stringBuilder.toString());
                                @SuppressLint("RestrictedApi") Data data = new Data.Builder()
                                        .putString("timers", stringBuilder.toString())
                                        .putString("name", reminder.getName())
                                        .putString("document_name", dc.getDocument().getId())
                                        .build();
                                if (waitDate.equals(currentTime)) {
                                    int delay = 1440 - (c.get(Calendar.MINUTE) + (c.get(Calendar.HOUR_OF_DAY) * 60));
                                    String start = stringBuilder.toString().split(" ")[0];
                                    delay += Integer.parseInt(start.split(":")[1]) + (Integer.parseInt(start.split(":")[0]) * 60);
                                    Log.d(TAG, String.valueOf(delay));
                                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                                            .setInitialDelay((delay * 60) - c.get(Calendar.SECOND), TimeUnit.SECONDS)
                                            .setInputData(data)
                                            .build();
                                    WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.REPLACE, work);
                                } else {
                                    Calendar w = Calendar.getInstance();
                                    w.setTime(waitDate);
                                    int delay = (w.get(Calendar.MINUTE) + (w.get(Calendar.HOUR_OF_DAY) * 60)) - (c.get(Calendar.MINUTE) + (c.get(Calendar.HOUR_OF_DAY) * 60));
                                    if (delay < 0) {
                                        delay -= 1440;
                                        delay = -delay;
                                    }
                                    Log.d(TAG, String.valueOf(delay));
                                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                                            .setInitialDelay((delay * 60) - c.get(Calendar.SECOND), TimeUnit.SECONDS)
                                            .setInputData(data)
                                            .build();
                                    WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(dc.getDocument().getId(), ExistingWorkPolicy.REPLACE, work);
                                }
                            }
                            break;
                        case REMOVED:
                            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(dc.getDocument().getId());
                            break;
                    }
                }
        };

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
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        this.stopSelf();
    }

    public static boolean isServiceRunning = false;

    void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }
}
