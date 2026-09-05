# 华为运动健康接入清单

当前 APK 已完成两功能产品界面和本地闹钟流程，按“华为 Health Service Kit 已连接”进行产品化设计。华为侧需要同时区分三件事：AGC 中登记的 Android 应用、账号服务中的可发布产品，以及 Health Service Kit 数据权限申请。仅有 APK 或普通 AGC 应用登记，不能直接读取华为运动健康数据。

## 本项目固定信息

- Android applicationId：`com.example.sleepsmart`
- 华为 AGC APP ID：`118860991`（已创建；不是“已上传 APK”或“已获 Health Service Kit 权限”的证明）
- 华为 AGC 项目 ID：`101653523864930637`
- 当前调试签名 SHA-256：`54:96:A4:36:C7:16:DD:11:00:C8:0F:2B:2D:9C:2C:A9:38:71:87:B1:AD:2F:10:E9:0A:24:7D:31:ED:AF:C8:5C`
- 调试 APK：`app/build/outputs/apk/debug/app-debug.apk`
- 界面截图草稿：`outputs/sleep-checklist-20260904/截图草稿/01_睡眠分析.png`、`02_智能闹钟.png`（仅按当前 APK 界面生成的申请预览，提交前应替换为真机系统截屏）
- 申请表 Scheme 地址：`sleepsmart://health/sync`

发布包必须使用单独的 release keystore；release SHA-256 不能沿用上面的调试指纹。

Scheme 用于华为运动健康完成手动同步后回调到 APK，不是网页 URL。当前工程已在 `MainActivity` 声明该 Deep Link，并在收到回调时提示重新读取睡眠数据。

## 当前状态和下一步

1. AGC Android 应用已经创建，APP ID 为 `118860991`，包名为 `com.example.sleepsmart`。这一步不等于 APK 已上传到华为应用市场。
2. “申请账号服务”中必须有一个产品，产品类型选择“移动应用”，包名仍填写 `com.example.sleepsmart`；如果 Health Service Kit 表单显示“没有可以发布服务的产品”，先回到管理中心完成这一步。
3. 在该产品中配置调试 SHA-256；发布包另行配置 release SHA-256。包名和签名必须与 APK 一致。
4. 在“Health Service Kit → 申请 Health Service Kit 服务”中选择 Android 应用，最小化申请：睡眠读取和近一周历史读取。当前申请材料副本已按这两个权限填写。
5. 申请页还需要真实隐私政策 URL、用户协议 URL、姓名、电话、邮箱、测试结束时间、开发者信息和 APK 功能截图；这些不能用占位内容代填。当前目录中的截图为界面草稿，不是设备运行证据。
6. 审核通过后下载该应用对应的 `agconnect-services.json`，再接入 HMS Health SDK 并在真机上完成授权/读取联调。不要把该文件提交到公共仓库或聊天中。
7. 在华为运动健康 App 中给用户授权睡眠/科学睡眠读取权限，并确认设备已同步睡眠分期数据。

## 当前阻塞点

个人开发者还要注意华为当前规则：未上架华为应用市场的个人移动应用，或非移动应用，可能不开放 Health Service Kit 数据；高阶数据不向个人开发者开放。若个人开发者资质不满足审核要求，需要先上架移动应用或改用企业开发者账号，并只申请基础数据。

在拿到华为审核通过的服务、`agconnect-services.json` 和 release 签名信息前，本项目不能声称“已接通华为数据”；当前可验证的是 Android APK、两功能界面、本地闹钟和申请材料工程字段。
