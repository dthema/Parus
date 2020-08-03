package com.example.parus.workmanagers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PereodicWorker extends Worker {

    private static final String TAG = "workmng";

    public PereodicWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, getTags().toString() + "one_time: start");
        Date currentTime = new Date(System.currentTimeMillis());
        int startTime_hour = getInputData().getInt("startTime_hour", 25);
        int startTime_minute = getInputData().getInt("startTime_minute", 61);
        int endTime_hour = getInputData().getInt("endTime_hour", 25);
        int endTime_minute = getInputData().getInt("endTime_minute", 61);
        int intervalTime_hour = getInputData().getInt("intervalTime_hour", 25);
        int intervalTime_minute = getInputData().getInt("intervalTime_minute", 61);
        if (!(startTime_hour == 25 && startTime_minute == 61 && endTime_hour == 25 && endTime_minute == 61 && intervalTime_hour == 25 && intervalTime_minute == 61)) {
            StringBuilder startTime = new StringBuilder();
            startTime.append(startTime_hour).append(":").append(startTime_minute);
            StringBuilder endTime = new StringBuilder();
            endTime.append(endTime_hour).append(":").append(endTime_minute);
            StringBuilder intervalTime = new StringBuilder();
            intervalTime.append(intervalTime_hour).append(":").append(intervalTime_minute);
            StringBuilder timers = new StringBuilder();
            while (startTime.toString().compareTo(endTime.toString()) < 0){
                timers.append(startTime).append(" ");
                String time = startTime.toString();
                startTime = startTime.replace(0, startTime.length(), "");
                int hour = Integer.parseInt(time.split(":")[0]);
                int min = Integer.parseInt(time.split(":")[1]);
                hour += intervalTime_hour;
                min += intervalTime_minute;
                if (min >= 60){
                    hour++;
                    min -= 60;
                }
                startTime.append(convertDate(hour)).append(":").append(convertDate(min));
            }
            Calendar c = Calendar.getInstance();
            c.setTime(currentTime);
            String[] times = timers.toString().split(" ");
            String waitDate = "100:100";
            for (String time : times) {
                Calendar w = Calendar.getInstance();
                w.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), Integer.parseInt(time.split(":")[0]), Integer.parseInt(time.split(":")[1]), c.get(Calendar.SECOND));
                if (c.compareTo(w) < 0) {
                    waitDate = time;
                    break;
                }
            }
            Log.d(TAG, waitDate);
            @SuppressLint("RestrictedApi") Data data = new  Data.Builder()
                    .putString("timers", timers.toString())
                    .putString("name", getInputData().getString("name"))
                    .putString("document_name", getInputData().getString("document_name"))
                    .build();
            if (waitDate.equals("100:100")){
                int delay = 1440 - (c.get(Calendar.MINUTE)+(c.get(Calendar.HOUR_OF_DAY)*60));
                String start = times[0];
                delay += Integer.parseInt(start.split(":")[1])+(Integer.parseInt(start.split(":")[0])*60);
                Log.d(TAG+"-delay", String.valueOf(delay));
                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                        .setInitialDelay((delay*60)-c.get(Calendar.SECOND), TimeUnit.SECONDS)
                        .setInputData(data)
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Objects.requireNonNull(getInputData().getString("document_name")), ExistingWorkPolicy.REPLACE, work);
            } else {
                int delay = Integer.parseInt(waitDate.split(":")[1])+(Integer.parseInt(waitDate.split(":")[0])*60)-(c.get(Calendar.MINUTE)+(c.get(Calendar.HOUR_OF_DAY)*60));
                Log.d(TAG+"-delay", String.valueOf(delay));
                OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                        .setInitialDelay((delay*60)-c.get(Calendar.SECOND), TimeUnit.SECONDS)
                        .setInputData(data)
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Objects.requireNonNull(getInputData().getString("document_name")), ExistingWorkPolicy.REPLACE, work);
            }
        }
        return Result.success();
    }

    private String convertDate(int input) {
        if (input >= 10) {
            return String.valueOf(input);
        } else {
            return "0" + input;
        }
    }

    @Override
    public void onStopped() {
        Log.d(TAG, getTags().toString() + "stop");
        super.onStopped();
    }
}