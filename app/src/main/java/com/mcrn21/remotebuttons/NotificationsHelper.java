package com.mcrn21.remotebuttons;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationsHelper {
    static void createNotificationChannel(Context context) {
        NotificationChannel serviceChannel = new NotificationChannel(
                Common.NOTIFICATIONS_CHANNEL_ID,
                context.getResources().getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    static Notification buildNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, Common.NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(context.getResources().getString(R.string.remote_buttons_notify_title))
                .setContentText(context.getResources().getString(R.string.remote_buttons_notify_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }
}
