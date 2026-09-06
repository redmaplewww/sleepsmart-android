# 轻醒 SleepSmart（Android APK）

一个可构建的 Android 智能闹钟原型：默认假设华为运动健康已经提供授权后的睡眠阶段数据。前端为 **WebView + 本地新拟态页面**（`assets/neumorphic-alarm.html`），通过 JS 桥与原生交互；原生侧负责精确闹钟调度、持久化与到点唤醒。

## 架构（2026-09，WebView 前端版）

```
app/src/main/java/com/example/sleepsmart/
├── MainActivity.kt        # WebView 宿主 + JS 桥（状态持久化/闹钟同步/主题底色/睡眠数据注入）
├── assets/
│   └── neumorphic-alarm.html  # 四页前端（今晚/趋势/闹钟/我的），新拟态 + 5 套主题
├── data/
│   ├── SettingsStore.kt   # 闹钟偏好模型（AlarmSettings）+ SharedPreferences 持久化
│   └── SleepDataProvider.kt # 睡眠页/趋势页数据提供器（JSON 注入 WebView）
└── alarm/
    ├── AlarmScheduler.kt  # 精确闹钟排程/取消与权限检查
    └── BootReceiver.kt    # 重启后按持久化设置重新排程
WakeReceiver.java          # 到点唤醒通知（CATEGORY_ALARM + 闹钟音 + 振动）+ 次日自动续排
SleepAnalyzer.java         # 睡眠阶段分析算法（演示链路在用；华为接入后继续复用）
VirtualSleepData.java      # 演示数据源
```

## JS 桥协议（`window.AndroidAlarm`）

| 方法 | 说明 |
| --- | --- |
| `getState()` | 返回持久化的闹钟设置 JSON（hour/minute/early/late/on），页面初始化用 |
| `saveAndSync(h, m, early, late, on)` | 持久化设置并同步系统闹钟；开启时请求通知权限、检查精确闹钟权限 |
| `getSleepData()` | 返回睡眠页/趋势页数据 JSON（阶段序列、周期数、建议唤醒点、近 7 晚），当前为演示源 |
| `setWebBackground("rgb(r,g,b)")` | 主题切换时同步系统栏与 WebView 底色 |

页面在浏览器直接打开也能预览（无桥时保留静态演示数据与手机框外观；在 APK 内自动进入全屏 app 模式）。

## 功能

- 12 小时表盘：拖动主指针设置预计起床时间（5 分钟步进），扇形阴影为唤醒窗口，拖动两条边调整提前/延后（5–90 分钟）。
- 设置持久化：所有闹钟偏好写入本机，重启 App 后界面与真实闹钟状态一致；手机重启后由 `BootReceiver` 自动恢复排程。
- 到点唤醒：`CATEGORY_ALARM` 高优先级通知，闹钟音 + 振动，点击回到应用；响铃后若仍为开启状态，自动续排明天的闹钟。
- 权限：到点通知需要 Android 13+ 的通知权限（开启闹钟时自动请求）；精确闹钟在 Android 12+ 需要系统授权（缺少时引导跳转设置）。
- 睡眠页/趋势页：当前展示内置演示数据（经 `SleepAnalyzer` 计算周期与建议唤醒点），页面如实标注"演示数据"。

> 重要：这不是医疗器械，也不能"确保"唤醒时一定处于某个睡眠阶段。消费级设备的阶段标签是概率估计；没有阶段数据时，应用按用户目标时间兜底。

## 华为运动健康接入（规划中）

数据层已按可替换设计：`SleepDataProvider` 是页面取数的唯一入口，接 HMS Health Kit 时仅需把它的取数实现替换为 Health Kit 查询（授权流程 + 阶段数据映射到现有 JSON 协议），HTML 与桥协议不变。接入需要 HMS 开发者准入、应用签名与用户授权，应在真机上单独验收。

## 构建

```powershell
./gradlew.bat assembleDebug --no-daemon
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`（当前版本 0.4.0）。

## 开源范围

仓库提交 Android 前端源码、算法实现、产品文档和构建配置。Gradle 依赖缓存与构建产物由 `.gitignore` 排除。历史版本：纯 Compose 前端保存在 `compose-ui-backup` 分支，不再随 main 演进。

## 贡献

欢迎提交 Issue 或 Pull Request。涉及闹钟行为、数据源与桥协议的改动，请同时说明数据来源、测试设备和已知限制。

## License

本项目使用 MIT License，见 [LICENSE](LICENSE)。

详见 [SCIENCE_AND_INTEGRATION.md](SCIENCE_AND_INTEGRATION.md)。
