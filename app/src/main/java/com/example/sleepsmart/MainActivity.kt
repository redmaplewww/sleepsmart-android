package com.example.sleepsmart

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.util.Calendar
import java.util.Locale

/**
 * 轻醒 SleepSmart —— 原生 WebView 前端
 *
 * 前端为 assets/neumorphic-alarm.html（新拟态、四页、多主题），
 * 通过 JS 桥接 AndroidAlarm 与原生闹钟调度交互：
 *   - AndroidAlarm.scheduleAlarm(hour, minute)  设置系统精确闹钟
 *   - AndroidAlarm.cancelAlarm()                取消闹钟
 */
class MainActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.addJavascriptInterface(AlarmBridge(this), "AndroidAlarm")
        webView.loadUrl("file:///android_asset/neumorphic-alarm.html")
    }

    /** 供 WebView JS 调用的闹钟桥接（@JavascriptInterface 方法运行在后台线程，统一切回主线程） */
    inner class AlarmBridge(private val context: Context) {
        @JavascriptInterface
        fun scheduleAlarm(hour: Int, minute: Int) {
            runOnUiThread { scheduleExactAlarm(context, targetCalendar(hour, minute)) }
        }

        @JavascriptInterface
        fun cancelAlarm() {
            runOnUiThread { cancelScheduledAlarm(context) }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001

        private fun alarmPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WakeReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun targetCalendar(h: Int, m: Int): Calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // 若目标时间已过（含 5 分钟缓冲），顺延到明天
                if (timeInMillis <= System.currentTimeMillis() + 300_000L) add(Calendar.DAY_OF_YEAR, 1)
            }

        private fun scheduleExactAlarm(context: Context, date: Calendar) {
            val manager = context.getSystemService(AlarmManager::class.java)
            if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) {
                context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                Toast.makeText(context, "请允许精确闹钟权限后再开启", Toast.LENGTH_LONG).show()
                return
            }
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                date.timeInMillis,
                alarmPendingIntent(context)
            )
            val text = String.format(
                Locale.getDefault(), "%02d:%02d",
                date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE)
            )
            Toast.makeText(context, "智能闹钟已设置为 $text", Toast.LENGTH_SHORT).show()
        }

        private fun cancelScheduledAlarm(context: Context) {
            val manager = context.getSystemService(AlarmManager::class.java)
            manager.cancel(alarmPendingIntent(context))
            Toast.makeText(context, "已取消智能闹钟", Toast.LENGTH_SHORT).show()
        }
    }
}
