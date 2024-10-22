package com.begletsov.parus.services;

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

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.begletsov.parus.MainActivity;
import com.begletsov.parus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final static String TAG = "Notification";
    public static boolean inChat = false;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId).update("token", token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // уведомление о новом сообщении в чате, если он закрыт
        if (remoteMessage.getData().size() > 0) {
            String title = "";
            String body = "";
            for (String key : remoteMessage.getData().keySet()) {
                if (key.equals("title"))
                    title = remoteMessage.getData().get(key);
                else if (key.equals("body"))
                    body = remoteMessage.getData().get(key);
            }
            assert title != null;
            if (title.equals("Новое сообщение") && !inChat) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("fromNotification", true);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                String CHANNEL_ID = "channel_chat";
                CharSequence CHANNEL_NAME = "Чат";
                String CHANNEL_SIREN_DESCRIPTION = "Chat";
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
                        .setSmallIcon(R.drawable.ic_chat_black_24dp)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setVibrate(new long[]{0, 500, 1000})
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(resultPendingIntent);
                Notification notification = status.build();
                notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (mNotificationManager != null)
                    mNotificationManager.notify(1, notification);
            }
            // уведомление о пулььсе, если оно вне нормы
            if (title.equals("Предупреждение")) {
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
                        .setContentTitle(title)
                        .setContentText(body)
                        .setVibrate(new long[]{0, 500, 1000})
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setContentIntent(resultPendingIntent);
                Notification notification = status.build();
                if (mNotificationManager != null)
                    mNotificationManager.notify(10, notification);
            }
            // уведомление о вызове помощника от подопечного
            if (title.equals("Уведомление")) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                String CHANNEL_ID = "channel_call";
                CharSequence CHANNEL_NAME = "Уведомления";
                String CHANNEL_SIREN_DESCRIPTION = "";
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
                        .setSmallIcon(R.drawable.ic_record_voice_over_black_24dp)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setVibrate(new long[]{0, 500, 1000})
                        .setDefaults(Notification.DEFAULT_LIGHTS)
                        .setContentIntent(resultPendingIntent);
                Notification notification = status.build();
                if (mNotificationManager != null)
                    mNotificationManager.notify(10, notification);
            }
        }
        super.onMessageReceived(remoteMessage);
    }
}
