package com.example.sleepsmart

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt

private val Ink = Color(0xFF1D2523)
private val CanvasColor = Color(0xFFF6F7F2)
private val Paper = Color(0xFFE9EEE8)
private val Moss = Color(0xFF39735F)
private val Sky = Color(0xFF789EB0)
private val Coral = Color(0xFFCC826B)
private val Deep = Color(0xFF3F566C)
private val Quiet = Color(0xFF6F7C77)
private val Rule = Color(0xFFD4DDD5)
private enum class Tab { TONIGHT, INSIGHTS, ALARM, PROFILE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SleepSmart() } }
}

@Composable private fun SleepSmart() {
    var tab by rememberSaveable { mutableStateOf(Tab.TONIGHT) }
    var hour by rememberSaveable { mutableIntStateOf(7) }; var minute by rememberSaveable { mutableIntStateOf(30) }
    var early by rememberSaveable { mutableIntStateOf(30) }; var late by rememberSaveable { mutableIntStateOf(30) }; var alarmOn by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current; val target = remember(hour, minute) { targetCalendar(hour, minute) }; val segments = remember(hour, minute) { VirtualSleepData.lastNight(target) }
    val result = remember(segments, early) { SleepAnalyzer.recommend(segments, target.time, early) }; val recommended = remember(result, late) { if (result.alarm.time > target.timeInMillis + late * 60000L) target.time else result.alarm }
    MaterialTheme(colorScheme = lightColorScheme(background = CanvasColor, surface = CanvasColor, primary = Moss, onBackground = Ink, onSurface = Ink)) {
        Scaffold(containerColor = CanvasColor, bottomBar = { BottomNav(tab) { tab = it } }) { p -> Column(Modifier.fillMaxSize().padding(p)) { AppHeader(tab); when(tab) {
            Tab.TONIGHT -> Tonight(result, segments, recommended) { tab = Tab.ALARM }; Tab.INSIGHTS -> Insights(result); Tab.ALARM -> Alarm(result, hour, minute, early, late, recommended, alarmOn, { hour=it.first; minute=it.second }, {early=it}, {late=it}, { scheduleAlarm(context, recommended); alarmOn=true }); Tab.PROFILE -> Profile()
        } } }
    }
}

@Composable private fun AppHeader(tab: Tab) { Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=18.dp), verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("轻醒", fontSize=21.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp); Text(when(tab){Tab.TONIGHT->"昨夜";Tab.INSIGHTS->"趋势";Tab.ALARM->"闹钟";Tab.PROFILE->"设置"}, color=Quiet, fontSize=12.sp) }; Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(8.dp).clip(CircleShape).background(Moss));Spacer(Modifier.width(6.dp));Text("已连接",color=Moss,fontSize=12.sp,fontWeight=FontWeight.Bold)} }; Box(Modifier.fillMaxWidth().height(1.dp).background(Rule)) }

@Composable private fun Tonight(result: SleepAnalyzer.Result, segments: List<SleepAnalyzer.Segment>, recommended: Date, openAlarm:()->Unit) { LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){ item{Text("昨夜",color=Quiet,fontSize=13.sp);Spacer(Modifier.height(7.dp));Text("睡眠记录",fontSize=31.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));SleepArc(result,segments)};item{SectionLine("睡眠数据","华为运动健康")};item{Overview(result)};item{SectionLine("明早闹钟","根据昨夜阶段设置")};item{Row(Modifier.fillMaxWidth().clickable{openAlarm()}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("设置唤醒时间",fontSize=19.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text("在目标时间前后选择合适阶段",color=Quiet,fontSize=13.sp)};Icon(Icons.Rounded.ArrowForward,"打开闹钟",tint=Moss)}};item{Text("建议时间 ${formatClock(recommended)} · ${result.reason}",color=Quiet,fontSize=12.sp)} } }

@Composable private fun Insights(result: SleepAnalyzer.Result) { LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(22.dp)){item{Text("睡眠趋势",fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("近 7 晚",color=Quiet,fontSize=13.sp)};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){listOf(64,73,68,81,76,84,78).forEachIndexed{i,h->Column(horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.width(25.dp).height(h.dp).background(if(i==6)Moss else Sky.copy(alpha=.4f)));Spacer(Modifier.height(6.dp));Text("${i+1}",color=Quiet,fontSize=10.sp)}}}};item{SectionLine("昨夜结构","${result.cycles} 个周期")};item{Column(verticalArrangement=Arrangement.spacedBy(13.dp)){StageRow("浅睡",result.lightMinutes,Moss);StageRow("深睡",result.deepMinutes,Deep);StageRow("REM",result.remMinutes,Coral)}};item{SectionLine("数据说明","阶段为设备估算")};item{Text("阶段数据用于日常参考。没有可用阶段时，闹钟会按设定的目标时间触发。",color=Quiet,fontSize=13.sp,lineHeight=21.sp)} } }

@Composable private fun Alarm(result: SleepAnalyzer.Result,hour:Int,minute:Int,early:Int,late:Int,recommended:Date,enabled:Boolean,onTime:(Pair<Int, Int>) -> Unit,onEarly:(Int)->Unit,onLate:(Int)->Unit,onToggle:()->Unit){val context=LocalContext.current;LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){item{Text("闹钟",fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text("拖动表盘调整时间",color=Quiet,fontSize=13.sp)};item{DialClock(hour*60+minute,early,late,onTime,onEarly,onLate)};item{Text("目标 ${formatTime(hour,minute)}  ·  建议 ${formatClock(recommended)}",color=Moss,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)};item{SectionLine("唤醒区间","实心指针为目标，空心指针为范围")};item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("目标时间",color=Quiet,fontSize=13.sp);Text(formatTime(hour,minute),fontSize=27.sp,fontWeight=FontWeight.Bold)};TextButton(onClick={TimePickerDialog(context,{_,h,m->onTime(h to m)},hour,minute,true).show()}){Icon(Icons.Rounded.Edit,null,modifier=Modifier.size(16.dp));Spacer(Modifier.width(5.dp));Text("手动设置",color=Moss)}}};item{Text(result.reason,color=Quiet,fontSize=12.sp,lineHeight=18.sp)};item{Button(onClick=onToggle,Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=if(enabled)Moss else Ink)){Icon(if(enabled)Icons.Rounded.Check else Icons.Rounded.Alarm,null);Spacer(Modifier.width(8.dp));Text(if(enabled)"闹钟已开启" else "开启闹钟",fontWeight=FontWeight.Bold)}}} }

@Composable private fun Profile(){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){item{Text("设置",fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("连接、通知和数据来源",color=Quiet,fontSize=13.sp)};item{SettingRow(Icons.Rounded.FavoriteBorder,"华为运动健康","已连接 · 最近同步今天 07:45",Moss)};item{SettingRow(Icons.Rounded.NotificationsNone,"通知","本机闹钟需要系统通知权限",Quiet)};item{SettingRow(Icons.Rounded.Info,"数据来源","睡眠阶段为设备估算",Quiet)};item{SectionLine("使用说明","请按日常参考使用")};item{Text("阶段数据不能替代医学诊断。窗口内没有可用阶段时，闹钟按目标时间触发。",color=Quiet,fontSize=13.sp,lineHeight=21.sp)}}}

@Composable private fun BottomNav(selected:Tab,onSelect:(Tab)->Unit){Row(Modifier.fillMaxWidth().background(CanvasColor).padding(horizontal=14.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceAround){NavItem(Icons.Rounded.NightsStay,"今晚",Tab.TONIGHT,selected,onSelect);NavItem(Icons.Rounded.Timeline,"趋势",Tab.INSIGHTS,selected,onSelect);NavItem(Icons.Rounded.Alarm,"闹钟",Tab.ALARM,selected,onSelect);NavItem(Icons.Rounded.PersonOutline,"我的",Tab.PROFILE,selected,onSelect)}}
@Composable private fun NavItem(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,value:Tab,selected:Tab,onSelect:(Tab)->Unit){Column(Modifier.width(70.dp).clickable{onSelect(value)}.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,contentDescription=label,tint=if(selected==value)Moss else Quiet,modifier=Modifier.size(21.dp));Spacer(Modifier.height(4.dp));Text(label,color=if(selected==value)Ink else Quiet,fontSize=11.sp,fontWeight=if(selected==value)FontWeight.Bold else FontWeight.Normal)}}
@Composable private fun SectionLine(title:String,meta:String){Row(Modifier.fillMaxWidth().padding(top=3.dp),verticalAlignment=Alignment.Bottom){Text(title,fontSize=17.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(meta,color=Quiet,fontSize=11.sp)};Spacer(Modifier.height(9.dp));Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))}
@Composable private fun Overview(r:SleepAnalyzer.Result){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("${r.sleepMinutes/60}h ${r.sleepMinutes%60}m","总睡眠",Moss);Metric("${r.cycles}","睡眠周期",Deep);Metric("${r.deepMinutes}m","深睡",Coral)}}
@Composable private fun Metric(v:String,l:String,c:Color){Column{Box(Modifier.width(28.dp).height(3.dp).background(c));Spacer(Modifier.height(8.dp));Text(v,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(l,color=Quiet,fontSize=12.sp)}}
@Composable private fun StageRow(l:String,m:Long,c:Color){Row(verticalAlignment=Alignment.CenterVertically){Text(l,Modifier.width(44.dp),fontSize=13.sp);Box(Modifier.weight(1f).height(8.dp).background(Paper)){Box(Modifier.fillMaxWidth((m/260f).coerceIn(.08f,1f)).fillMaxHeight().background(c))};Spacer(Modifier.width(12.dp));Text("${m}m",color=Quiet,fontSize=12.sp)}}
@Composable private fun SettingRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,detail:String,tint:Color){Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,contentDescription=null,tint=tint,modifier=Modifier.size(22.dp));Spacer(Modifier.width(14.dp));Column{Text(title,fontSize=16.sp,fontWeight=FontWeight.Bold);Text(detail,color=Quiet,fontSize=12.sp)}}}
@Composable private fun SleepArc(r:SleepAnalyzer.Result,segments:List<SleepAnalyzer.Segment>){Canvas(Modifier.fillMaxWidth().height(150.dp)){val w=size.width;val y=size.height*.55f;drawLine(Rule,Offset(0f,y),Offset(w,y),1.dp.toPx());val total=max(1L,r.sleepMinutes);var x=0f;segments.forEach{s->val mins=max(1L,(s.end.time-s.start.time)/60000);val width=w*mins/total;val color=when(s.stage){SleepAnalyzer.Stage.DEEP->Deep;SleepAnalyzer.Stage.REM->Coral;SleepAnalyzer.Stage.LIGHT->Moss;else->Quiet};drawLine(color,Offset(x,y),Offset(x+width,y),15.dp.toPx(),StrokeCap.Round);x+=width};drawLine(Ink,Offset(w*.84f,y-34.dp.toPx()),Offset(w*.84f,y+25.dp.toPx()),2.dp.toPx())}}
@Composable private fun WakeTimeline(early:Int,late:Int){Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${early}分钟前",color=Quiet,fontSize=12.sp);Text("目标",fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${late}分钟后",color=Quiet,fontSize=12.sp)};Spacer(Modifier.height(11.dp));Box(Modifier.fillMaxWidth().height(8.dp).background(Paper)){Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f).height(8.dp).background(Sky));Box(Modifier.size(14.dp).clip(CircleShape).background(Moss));Box(Modifier.weight(1f).height(8.dp).background(Coral))}}}}
@Composable private fun DialClock(target:Int,early:Int,late:Int,onTime:(Pair<Int,Int>)->Unit,onEarly:(Int)->Unit,onLate:(Int)->Unit){val dialSize=320.dp;val density=LocalDensity.current;val px=with(density){dialSize.toPx()};var active by remember{mutableStateOf("target")};Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Canvas(Modifier.size(dialSize).pointerInput(target,early,late){detectDragGestures(onDragStart={p->val c=Offset(px/2,px/2);val handles=listOf("start" to pointForMinute(target-early,c,px*.39f),"target" to pointForMinute(target,c,px*.39f),"end" to pointForMinute(target+late,c,px*.39f));active=handles.minBy{distance(it.second,p)}.first},onDrag={change,_->val c=Offset(px/2,px/2);val minute=minuteForPoint(change.position,c);when(active){"start"->onEarly(((target-minute+1440)%1440).coerceIn(5,90));"end"->onLate(((minute-target+1440)%1440).coerceIn(5,90));else->onTime(minute/60 to minute%60)};change.consume()})}){val c=Offset(size.width/2,size.height/2);val radius=size.minDimension*.4f;drawCircle(Paper,radius,c);drawCircle(Rule,radius,c,style=Stroke(1.dp.toPx()));val start=target-early;val sweep=early+late;drawArc(Moss.copy(alpha=.18f),start/1440f*360f-90f,sweep/1440f*360f,false,c-Offset(radius,radius),Size(radius*2,radius*2),style=Stroke(16.dp.toPx(),cap=StrokeCap.Round));for(i in 0 until 24){val a=i*2f*PI.toFloat()/24f-PI.toFloat()/2f;val r1=radius- if(i%6==0)14.dp.toPx() else 7.dp.toPx();val r2=radius+7.dp.toPx();drawLine(if(i%6==0)Ink else Rule,pointForAngle(a,c,r1),pointForAngle(a,c,r2),if(i%6==0)2.dp.toPx() else 1.dp.toPx(),cap=StrokeCap.Round)};listOf(target-early to Sky,target to Moss,target+late to Coral).forEachIndexed{i,(m,color)->val p=pointForMinute(m,c,radius);val inner=pointForMinute(m,c,radius*.52f);drawLine(color,c,inner,if(i==1)3.dp.toPx() else 2.dp.toPx(),cap=StrokeCap.Round);drawLine(color,inner,p,if(i==1)3.dp.toPx() else 2.dp.toPx(),cap=StrokeCap.Round);if(i==1){drawCircle(color,9.dp.toPx(),p);drawCircle(CanvasColor,4.dp.toPx(),p)}else{drawCircle(CanvasColor,9.dp.toPx(),p);drawCircle(color,9.dp.toPx(),p,style=Stroke(2.dp.toPx()))}};drawCircle(Ink,3.dp.toPx(),c)};Text(formatTime(target/60,target%60),fontSize=38.sp,fontWeight=FontWeight.Bold);Text("目标起床",color=Quiet,fontSize=11.sp,modifier=Modifier.padding(top=55.dp))}}
@Composable private fun RangeControl(early:Int,late:Int,onEarly:(Int)->Unit,onLate:(Int)->Unit){Column{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("提前 $early 分钟",color=Quiet,fontSize=12.sp);Text("延后 $late 分钟",color=Quiet,fontSize=12.sp)};Slider((early+late).toFloat(),{val total=it.toInt();val left=(total*early.toFloat()/(early+late).coerceAtLeast(1)).toInt().coerceIn(5,90);onEarly(left);onLate((total-left).coerceIn(5,90))},valueRange=10f..180f,steps=16,colors=SliderDefaults.colors(thumbColor=Moss,activeTrackColor=Moss))}}

private fun pointForAngle(angle:Float,center:Offset,radius:Float)=Offset(center.x+cos(angle)*radius,center.y+sin(angle)*radius)
private fun pointForMinute(minute:Int,center:Offset,radius:Float)=pointForAngle(minute/1440f*2f*PI.toFloat()-PI.toFloat()/2f,center,radius)
private fun minuteForPoint(point:Offset,center:Offset):Int{val angle=atan2(point.y-center.y,point.x-center.x)+PI.toFloat()/2f;val normalized=(angle+2f*PI.toFloat())%(2f*PI.toFloat());return ((normalized/(2f*PI.toFloat()))*1440f).toInt()/5*5}
private fun distance(a:Offset,b:Offset)=sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y))

private fun targetCalendar(h:Int,m:Int)=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,h);set(Calendar.MINUTE,m);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);if(timeInMillis<=System.currentTimeMillis()+300000)add(Calendar.DAY_OF_YEAR,1)}
private fun formatTime(h:Int,m:Int)=String.format(Locale.getDefault(),"%02d:%02d",h,m)
private fun formatClock(d:Date)=SimpleDateFormat("HH:mm",Locale.getDefault()).format(d)
private fun scheduleAlarm(context:Context,date:Date){val manager=context.getSystemService(AlarmManager::class.java);val p=PendingIntent.getBroadcast(context,1001,Intent(context,WakeReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!manager.canScheduleExactAlarms()){context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));Toast.makeText(context,"请允许精确闹钟权限后再开启",Toast.LENGTH_LONG).show();return};manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,date.time,p);Toast.makeText(context,"智能闹钟已设置为 ${formatClock(date)}",Toast.LENGTH_SHORT).show()}
