package com.example.sleepsmart;
import android.app.*; import android.content.*;
public class WakeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i){
        String channel="sleep_wake"; NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(channel,"智能唤醒",NotificationManager.IMPORTANCE_HIGH));
        Notification n=new Notification.Builder(c,channel).setSmallIcon(com.example.sleepsmart.R.drawable.ic_launcher).setContentTitle("轻醒时间到").setContentText("根据你的睡眠窗口，现在是较温和的唤醒时机").setAutoCancel(true).build();
        nm.notify(1001,n);
    }
}
