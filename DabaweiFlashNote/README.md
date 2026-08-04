# 大尾巴闪念

Android 本地闪念捕捉 App。核心目标是打开即写、下拉保存、时间流回看、待办聚合，并同步到个人 Obsidian vault。

详细产品范围、当前功能、数据协议和未来需求见：

- [PRD.md](PRD.md)

项目环境、构建约定、真机调试规则见上级目录：

- [../AGENTS.md](../AGENTS.md)

## 当前状态

- 当前 APK 基线：`versionCode=48`，`versionName=0.48-todo-reminders`。
- 当前 P0：已完成待办提醒协议、SQLite 对账、单次本地调度、通知操作、重启/时间变化恢复、Obsidian 插件字段同步和权限降级提示。
- Obsidian 待办同步插件：`v1.3.0`；原始任务笔记不做批量回写，仅更新单一同步文件。
- 当前技术栈：原生 Android XML + Java，手工 Android SDK 构建。
- 当前包名：`com.dabawei.flashnote`。
- 当前产品名：`大尾巴闪念`。
- 命名说明：`dabawei` 是早期拼写债务，正确拼写应为 `daweiba`；包名修改涉及 Android 数据迁移，暂不贸然调整。

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
