package com.example.sleepsmart.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.sleepsmart.WakeReceiver
import java.util.Calendar

/** 精确闹钟的设置与取消；同一个 PendingIntent 既是排程凭据也是取消凭据。 */
object AlarmScheduler {
    private const val REQUEST_CODE = 1001

    @JvmStatic
    fun wakeOperation(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WakeReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Android 12+ 需要用户在系统设置中授予“闹钟和提醒”。 */
    @JvmStatic
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    @JvmStatic
    fun exactAlarmSettingsIntent(): Intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)

    /** 目标时间已过（含 5 分钟缓冲）时顺延到明天。 */
    @JvmStatic
    fun nextOccurrence(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis() + 300_000L) add(Calendar.DAY_OF_YEAR, 1)
    }

    /** 设置闹钟；缺少精确闹钟权限时返回 false，由调用方引导授权。 */
    @JvmStatic
    fun schedule(context: Context, at: Calendar): Boolean {
        if (!canScheduleExact(context)) return false
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.timeInMillis, wakeOperation(context))
        return true
    }

    @JvmStatic
    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(wakeOperation(context))
    }
}
