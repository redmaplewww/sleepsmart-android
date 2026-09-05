package com.example.sleepsmart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Deterministic test source used until the Huawei Health Service Kit adapter is approved. */
public final class VirtualSleepData {
    private VirtualSleepData() { }

    public static List<SleepAnalyzer.Segment> lastNight(Calendar target) {
        Calendar start = (Calendar) target.clone();
        start.add(Calendar.DAY_OF_YEAR, -1);
        start.set(Calendar.HOUR_OF_DAY, 23);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        String[] stages = {"LIGHT", "DEEP", "LIGHT", "REM", "LIGHT", "DEEP", "LIGHT", "REM", "LIGHT", "DEEP", "LIGHT", "REM", "LIGHT", "DEEP", "LIGHT"};
        int[] minutes = {20, 50, 25, 25, 40, 40, 40, 25, 45, 20, 40, 30, 50, 20, 20};
        List<SleepAnalyzer.Segment> result = new ArrayList<>();
        Calendar cursor = (Calendar) start.clone();
        for (int i = 0; i < stages.length; i++) {
            Calendar end = (Calendar) cursor.clone();
            end.add(Calendar.MINUTE, minutes[i]);
            result.add(new SleepAnalyzer.Segment(cursor.getTime(), end.getTime(), SleepAnalyzer.Stage.valueOf(stages[i])));
            cursor = end;
        }
        return result;
    }
}
