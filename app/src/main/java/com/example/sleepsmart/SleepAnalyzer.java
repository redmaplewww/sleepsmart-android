package com.example.sleepsmart;

import java.text.*;
import java.util.*;

/** Deterministic, explainable smart-alarm calculation. Input timestamps use yyyy-MM-dd HH:mm. */
public final class SleepAnalyzer {
    public enum Stage { AWAKE, LIGHT, DEEP, REM, UNKNOWN }
    public static final class Segment { public final Date start,end; public final Stage stage;
        Segment(Date s, Date e, Stage t){start=s;end=e;stage=t;} }
    public static final class Result { public Date sleepStart, sleepEnd, alarm; public int cycles; public long sleepMinutes, lightMinutes, deepMinutes, remMinutes; public String reason; }
    private static final SimpleDateFormat F = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    static { F.setLenient(false); }
    public static Date parseDate(String s) throws ParseException { return F.parse(s.trim()); }
    public static Result recommend(List<Segment> segs, Date target, int windowMinutes){
        if(segs.isEmpty()) throw new IllegalArgumentException("没有睡眠阶段数据");
        Result r=new Result(); r.sleepStart=segs.get(0).start; r.sleepEnd=segs.get(segs.size()-1).end;
        r.sleepMinutes=0; r.lightMinutes=0; r.deepMinutes=0; r.remMinutes=0;
        for(Segment s:segs){ long minutes=Math.max(0,(s.end.getTime()-s.start.getTime())/60000); if(s.stage==Stage.LIGHT)r.lightMinutes+=minutes; else if(s.stage==Stage.DEEP)r.deepMinutes+=minutes; else if(s.stage==Stage.REM)r.remMinutes+=minutes; if(s.stage!=Stage.AWAKE)r.sleepMinutes+=minutes; }
        r.cycles=detectCycles(segs,r.sleepMinutes);
        long from=target.getTime()-windowMinutes*60000L, to=target.getTime(); Segment best=null; double bestScore=-1;
        for(Segment s:segs){ if(s.end.getTime()<from||s.start.getTime()>to) continue; long t=Math.max(from,s.start.getTime());
            double stageScore=s.stage==Stage.LIGHT?1.0:s.stage==Stage.REM?0.92:s.stage==Stage.AWAKE?0.84:s.stage==Stage.UNKNOWN?0.45:0.05;
            double proximity=1.0-Math.abs(target.getTime()-t)/(double)Math.max(1,windowMinutes*60000L)*0.35;
            double score=stageScore*0.75+proximity*0.25; if(score>bestScore){bestScore=score;best=s;}
        }
        if(best!=null){ r.alarm=new Date(Math.max(from,best.start.getTime())); r.reason="窗口内检测到"+label(best.stage)+"，优先在该阶段唤醒"; }
        else { r.alarm=target; r.reason="窗口内没有可用阶段，按目标时间兜底；请勿把结果视为医学诊断"; }
        return r;
    }
    /** Virtual/consumer sleep staging heuristic: count completed NREM-to-REM arcs, with a 90-minute fallback. */
    private static int detectCycles(List<Segment> segs,long sleepMinutes){
        int arcs=0; boolean nrem=false;
        for(Segment s:segs){ if(s.stage==Stage.LIGHT||s.stage==Stage.DEEP)nrem=true; if(s.stage==Stage.REM&&nrem){arcs++; nrem=false;} }
        return Math.max(1,arcs>0?arcs:(int)Math.round(sleepMinutes/90.0));
    }
    public static String format(Date d){return F.format(d);}
    public static String label(Stage s){switch(s){case LIGHT:return "浅睡";case REM:return "REM";case DEEP:return "深睡";case AWAKE:return "清醒";default:return "未知";}}
}
