# 大尾巴闪念

Android 本地闪念捕捉 App。核心目标是打开即写、下拉保存、时间流回看、待办聚合，并同步到个人 Obsidian vault。

详细产品范围、当前功能、数据协议和未来需求见：

- [PRD.md](PRD.md)

项目环境、构建约定、真机调试规则见上级目录：

- [../AGENTS.md](../AGENTS.md)

## 当前状态

- 当前构建目标：`versionCode=52`，`versionName=0.6.0-shadcn-rhea-ui`；本地构建、签名和包信息核验已通过。
- 当前 P0/P1/P1.1/P1.2：已完成待办提醒协议、SQLite 对账、单次与多级本地调度、每日待办概览、低频后台同步、统一自然语言自动提醒、Obsidian 待办稳定基准、防漂移/防复活、撤销/修改反馈、冲突时间提示、通知隐私策略、提醒诊断、重启/时间变化恢复、权限降级提示，以及待办提醒同步推送到可配置的飞书机器人。
- 自然语言自动提醒仅作用于 App 待办和同步进入手机的 Obsidian 未完成待办；日期无具体时分默认 08:00，相对分钟/小时按稳定基准精确顺延。下一开发目标转为 P2 规划，需求以 `PRD.md` 7.5 为准。
- 真机验证状态（2026-08-06）：已通过 ADB 将 v0.50 APK 安装到 vivo V2405/V2405A，包管理器确认 `versionCode=50`、`versionName=0.50-auto-natural-reminders` 并成功启动；已现场验证本地待办自然语言自动识别、提醒展示和“撤销自动提醒”，撤销后重启 App 未复活。随后手机在通话状态下断开 ADB，2 分钟到点通知、1 小时、三天默认 08:00、Obsidian 同步及锁屏/息屏/重启等场景仍待设备重新连接后补测。
- 飞书推送（2026-08-07）：v0.52 将自动识别提醒改为不弹确认窗口的轻量提示，并将飞书推送改为结构化消息卡片，卡片显示待办内容、提醒时间和来源“大尾巴闪念.手机端”；Webhook 默认启用且仍可在设置页关闭或替换。v0.52 已通过纯 Java 测试、静态检查、构建和签名校验；vivo 真机安装被系统拒绝，当前设备包管理器仍为 v0.51，待开启 USB 安装授权后补装。
- Rhea UI（2026-08-07）：已按 preset `b1au7YYAi` 完成原生 XML + Java 的 Rhea / Neutral / Blue 双主题重绘：语义令牌、浅/深资源、`跟随系统 / 浅色 / 深色` 偏好、八个旧主题迁移、首页/历史/待办/设置/快速捕捉/弹窗/桌面组件资源和敏感字段遮罩均已接入；原有 14 个图标按基线恢复，提醒/同步标签改为浅语义组合；修复待办页占位和设置页状态栏/固定底导，并加入随包苹方阅读字体（与 Claude 互斥）。未改变 SQLite、WebDAV、提醒、飞书、分享或图片链路。五项既有回归、新 UI 契约、故障注入红→绿、构建签名均已通过；当前 vivo 真机锁屏，待解锁后补做 16 张现场截图，详见上级 `BLOCKED.md`。
- P1 后台同步默认关闭，开启后由 JobScheduler 约每 6 小时尽力执行；已保存到手机的提醒不依赖后台同步。
- Obsidian 待办同步插件：`v1.3.0`；原始任务笔记不做批量回写，仅更新单一同步文件。
- 当前技术栈：原生 Android XML + Java，手工 Android SDK 构建。
- 当前包名：`com.dabawei.flashnote`。
- 当前产品名：`大尾巴闪念`。
- 命名说明：`dabawei` 是早期拼写债务，正确拼写应为 `daweiba`；包名修改涉及 Android 数据迁移，暂不贸然调整。
- 当前交付状态：`0.6.0-shadcn-rhea-ui` 代码和本地 APK 已完成；真机当前被锁屏拦截，不能把本地构建通过写成视觉验收完成。详见 `PRD.md` 7.6 节、`../BLOCKED.md` 和 `../PROGRESS.md`。

## Git 归档与推送

- 固定归档仓库：[zgzzmnbx/Daweiba-android-SparkIDEA](https://github.com/zgzzmnbx/Daweiba-android-SparkIDEA)。
- 默认分支：`master`。
- 重要代码、配置、构建脚本、PRD 或 README 变更后，先完成本地提交或备份标签，再推送到 `origin/master`；不得强制覆盖远端历史。
- 本次规则写入项目文件后的备份标签：`backup-20260806-git-archive-rule`。

## 构建

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\build-apk.ps1
```

输出：

```text
build\outputs\DabaweiFlashNote-debug.apk
```

构建前会自动备份核心代码到：

```text
90-版本代码备份\
```

## 测试

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\test-hello.ps1
powershell -ExecutionPolicy Bypass -File .\tools\test-markdown-exporter.ps1
powershell -ExecutionPolicy Bypass -File .\tools\test-reminder-p0.ps1
powershell -ExecutionPolicy Bypass -File .\tools\test-reminder-p1.ps1
powershell -ExecutionPolicy Bypass -File .\tools\test-feishu.ps1
```

## 安装到真机

先确认 `adb devices` 中设备状态为 `device`，再执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\install-apk.ps1
```

## 关键外部文件

- 闪念同步目标：`C:\OBS\Damon\【MOC】闪念-随手记.md`
- 待办同步入口：`C:\OBS\Damon\【MOC】大尾巴待办同步.md`
- Obsidian 待办同步插件：`C:\OBS\Damon\.obsidian\plugins\dabawei-todo-sync`

## 维护规则

- 功能需求、未来规划、数据协议优先写入 [PRD.md](PRD.md)。
- 环境路径、构建工具、真机调试规则优先写入 [../AGENTS.md](../AGENTS.md)。
- README 只作为项目入口，不再堆积长版本流水账。
