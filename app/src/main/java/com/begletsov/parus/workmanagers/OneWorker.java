package com.begletsov.parus.workmanagers;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.begletsov.parus.MainActivity;
import com.begletsov.parus.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OneWorker extends Worker {

    private static final String TAG = "workmng";

    public OneWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, getId() + " - notification start");
        String timersO = getInputData().getString("timers");
        assert timersO != null;
        String[] timers = timersO.split(" ");
        Date currentTime = new Date(System.currentTimeMillis());
        String waitDate = "100:100";
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String CHANNEL_ID = "channel_reminders";
        CharSequence CHANNEL_NAME = "Напоминания";
        String CHANNEL_SIREN_DESCRIPTION = "Alerts";
        NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setLightColor(Color.GRAY);
            mChannel.enableLights(true);
            mChannel.setDescription(CHANNEL_SIREN_DESCRIPTION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(mChannel);
            }
        }
        NotificationCompat.Builder status = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
        status.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle("Напоминание")
                .setContentText(getInputData().getString("name"))
                .setVibrate(new long[]{0, 500, 1000})
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(resultPendingIntent);
        Notification notification = status.build();
        notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        assert mNotificationManager != null;
        mNotificationManager.notify(2, notification);
        for (String timer : timers) {
            Calendar c = Calendar.getInstance();
            c.setTime(currentTime);
            Calendar w = Calendar.getInstance();
            w.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), Integer.parseInt(timer.split(":")[0]), Integer.parseInt(timer.split(":")[1]), c.get(Calendar.SECOND));
            if ((c.get(Calendar.HOUR_OF_DAY) == w.get(Calendar.HOUR_OF_DAY) && c.get(Calendar.MINUTE) < w.get(Calendar.MINUTE)) || c.get(Calendar.HOUR_OF_DAY) < w.get(Calendar.HOUR_OF_DAY)) {
                waitDate = timer;
                break;
            }
        }
        Log.d(TAG+"-wait", waitDate);
        @SuppressLint("RestrictedApi") Data data = new  Data.Builder()
                .putString("timers", timersO)
                .putString("name", getInputData().getString("name"))
                .putString("document_name", getInputData().getString("document_name"))
                .build();
        Calendar c = Calendar.getInstance();
        c.setTime(currentTime);
        if (waitDate.equals("100:100")){
            int delay = 1440 - (c.get(Calendar.MINUTE)+(c.get(Calendar.HOUR_OF_DAY)*60));
            String start = timers[0];
            delay += Integer.parseInt(start.split(":")[1])+(Integer.parseInt(start.split(":")[0])*60);
            Log.d(TAG+"-delayA", String.valueOf(delay));
            Log.d(TAG+"-delaySec", String.valueOf(c.get(Calendar.SECOND)));
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                    .setInitialDelay((delay*60)-c.get(Calendar.SECOND), TimeUnit.SECONDS)
                    .setInputData(data)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Objects.requireNonNull(getInputData().getString("document_name")), ExistingWorkPolicy.REPLACE, work);
        } else {
            Integer delay = Integer.parseInt(waitDate.split(":")[1])+(Integer.parseInt(waitDate.split(":")[0])*60)-(c.get(Calendar.MINUTE)+(c.get(Calendar.HOUR_OF_DAY)*60));
            Log.d(TAG+"-delayB", String.valueOf(delay));
            Log.d(TAG+"-delaySec", String.valueOf(c.get(Calendar.SECOND)));
            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(OneWorker.class)
                    .setInitialDelay((delay*60)-c.get(Calendar.SECOND), TimeUnit.SECONDS)
                    .setInputData(data)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Objects.requireNonNull(getInputData().getString("document_name")), ExistingWorkPolicy.REPLACE, work);
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        Log.d(TAG, getTags().toString() + "stop");
        super.onStopped();
    }
}