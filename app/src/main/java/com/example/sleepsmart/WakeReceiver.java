package com.example.sleepsmart;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

import com.example.sleepsmart.alarm.AlarmScheduler;
import com.example.sleepsmart.data.AlarmSettings;
import com.example.sleepsmart.data.SettingsStore;

/**
 * 到点唤醒：以闹钟类别发出高优先级通知（闹钟音 + 振动，点击回到应用）。
 * 闹钟为一次性，响铃后若开关仍为开启，按持久化设置自动续排明天。
 */
public class WakeReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "smart_wake_alarm";
    private static final int NOTIFICATION_ID = 1001;
    private static final int OPEN_APP_REQUEST = 1002;

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        manager.createNotificationChannel(wakeChannel());

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openApp = PendingIntent.getActivity(
                context, OPEN_APP_REQUEST, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_sleep)
                .setContentTitle("轻醒 · 到起床时间了")
                .setContentText("现在处于睡眠窗口内的温和唤醒时机")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build();
        manager.notify(NOTIFICATION_ID, notification);

        rescheduleNextDay(context);
    }

    /** 响铃后按持久化设置续排明天，避免第二天静默失效。 */
    private void rescheduleNextDay(Context context) {
        AlarmSettings settings = SettingsStore.load(context);
        if (!settings.getEnabled()) return;
        if (!AlarmScheduler.canScheduleExact(context)) return;

        // 显式以明天为锚点：响铃时刻的目标时间今天往往还没过，直接顺推会排到今天
        java.util.Calendar next = java.util.Calendar.getInstance();
        next.add(java.util.Calendar.DAY_OF_YEAR, 1);
        next.set(java.util.Calendar.HOUR_OF_DAY, settings.getHour());
        next.set(java.util.Calendar.MINUTE, settings.getMinute());
        next.set(java.util.Calendar.SECOND, 0);
        next.set(java.util.Calendar.MILLISECOND, 0);
        if (AlarmScheduler.schedule(context, next)) {
            SettingsStore.save(context, settings.withScheduledAt(next.getTimeInMillis()));
        }
    }

    private NotificationChannel wakeChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "智能唤醒", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("智能闹钟到点时的唤醒提醒通知");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        channel.setSound(alarmSound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 400, 300, 400});
        return channel;
    }
}
