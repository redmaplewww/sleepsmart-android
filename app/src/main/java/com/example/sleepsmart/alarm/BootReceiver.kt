package com.example.sleepsmart.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleepsmart.data.SettingsStore

/** 重启会清除系统闹钟；开机后按持久化设置重新排程。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore.load(context)
        if (!settings.enabled) return
        if (!AlarmScheduler.canScheduleExact(context)) return
        val next = AlarmScheduler.nextOccurrence(settings.hour, settings.minute)
        if (AlarmScheduler.schedule(context, next)) {
            SettingsStore.save(context, settings.withScheduledAt(next.timeInMillis))
        }
    }
}
