package com.example.sleepsmart.data

import com.example.sleepsmart.SleepAnalyzer
import com.example.sleepsmart.VirtualSleepData
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 睡眠页数据提供器：当前接 VirtualSleepData 演示数据源，
 * 经 SleepAnalyzer 计算后输出 JSON，注入 WebView 展示。
 * 接入华为 Health Kit 时仅替换本类的取数实现，桥协议与页面不动。
 */
object SleepDataProvider {

    private const val STAGE_KEY_LIGHT = "light"
    private const val STAGE_KEY_DEEP = "deep"
    private const val STAGE_KEY_REM = "rem"
    private const val STAGE_KEY_AWAKE = "awake"

    /** 生成睡眠页与趋势页需要的全部字段的 JSON 字符串。 */
    @JvmStatic
    fun json(hour: Int, minute: Int): String {
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 300_000L) add(Calendar.DAY_OF_YEAR, 1)
        }
        val segments = VirtualSleepData.lastNight(target)
        val result = SleepAnalyzer.recommend(segments, target.time, 30)
        val sleepStart = segments.first().start
        val sleepEnd = segments.last().end
        val totalMinutes = result.sleepMinutes.coerceAtLeast(1L)
        val upcoming = sleepStart.after(Date())

        val stages = JSONArray()
        segments.forEach { s ->
            stages.put(
                JSONObject()
                    .put("k", stageKey(s.stage))
                    .put("m", (s.end.time - s.start.time) / 60000L),
            )
        }

        val trend = JSONArray()
        val pattern = longArrayOf(392L, 448L, 425L, 466L, 431L, 452L, 490L)
        val weekdayCN = arrayOf("日", "一", "二", "三", "四", "五", "六")
        pattern.forEachIndexed { index, minutes ->
            val day = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index - (pattern.size - 1))
            }
            trend.put(
                JSONObject()
                    .put("label", weekdayCN[day.get(Calendar.DAY_OF_WEEK)])
                    .put("minutes", minutes)
                    .put("latest", index == pattern.size - 1),
            )
        }

        val recommended = Calendar.getInstance().apply { time = result.alarm }
        val recommendedMinute = recommended.get(Calendar.HOUR_OF_DAY) * 60 + recommended.get(Calendar.MINUTE)

        return JSONObject()
            .put("periodLabel", if (upcoming) "今晚 · 预演" else "昨夜")
            .put("dateText", SimpleDateFormat("M月d日 E", Locale.CHINA).format(sleepStart))
            .put("startText", SimpleDateFormat("HH:mm", Locale.getDefault()).format(sleepStart))
            .put("endText", SimpleDateFormat("HH:mm", Locale.getDefault()).format(sleepEnd))
            .put("totalMinutes", totalMinutes)
            .put("cycles", result.cycles)
            .put("deepMinutes", result.deepMinutes)
            .put("lightMinutes", result.lightMinutes)
            .put("remMinutes", result.remMinutes)
            .put("stages", stages)
            .put("recommendedMinute", recommendedMinute)
            .put("recommendedFraction", recommendedFraction(result.alarm, sleepStart, sleepEnd))
            .put("reason", result.reason)
            .put("trend", trend)
            .toString()
    }

    private fun recommendedFraction(recommended: Date, start: Date, end: Date): Double {
        val span = (end.time - start.time).coerceAtLeast(1L)
        return ((recommended.time - start.time).toDouble() / span).coerceIn(0.0, 1.0)
    }

    private fun stageKey(stage: SleepAnalyzer.Stage): String = when (stage) {
        SleepAnalyzer.Stage.DEEP -> STAGE_KEY_DEEP
        SleepAnalyzer.Stage.REM -> STAGE_KEY_REM
        SleepAnalyzer.Stage.LIGHT -> STAGE_KEY_LIGHT
        SleepAnalyzer.Stage.AWAKE -> STAGE_KEY_AWAKE
        SleepAnalyzer.Stage.UNKNOWN -> STAGE_KEY_AWAKE
    }
}
