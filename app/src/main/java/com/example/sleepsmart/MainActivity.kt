package com.example.sleepsmart

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sleepsmart.alarm.AlarmScheduler
import com.example.sleepsmart.data.AlarmSettings
import com.example.sleepsmart.data.SettingsStore
import com.example.sleepsmart.data.SleepDataProvider
import org.json.JSONObject

/**
 * 轻醒 SleepSmart —— 原生 WebView 前端
 *
 * 前端为 assets/neumorphic-alarm.html（新拟态、四页、多主题），
 * 通过 JS 桥 AndroidAlarm 与原生交互：
 *   - getState()                       读取持久化的闹钟设置（页面初始化用）
 *   - saveAndSync(h, m, early, late, on) 持久化设置并同步系统闹钟（开启时请求通知权限）
 *   - setWebBackground("rgb(r,g,b)")    主题切换时同步系统栏与 WebView 底色
 *   - getSleepData()                    睡眠页/趋势页数据（当前为演示源，待接华为 Health Kit）
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
        }
        // targetSdk 35+ 强制 edge-to-edge：让页面内容避开系统栏
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        webView.addJavascriptInterface(AlarmBridge(this, webView), "AndroidAlarm")
        webView.loadUrl("file:///android_asset/neumorphic-alarm.html")
    }

    /** 供 WebView JS 调用的桥接。@JavascriptInterface 方法运行在后台线程，UI 操作统一切回主线程。 */
    private class AlarmBridge(
        private val activity: MainActivity,
        private val webView: WebView,
    ) {
        private val context: Context = activity

        @JavascriptInterface
        fun getState(): String {
            val s = SettingsStore.load(context)
            return JSONObject()
                .put("hour", s.hour)
                .put("minute", s.minute)
                .put("early", s.earlyMinutes)
                .put("late", s.lateMinutes)
                .put("on", s.enabled)
                .toString()
        }

        @JavascriptInterface
        fun getSleepData(): String = SleepDataProvider.json(readHour(), readMinute())

        @JavascriptInterface
        fun saveAndSync(hour: Int, minute: Int, early: Int, late: Int, on: Boolean) {
            val clamped = SettingsStore.load(context).copy(
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                earlyMinutes = early.coerceIn(5, 90),
                lateMinutes = late.coerceIn(5, 90),
                enabled = on,
            )
            SettingsStore.save(context, clamped)
            activity.runOnUiThread { activity.applyAlarmState(clamped) }
        }

        /** 主题切换时同步系统栏与 WebView 底色，参数形如 "rgb(250, 249, 245)"。 */
        @JavascriptInterface
        fun setWebBackground(cssColor: String) {
            val rgb = Regex("\\d+").findAll(cssColor).map { it.value.toInt() }.toList()
            if (rgb.size < 3) return
            val color = Color.rgb(rgb[0], rgb[1], rgb[2])
            activity.runOnUiThread {
                webView.setBackgroundColor(color)
                activity.window.statusBarColor = color
                activity.window.navigationBarColor = color
                val controller = WindowCompat.getInsetsController(activity.window, webView)
                controller.isAppearanceLightStatusBars = ColorUtils.calculateLuminance(color) > 0.5
                controller.isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(color) > 0.5
            }
        }

        private fun readHour(): Int = SettingsStore.load(context).hour
        private fun readMinute(): Int = SettingsStore.load(context).minute
    }

    // ===== 闹钟状态落地（主线程） =====

    internal fun applyAlarmState(settings: AlarmSettings) {
        if (!settings.enabled) {
            AlarmScheduler.cancel(this)
            SettingsStore.save(this, settings.withScheduledAt(0L))
            toast("已取消智能闹钟")
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        if (!AlarmScheduler.canScheduleExact(this)) {
            startActivity(AlarmScheduler.exactAlarmSettingsIntent())
            toast("请先授予“闹钟和提醒”权限")
            return
        }
        val target = AlarmScheduler.nextOccurrence(settings.hour, settings.minute)
        if (AlarmScheduler.schedule(this, target)) {
            SettingsStore.save(this, settings.withScheduledAt(target.timeInMillis))
            val time = "%02d:%02d".format(target.get(java.util.Calendar.HOUR_OF_DAY), target.get(java.util.Calendar.MINUTE))
            toast("智能闹钟已设置为 $time")
        } else {
            toast("未能设置闹钟，请检查权限")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
