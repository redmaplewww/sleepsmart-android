package com.example.sleepsmart.data

import android.content.Context

/** 闹钟偏好，持久化在本机；WebView 前端通过 JS 桥读写同一份状态。 */
data class AlarmSettings(
    val hour: Int,
    val minute: Int,
    val earlyMinutes: Int,
    val lateMinutes: Int,
    val enabled: Boolean,
    val scheduledAtMillis: Long,
) {
    companion object {
        val DEFAULT = AlarmSettings(
            hour = 7,
            minute = 30,
            earlyMinutes = 30,
            lateMinutes = 30,
            enabled = false,
            scheduledAtMillis = 0L,
        )
    }

    fun withScheduledAt(millis: Long): AlarmSettings = copy(scheduledAtMillis = millis)
}

object SettingsStore {
    private const val FILE = "sleepsmart_settings"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_EARLY = "early_minutes"
    private const val KEY_LATE = "late_minutes"
    private const val KEY_ENABLED = "alarm_enabled"
    private const val KEY_SCHEDULED_AT = "scheduled_at"

    @JvmStatic
    fun load(context: Context): AlarmSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return AlarmSettings(
            hour = prefs.getInt(KEY_HOUR, AlarmSettings.DEFAULT.hour).coerceIn(0, 23),
            minute = prefs.getInt(KEY_MINUTE, AlarmSettings.DEFAULT.minute).coerceIn(0, 59),
            earlyMinutes = prefs.getInt(KEY_EARLY, AlarmSettings.DEFAULT.earlyMinutes).coerceIn(5, 90),
            lateMinutes = prefs.getInt(KEY_LATE, AlarmSettings.DEFAULT.lateMinutes).coerceIn(5, 90),
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            scheduledAtMillis = prefs.getLong(KEY_SCHEDULED_AT, 0L),
        )
    }

    @JvmStatic
    fun save(context: Context, settings: AlarmSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putInt(KEY_EARLY, settings.earlyMinutes)
            .putInt(KEY_LATE, settings.lateMinutes)
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putLong(KEY_SCHEDULED_AT, settings.scheduledAtMillis)
            .apply()
    }
}
