# Android App 开发环境项目

## 项目目标

本项目用于通过 Codex 全程协作开发 Android 手机 App。当前阶段采用轻量命令行开发环境，已安装 Android Studio zip 版，不安装模拟器，优先使用 vivo X200 Pro 真机调试，并尽量把大体积文件放到 D 盘。

当前应用方向：`大尾巴闪念`，一个打开即写、一键保存、时间流回看、搜索、Markdown 导出的本地闪念捕捉 App。

产品需求、当前功能、数据协议和未来规划已沉淀到 `DabaweiFlashNote\PRD.md`；本 README 主要保留开发环境、构建链路和历史交接信息。

## 当前环境状态

- 系统：Windows，PowerShell
- 项目目录：`D:\Codex-Temp\260606-android-dev`
- 开发工具根目录：`D:\Dev`
- Git：已存在，路径 `C:\Program Files\Git\cmd\git.exe`
- JDK：已安装 Temurin JDK 17，路径 `D:\Dev\Java\jdk-17`
- Android SDK：已安装，路径 `D:\Dev\Android\sdk`
- Gradle 缓存：已指定到 `D:\Dev\.gradle`
- Android Studio：已安装 zip 版，路径 `D:\Dev\Android\AndroidStudio\android-studio`
- Android Studio 启动脚本：`D:\Dev\Android\AndroidStudio\Start-Android-Studio-D.cmd`

## 已安装 Android SDK 包

- `platform-tools` 37.0.0
- `platforms;android-35`
- `build-tools;35.0.0`

## 环境变量

用户级环境变量已设置：

```powershell
JAVA_HOME=D:\Dev\Java\jdk-17
ANDROID_HOME=D:\Dev\Android\sdk
ANDROID_SDK_ROOT=D:\Dev\Android\sdk
GRADLE_USER_HOME=D:\Dev\.gradle
ANDROID_USER_HOME=D:\Dev\Android\.android
ANDROID_AVD_HOME=D:\Dev\Android\.android\avd
```

用户级 `Path` 已加入：

```powershell
D:\Dev\Java\jdk-17\bin
D:\Dev\Android\sdk\cmdline-tools\latest\bin
D:\Dev\Android\sdk\platform-tools
```

## 验证结果

已验证：

- `java -version` 可运行，版本为 Temurin 17.0.19
- `sdkmanager --version` 可运行，版本为 19.0
- `adb version` 可运行，版本为 37.0.0-14910828
- `sdkmanager --list_installed` 可列出已安装 SDK 包
- Android Studio zip 包 SHA-256 已校验，通过官方校验值 `7a902e8447c24bfa2cf1010173811a0568b7be120b6d14614deaf4285b80ec8c`
- Android Studio 配置、缓存、插件、日志路径已改到 `D:\Dev\Android\AndroidStudio\profile`

真机验证：

- vivo X200 Pro 已连接并授权，`adb devices -l` 显示状态为 `device`。
- 当前应用：`DabaweiFlashNote`，包名 `com.dabawei.flashnote`。
- 已通过 `adb shell am start -n com.dabawei.flashnote/.MainActivity` 启动应用。
- 旧测试包 `com.dabawei.helloworld` 已从手机卸载。

## 下一步

1. 使用 `D:\Dev\Android\AndroidStudio\Start-Android-Studio-D.cmd` 启动 Android Studio。
2. 首次启动如提示 SDK，选择已存在的 `D:\Dev\Android\sdk`，不要重新下载 SDK 或模拟器。
3. 后续开发优先基于 `DabaweiFlashNote` 工程继续扩展“大尾巴闪念”。
4. 如需重新安装，进入 `DabaweiFlashNote` 后运行 `powershell -ExecutionPolicy Bypass -File .\tools\install-apk.ps1`。

## 最近交接摘要

- 2026-06-06：完成第一阶段 Android 命令行开发环境安装。大体积 JDK、Android SDK、Gradle 缓存均放在 D 盘；未安装 Android Studio、模拟器、NDK、Flutter、React Native。
- 2026-06-06：补充安装 Android Studio Quail 1 zip 版到 D 盘，并将 IDE 配置、缓存、插件、日志路径固定到 D 盘；仍未安装模拟器、NDK、Flutter、React Native。
- 2026-06-06：先用最小 Android App 跑通资源测试、手工 SDK 构建、APK 签名校验、vivo X200 Pro 真机安装和 adb 启动验证。
- 2026-06-06：扩展为“大尾巴闪念”MVP，包含打开即写、一键保存、SQLite 本地时间流、搜索、Markdown 导出；已通过资源测试、Markdown 导出测试、APK 构建和签名验证。
- 2026-06-06：工程目录改名为 `DabaweiFlashNote`，包名改为 `com.dabawei.flashnote`，APK 输出改为 `DabaweiFlashNote-debug.apk`；已安装到 vivo X200 Pro 并通过 adb 启动，旧测试包 `com.dabawei.helloworld` 已卸载。
- 2026-06-06：完成 UI v0.2，美化标题区间距，新增纸张、夜墨、森绿三套主题和本地主题记忆；已重新推送 APK 到 vivo X200 Pro 并启动。
- 2026-06-06：完成桌面组件 v0.3，新增 `FlashNoteWidgetProvider` 和 `QuickCaptureActivity`；APK 元数据提升到 `versionCode=3`、`versionName=0.3-widget`，已重新推送到 vivo X200 Pro。
- 2026-06-06：完成 WebDAV 锚点同步 v0.4，新增同步设置页、锚点插入、WebDAV GET/PUT 覆盖上传、SQLite 同步状态；APK 元数据提升到 `versionCode=4`、`versionName=0.4-webdav`。已通过测试和构建签名验证；当前 adb 设备列表为空，待手机重新连接后推送。
- 2026-06-06：完成批量同步 v0.5，保存闪念只写入本地待同步队列，不再逐条 WebDAV；同步设置页新增“立即同步待同步”，一次下载目标 Markdown、插入多条待同步闪念、一次上传并批量标记状态。APK 元数据提升到 `versionCode=5`、`versionName=0.5-batch-sync`。已通过测试和构建签名验证；当前 adb 设备列表为空，待手机重新连接后推送。
- 2026-06-06：完成坚果云默认配置 v0.6，同步设置页默认启用 WebDAV，并内置坚果云服务器、账号、应用密码和目标 Markdown 路径；新增 WebDAV 路径编码器，支持 `OBS\Damon\【MOC】随手记-Claw编辑版.md` 这类路径。APK 元数据提升到 `versionCode=6`、`versionName=0.6-nutstore-defaults`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成设置页整理 v0.7，主界面只保留“同步”按钮和齿轮设置；主题切换、WebDAV 同步配置和 Markdown 导出统一放入设置页；时间流中已同步笔记显示“已同步”标记。APK 元数据提升到 `versionCode=7`、`versionName=0.7-settings-sync-badge`，已安装并启动到 vivo X200 Pro。
- 2026-06-06：完成删除笔记 v0.8，时间流卡片新增“删除”按钮，删除后刷新本地 SQLite 和桌面组件；同步按钮保持绿色实心按钮造型。APK 元数据提升到 `versionCode=8`、`versionName=0.8-delete-note`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成删除图标 v0.9，将时间流卡片的文字删除按钮改为小垃圾桶图标按钮，保留删除逻辑和可访问性描述。APK 元数据提升到 `versionCode=9`、`versionName=0.9-trash-icon`。已通过测试和构建签名验证；当前 adb 设备列表为空，待手机重新连接后推送。
- 2026-06-06：完成保存为待办 v0.10，保存按钮旁新增“保存为待办”；本地 SQLite 新增 `note_type`，同步到 Obsidian Markdown 时待办输出为 `- [ ] 时间 内容 #待办`。APK 元数据提升到 `versionCode=10`、`versionName=0.10-save-todo`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成待办标签 v0.11，时间流中待办笔记显示商务蓝“待办”标签；同步到 Obsidian 的待办格式和 `#待办` 标签保持不变。APK 元数据提升到 `versionCode=11`、`versionName=0.11-todo-badge`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成纸面工作流标签 v0.12，时间流标签顺序改为“待办”在前、“已同步”在后；待办保持商务蓝实心胶囊，已同步改为轻量绿色描边胶囊。APK 元数据提升到 `versionCode=12`、`versionName=0.12-paper-badges`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成设计主题 v0.13，设置页主题循环新增 Apple、Linear、Notion、Raycast、Obsidian 五套风格，加上原有纸张、夜墨、森绿共 8 套主题。APK 元数据提升到 `versionCode=13`、`versionName=0.13-design-themes`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-06：完成主题下拉与动作按钮主题化 v0.14，设置页主题切换改为下拉菜单；保存、保存为待办、同步三个按钮按不同主题使用独立动作色。APK 元数据提升到 `versionCode=14`、`versionName=0.14-theme-dropdown-buttons`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-07：完成同步标签后置 v0.16，普通笔记同步为 `- 时间 内容 #闪念`，待办笔记同步为 `- [ ] 时间 内容 #闪念 #待办`。APK 元数据提升到 `versionCode=16`、`versionName=0.16-sync-tags-after-content`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-08：完成纸张主题 UI 重绘与记录操作菜单 v0.17，主界面改为白底深绿纸面风格、状态提示、时间线卡片；点击记录可编辑、重新同步、切换待办/普通笔记，删除前增加确认。APK 元数据提升到 `versionCode=17`、`versionName=0.17-paper-ui-note-actions`。已通过测试和构建签名验证；当前 adb 未发现设备，待手机连接后推送。
- 2026-06-08：完成纸张主题现代视觉重构 v0.18，顶栏移除同步/齿轮按钮并改为 `+ 记录闪念`，搜索框改为图标容器，输入框新增右下工具栏，保存按钮与待办按钮按纸张主题精修，时间流卡片与底部导航重绘。APK 元数据提升到 `versionCode=18`、`versionName=0.18-paper-ui-polish`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-08：完成纸张主题阴影与标签修正 v0.19，移除主界面动态 elevation 阴影，顶部按钮改为“同步笔记”并恢复批量同步动作；待办/已同步标签改为无描边浅色胶囊并提升文字可读性。APK 元数据提升到 `versionCode=19`、`versionName=0.19-paper-shadow-badge-fix`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-08：完成纸张主题按钮细修 v0.20，顶部“同步笔记”按钮缩小为更秀气的尺寸，移除输入框右下工具栏图标，并清理 Button 默认 `stateListAnimator` 阴影。APK 元数据提升到 `versionCode=20`、`versionName=0.20-paper-button-cleanup`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-08：完成输入辅助按钮 v0.21，在闪念输入框下新增“上传图片”和“剪切板”两个按钮；上传图片会打开系统图片选择器并插入 Markdown 图片引用，剪切板会把最近一条剪切板文字填入输入框。APK 元数据提升到 `versionCode=21`、`versionName=0.21-input-assist-buttons`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-08：完成 UI 图标重绘 v0.22，将 `icon\小荳` 下 SVG 转为 Android vector drawable；上传图片/剪切板改回输入框内部右下角小图标，保存、保存为待办、同步笔记、底部导航、删除等入口替换为对应图标。APK 元数据提升到 `versionCode=22`、`versionName=0.22-icon-redraw-input-toolbar`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-08：修复 v0.22 启动崩溃，原因是 SVG 转 vector 时误把 `p-id` 捕获成 `pathData`，导致 `ic_input_image` inflate 失败；已重生成全部 `icon\小荳` vector 并增加非法 pathData 自查。v0.22 已重新安装到 vivo X200 Pro，adb 启动未再发现启动崩溃。
- 2026-06-08：完成纸张主题密度清理 v0.23，收窄“同步笔记”、保存、保存为待办按钮高度；保存文案改为“保存单条笔记”；去除状态文字前的小圆点和时间流右侧“全部”；底部导航去除图标，仅保留文字。APK 元数据提升到 `versionCode=23`、`versionName=0.23-paper-density-cleanup`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-08：完成底部导航图标修复 v0.24，按用户指定恢复底部四个图标：`1-首页.svg`、`2-历史.svg`、`3-统计.svg`、`4-设置.svg`，对应文案为“首页、历史、统计、设置”；重新生成安全 Android vector 并进行非法 `pathData` 自查。APK 元数据提升到 `versionCode=24`、`versionName=0.24-bottom-nav-icons`。已通过测试、构建签名验证、vivo X200 Pro 安装和 adb 启动。
- 2026-06-08：完成底部导航对齐修复 v0.25，将底部四个 Tab 从 `TextView` 顶部复合图标改为独立 `ImageView + TextView` 垂直结构，图标尺寸放大到 26dp，并统一居中和上下间距。APK 元数据提升到 `versionCode=25`、`versionName=0.25-bottom-nav-align`。已通过测试、构建签名验证、vivo X200 Pro 安装、adb 启动和真机截图复核。
- 2026-06-11：完成 Obsidian 写入格式优化 v0.26，同步到 Markdown 的每条笔记改为 `#### 闪念-yyyyMMddHHmm` 标题块，包含闪念/待办列表项、`记录日期::`、`备注::` 和 `^flash-yyyyMMdd-HHmm` 块引用；待办笔记保留 `- [ ]` 和 `#闪念 #待办`，普通笔记写为 `- 内容 #闪念`。APK 元数据提升到 `versionCode=26`、`versionName=0.26-obsidian-flash-blocks`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成 Obsidian 闪念标题格式微调 v0.27，同步写入标题从 `#### 闪念-yyyyMMddHHmm` 改为 `**闪念-yyyyMMddHHmm**`，其余列表项、`记录日期::`、`备注::` 和块引用格式保持不变。APK 元数据提升到 `versionCode=27`、`versionName=0.27-obsidian-bold-flash-title`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成 Obsidian 闪念标题前缀微调 v0.28，同步写入标题从 `**闪念-yyyyMMddHHmm**` 改为 `**大尾巴闪念-yyyyMMddHHmm**`，其余结构保持不变。APK 元数据提升到 `versionCode=28`、`versionName=0.28-dawei-flash-title`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成纸张主题按钮与顶部 Logo 优化 v0.29，保存/保存为待办按钮改为低存在感柔和色块，纸张主题使用奶绿色浅底和深绿文字图标，其他主题补充各自独立的低饱和动作色；顶部“大尾巴闪念”改为 `sans-serif-medium`、增加 0.08 字距，并按状态栏高度动态增加顶部安全区 padding。APK 元数据提升到 `versionCode=29`、`versionName=0.29-soft-actions-logo-safearea`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成 Obsidian 图片附件上传 v0.30，输入框上传图片按钮不再插入 `content://`，而是读取手机图片并通过 WebDAV 上传到目标笔记同级 vault 的 `assets` 目录（默认对应 `OBS/Damon/assets`），输入框插入 `![[assets/文件名|200]]`，确保 Obsidian 展示宽度固定为 200 像素。APK 元数据提升到 `versionCode=30`、`versionName=0.30-obsidian-image-assets`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成图片延迟上传 v0.31，输入框上传图片按钮只插入 `![待上传图片](content://...)` 占位，可继续输入文字并保存到本地数据库；点击同步笔记或重新同步单条时，才批量上传占位图片到 `OBS/Damon/assets` 并替换为 `![[assets/文件名|200]]` 后写入 Obsidian。APK 元数据提升到 `versionCode=31`、`versionName=0.31-deferred-image-upload`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：修复图片同步失败 v0.32，移除 Android `HttpURLConnection` 不支持的 WebDAV `MKCOL` 建目录请求，改为直接 `PUT` 上传图片到已存在的 `OBS/Damon/assets`；若目录不存在，返回明确提示。APK 元数据提升到 `versionCode=32`、`versionName=0.32-image-put-no-mkcol`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-11：完成设置页版本信息 v0.33，设置页面底部显示 APK `versionName/versionCode` 和构建日期；构建脚本每次打包前生成 `BuildInfo.java` 写入当前构建时间。APK 元数据提升到 `versionCode=33`、`versionName=0.33-settings-version-info`，本次构建日期为 `2026-06-11 11:56`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-12：修复同步目标 Markdown 404 v0.34，默认路径从旧的 `OBS\Damon\【MOC】随手记-Claw编辑版.md` 改为 `OBS\Damon\【MOC】闪念-随手记.md`，并在读取设置时自动迁移旧默认路径；已用 WebDAV 验证新路径 HTTP 200。APK 元数据提升到 `versionCode=34`、`versionName=0.34-sync-path-flash-note`。已通过测试和构建签名验证；安装时 adb 连接中断，尚未成功推送到手机。
- 2026-06-12：完成输入框下拉快捷动作 v0.35，在输入框区域下拉时页面整体下移并露出隐藏同步图标，松手超过阈值后震动一次；输入框有内容时保存为普通单条笔记，输入框为空时触发同步笔记。APK 元数据提升到 `versionCode=35`、`versionName=0.35-pull-input-save-sync`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-12：补充 APK 归档规则，`tools\build-apk.ps1` 在每次清理旧构建目录前会先把现有 APK 复制到 `DabaweiFlashNote\apk-archive\DabaweiFlashNote-v版本号-versionName.apk`，并在成功构建后更新 `apk-archive\last-build.json`；已验证归档出 `DabaweiFlashNote-v35-0.35-pull-input-save-sync.apk`。
- 2026-06-12：完成输入提示与下拉图标位置优化 v0.36，输入框提示改为“想到什么,马上写下，下拉保存，再拉同步”；下拉隐藏同步图标放大到 56dp，并下移到 64dp 顶距以避开灵动岛/状态栏。APK 元数据提升到 `versionCode=36`、`versionName=0.36-pull-hint-indicator`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-12：完成下拉图标完整露出与动作提示 v0.37，下拉隐藏指示改为“同步图标 + 提示语”横向组合；输入框有内容时提示“再拉就保存辣！”，输入框为空时提示“再拉就同步辣！”，并提高下拉最大距离保证整组提示完整露出。APK 元数据提升到 `versionCode=37`、`versionName=0.37-pull-prompt-indicator`；构建前已自动归档上一版 `DabaweiFlashNote-v36-0.36-pull-hint-indicator.apk`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-12：完成输入框双行浅色提示 v0.38，输入框 hint 改为两行“想到什么 马上写下 / 下拉保存 再拉同步”，并将输入框提示色调浅，降低视觉存在感。APK 元数据提升到 `versionCode=38`、`versionName=0.38-two-line-input-hint`；构建前已自动归档上一版 `DabaweiFlashNote-v37-0.37-pull-prompt-indicator.apk`。已通过测试和构建签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-12：完成下拉保存自动识别待办 v0.39，输入框下拉触发保存时会自动检测内容是否包含“待办”；包含则保存为待办笔记，不包含则保存为普通笔记，空输入框仍触发同步笔记。APK 元数据提升到 `versionCode=39`、`versionName=0.39-pull-auto-todo`；构建前已自动归档上一版 `DabaweiFlashNote-v38-0.38-two-line-input-hint.apk`。已通过资源测试、纯 Java 测试、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-13：完成 Claude 字体风格设置 v0.40，设置页新增“Claude 字体风格”开关，可在现有主题之上切换为更接近 Claude 阅读气质的系统 serif 字体；主界面、时间流卡片、快速输入页和设置页会读取该开关。APK 元数据提升到 `versionCode=40`、`versionName=0.40-claude-font-style`；构建前已自动归档上一版 `DabaweiFlashNote-v39-0.39-pull-auto-todo.apk`。已通过资源测试、纯 Java 测试、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-13：完成历史页和待办页入口骨架 v0.41，首页移除搜索框，底部“历史”页承接搜索栏和同款时间流列表，并支持历史页下拉同步；底部“统计”改为“待办”，待办页先显示“同步待办”入口和空状态，占位等待下一版接入 Obsidian 待办拉取。APK 元数据提升到 `versionCode=41`、`versionName=0.41-history-todo-skeleton`；构建前已自动归档上一版 `DabaweiFlashNote-v40-0.40-claude-font-style.apk`。已通过资源测试、纯 Java 测试、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-13：修复历史页翻阅冲突 v0.42，移除历史页搜索栏、标题和时间流列表上的下拉同步监听；下拉保存/同步仅保留在首页输入框区域，避免影响历史页正常滚动翻阅。APK 元数据提升到 `versionCode=42`、`versionName=0.42-history-scroll-fix`；构建前已自动归档上一版 `DabaweiFlashNote-v41-0.41-history-todo-skeleton.apk`。已通过资源测试、纯 Java 测试、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-13：完成待办单一同步文件读取 v0.43，使用 Obsidian CLI 创建 `【MOC】大尾巴待办同步.md`，文件内包含 Tasks 查询区和 `DABAWEI_TODO_SYNC_BEGIN/END` 机器可读区块；App 待办页“同步待办”按钮通过坚果云 WebDAV 读取 `OBS\Damon\【MOC】大尾巴待办同步.md`，解析并显示待办正文、来源文件、行号和备注，不扫描整个 vault、不回写原始任务。APK 元数据提升到 `versionCode=43`、`versionName=0.43-todo-sync-read`；构建前已自动归档上一版 `DabaweiFlashNote-v42-0.42-history-scroll-fix.apk`。已通过资源测试、纯 Java 测试、TodoSyncParser 测试、WebDAV GET 200 验证、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-13：完成 Obsidian 端待办同步插件 v1.0.0，安装到 `C:\OBS\Damon\.obsidian\plugins\dabawei-todo-sync` 并加入启用列表；插件命令为“刷新大尾巴待办同步文件”，会扫描 vault 中未完成且包含 `#待办` 的任务，按既定排除文件/目录规则写入 `【MOC】大尾巴待办同步.md` 的 `DABAWEI_TODO_SYNC_BEGIN/END` 区块。已通过 Node 纯函数测试、Obsidian 插件 reload、命令执行、`dev:errors` 无错误和坚果云 WebDAV GET 200 验证。
- 2026-06-17：优化 Obsidian 待办同步插件输出格式，同步区块内改为按来源笔记分组，使用 `### [[来源笔记]]` 标题分隔各笔记下的待办；任务行和 `来源文件::/行号::/块ID::/备注::` 元数据保持不变，手机端解析兼容。已通过 Node 纯函数测试、Obsidian 插件 reload、命令执行刷新和 `dev:errors` 无错误验证。
- 2026-06-17：完成手机端待办页分组与自动同步 v0.44，进入待办页时自动同步一次并移除占空间的“同步待办”按钮；待办列表按来源笔记分组显示，组标题展示笔记名/路径，组内卡片仅展示任务正文、行号和备注，减少重复信息。APK 元数据提升到 `versionCode=44`、`versionName=0.44-todo-grouped-auto-sync`；构建前已自动归档上一版 `DabaweiFlashNote-v43-0.43-todo-sync-read.apk`。已通过资源测试、纯 Java 测试、TodoSyncParser 测试、APK 构建和签名验证；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-17：完成系统分享入口 v0.45，`大尾巴闪念` 会出现在其他 App 的分享列表中；分享文字和链接会自动填入首页输入框，分享单张或多张图片会插入 `![待上传图片](content://...)` 占位，保存后继续沿用延迟上传到 Obsidian `assets` 的链路。APK 元数据提升到 `versionCode=45`、`versionName=0.45-share-target`；构建前已自动归档上一版 `DabaweiFlashNote-v44-0.44-todo-grouped-auto-sync.apk`。已通过资源测试、纯 Java 测试、APK 构建签名验证和 APK Manifest 反查；当前 adb 未发现设备，尚未推送到手机。
- 2026-06-17：修复分享图片同步权限丢失 v0.46，分享图片填入输入框时会先复制到 App 私有 `pending-images` 待上传目录，并以 `file://` 占位保存；同步图片时同时支持 `content://` 和 `file://` 占位，上传成功后自动删除本地待上传副本，避免 `has no access` 导致同步失败。APK 元数据提升到 `versionCode=46`、`versionName=0.46-share-image-cache`。
- 2026-06-17：补充旧分享图片权限兜底 v0.47，若待同步旧记录里仍包含 `content://media/...` 图片占位，会在同步前请求系统图片读取权限，授权后自动继续同步；若拒绝授权则提示重新分享或重新选择图片。APK 元数据提升到 `versionCode=47`、`versionName=0.47-legacy-media-permission`。同时调整版本备份策略：新增 `tools\backup-core-code.py`，构建前自动备份核心代码到 `90-版本代码备份\DabaweiFlashNote-版本号-日期.zip`，不再备份 APK。
- 2026-06-17：完成 Obsidian 待办同步插件 v1.1.0，插件不再使用硬编码排除范围，而是读取 `【MOC】大尾巴待办同步.md` 内第一个 `tasks` 查询块作为同步范围；支持 `not done`、`tags include`、`filename/path include/does not include`、`limit`、`sort by ...` 等当前查询条件，`#` 注释行不生效。手动执行“刷新大尾巴待办同步文件”命令或左侧栏按钮时，会刷新同步区块并打开该同步笔记；后台自动刷新不打扰当前页面。已通过 Node 查询解析测试、Obsidian 插件 reload、命令执行和 `dev:errors` 无错误验证。
