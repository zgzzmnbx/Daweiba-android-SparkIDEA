# 项目 AGENTS.md

## 项目定位

这是一个 Android 手机 App 开发项目，当前阶段采用轻量命令行开发环境，由 Codex 负责主要代码编写、构建、安装和日志排查。优先使用 vivo X200 Pro 真机调试。

产品需求、当前功能、数据协议和未来规划以 `DabaweiFlashNote\PRD.md` 为准；`AGENTS.md` 只保留环境、协作、构建和验证规则。

## 协作规则

- 优先把大体积开发依赖放到 D 盘，避免占用 C 盘。
- 当前阶段已安装 Android Studio zip 版到 D 盘；不安装 Android Emulator、NDK、Flutter、React Native，除非后续需求明确需要。
- Android SDK 固定使用 `D:\Dev\Android\sdk`。
- JDK 固定使用 `D:\Dev\Java\jdk-17`。
- Gradle 缓存固定使用 `D:\Dev\.gradle`。
- Android Studio 固定使用 `D:\Dev\Android\AndroidStudio\android-studio`。
- Android Studio 优先通过 `D:\Dev\Android\AndroidStudio\Start-Android-Studio-D.cmd` 启动。
- Android Studio 的 config/system/plugins/log 固定使用 `D:\Dev\Android\AndroidStudio\profile`。
- Android 用户配置和 AVD 目录固定使用 `D:\Dev\Android\.android` 与 `D:\Dev\Android\.android\avd`。
- 新建 Android 项目时，优先使用项目内 Gradle Wrapper，不要求全局安装 Gradle。
- 如 Gradle 分发包下载不稳定，可先用 Android SDK Build Tools 手工构建最小 APK，确保真机链路先跑通，再补 Gradle 工程化。
- 修改项目配置后必须实际运行构建命令验证；能真机安装时，继续用 `adb install` 或 Gradle install task 验证。

## 验证方式

基础环境验证命令：

```powershell
java -version
sdkmanager --version
adb version
adb devices
sdkmanager --sdk_root=D:\Dev\Android\sdk --list_installed
```

Android Studio 检查路径：

```powershell
D:\Dev\Android\AndroidStudio\android-studio\bin\studio64.exe
D:\Dev\Android\AndroidStudio\Start-Android-Studio-D.cmd
```

真机验证要求：

- `adb devices` 能看到 vivo X200 Pro。
- 设备状态必须为 `device`，不能是 `unauthorized`。
- 如果设备不可见，优先检查手机 USB 调试、数据线模式、授权弹窗和 Windows 设备管理器驱动。

## 项目经验 / 注意事项

- C 盘空间不足，本项目不要把 SDK、Gradle 缓存、模拟器镜像放到 C 盘。
- 本项目固定归档、备份和推送到 GitHub 仓库 `https://github.com/zgzzmnbx/Daweiba-android-SparkIDEA`，默认分支为 `master`。涉及代码、配置、构建脚本、PRD、README 等重要变更时，先检查 `git status` 并完成本地提交或备份标签，再执行 `git push origin master`；不得使用强制推送覆盖远端历史。当前规则落盘后的配套备份标签为 `backup-20260806-git-archive-rule`。
- `C:\Users\zgzzm\.android` 可能已经存在少量 adb key 等旧配置，通常占用很小；后续新配置优先通过 `ANDROID_USER_HOME` 放到 D 盘。
- Android Studio 首次启动时，如提示安装 SDK 或模拟器，不要选默认 C 盘路径；SDK 选择 `D:\Dev\Android\sdk`，模拟器暂不安装。
- 新开 PowerShell 或 Codex 线程后，用户级环境变量才会天然生效；当前会话如找不到命令，先临时设置 `JAVA_HOME`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 和 `Path`。
- 每次版本更新或输出新的 `DabaweiFlashNote` APK 前，必须先备份核心代码，不再备份 APK；当前由 `DabaweiFlashNote\tools\backup-core-code.py` 和 `tools\build-apk.ps1` 自动执行，备份输出到 `DabaweiFlashNote\90-版本代码备份\`，命名为 `DabaweiFlashNote-版本号-日期.zip`。
- `tools\build-apk.ps1` 生成 `BuildInfo.java` 时必须使用无 BOM UTF-8；Windows PowerShell 的 `Set-Content -Encoding UTF8` 可能写入 BOM，导致 `javac` 报 `非法字符: '\ufeff'`。
- Windows 下新增提醒类较多时，d8 可能触发“命令行过长”；`tools\build-apk.ps1` 已固定使用无 BOM UTF-8 参数文件调用 d8，不要改回逐个展开 class 文件路径。
- 待办提醒数据库使用普通应用存储，`ReminderReceiver` 不声明 direct boot；提醒恢复依赖开机、系统时间和时区广播，修改提醒权限或通知权限时必须保留设置入口与非精确调度降级。
- P1 后台同步默认关闭，开启后由 JobScheduler 约每 6 小时尽力执行；系统省电、网络和厂商后台策略可能延迟，不能把它当作实时同步或提醒触发链路。
- 多级提醒按 `reminder_occurrences` 独立保存；具体截止时分按提前 24 小时 / 1 小时计算，仅日期按前一天 09:00、截止日 17:00 计算，修改主提醒或截止时间后必须重新调度 occurrence。
- 锁屏隐私同时由通知正文和通知可见性控制；修改设置后要重新创建通知通道并保持通知内不显示待办正文，不能只依赖系统锁屏设置。
