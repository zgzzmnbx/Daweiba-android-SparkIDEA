# 自然语言自动提醒开发任务书

```text
你是执行者，本任务书是唯一任务来源；工作区是 D:\Codex-Temp\260606-android-dev，App 根目录是其下 DabaweiFlashNote。中途没人可问，拿不准的写 BLOCKED.md，继续做不受影响的部分。断线或换会话先读 PROGRESS.md，每完成一项立即更新。目标是让手机和 Obsidian 待办中的自然语言时间自动变成稳定、不漂移、可撤销的本地提醒。取舍顺序：时间算对 > 不误提醒 > 自动化覆盖 > 开发速度。“只允许/不许”是硬约束，“建议”可调整但要在 PROGRESS.md 说明。

## 我替领导拍的板
- 自动范围→仅扫描 App 中保存为待办的闪念，以及同步到手机的 Obsidian 未完成待办；不扫描普通闪念｜这是领导最终确认范围。
- 无具体几点→目标日期当地 08:00；相对小时/分钟按当前时刻顺延｜这是领导明确规则。
- Obsidian 相对时间基准→优先可靠创建时间，否则首次成功同步时间（猜的）｜每次按同步时间重算会永久漂移。
- 多个冲突时间→不猜，显示待选择（猜的）｜错提醒比少提醒代价高。
- 版本→versionCode 50、versionName 0.50-auto-natural-reminders（猜的）｜若真实基线不为49则顺延。

## 界限
只允许修改 DabaweiFlashNote 下提醒相关 Java/资源/测试/构建版本/README/PRD，以及根 PROGRESS.md、BLOCKED.md。C:\OBS\Damon\.obsidian\plugins\dabawei-todo-sync 默认只读；只有证明现有插件无法传递正文、任务ID、截止日期或显式提醒时间时才能改 main.js/test.js/README.md并记录证据。不许改包名、WebDAV目标、同步锚点、普通闪念行为、通知样式、P2功能或引入服务器/大型依赖。不许删除、跳过、放宽现有测试，不许 mock 被测解析器，不许用 || true。开发前运行现有核心备份；Git 工作区不干净时只提交本任务文件，禁止夹带用户改动。

## 现状与任务0
2026-08-06实测：versionCode 49 / 0.49-p1-todo-reminders；数据库v7；解析器仅支持阿拉伯数字“分钟/小时后”和“今天/明天/后天+几点”，保存后需确认；“两小时后”测试明确不支持；Obsidian仅传显式提醒字段；test-reminder-p1与插件Node测试全绿；adb无设备。先复核 AGENTS.md、两级README、PRD 7.5、NaturalLanguageReminderParser、MainActivity、TodoSyncCoordinator、ReminderReconciliation、FlashNoteDatabase及真实git状态；对不上就写BLOCKED.md并停受影响项。核对后在PROGRESS.md用≤10行写目标、顺序、最大风险。

## 任务1：统一时间解析
实现：30分钟后/半小时后/一个或两小时之后、明天/后天/大后天、N天后、N天后几点、YYYY-MM-DD、M月D日；阿拉伯数及一至九十九中文数；后/之后/以后。日期无时分=08:00，相对分钟小时不用08:00。排除创建日期::、记录日期::等元数据；无数字“几天后”、过去时间、多个冲突时间不自动调度。解析结果返回来源表达、基准、是否唯一。补固定时区和固定now的表驱动测试，至少覆盖PRD 7.5.4全部正反例。

## 任务2：自动调度与防复活
本地保存为待办且结果唯一时直接调度，不再确认，原文不变，并显示“已自动设置+撤销/修改”。同步Obsidian时优先级严格为 #不提醒 > 显式提醒时间 > 正文自然语言 > 截止日08:00。数据库升级必须保留旧数据，记录 reminder_source/source_expression/source_signature/natural_reference_at/auto_suppressed。相同taskId+相同表达连续同步不得漂移或重复；用户取消后不得复活；表达变化要取消旧闹钟再建新闹钟。普通闪念零变化。

## 任务3：验证、版本、交付
运行：powershell -ExecutionPolicy Bypass -File .\tools\test-reminder-p0.ps1；powershell -ExecutionPolicy Bypass -File .\tools\test-reminder-p1.ps1；powershell -ExecutionPolicy Bypass -File .\tools\test-hello.ps1；powershell -ExecutionPolicy Bypass -File .\tools\test-markdown-exporter.ps1；powershell -ExecutionPolicy Bypass -File .\tools\build-apk.ps1。新增测试必须先故意破坏一个默认08:00或防漂移断言，贴红灯，恢复后贴绿灯。若改插件，再运行 node test.js。设备在线则安装并测2分钟后、1小时后、三天后默认08:00、Obsidian同步、取消不复活；离线必须明确未做真机验证。更新版本、README和PRD实际状态；重复坑写AGENTS.md。

## 规矩
同一验收连败3次换下一项；结果劣于基线就回滚并如实报告。测试数量只增不减、跳过为0。不要把“编译通过”冒充提醒时间正确，也不要把首次同步成功冒充连续同步不漂移。

## 完成条件
1. PRD列出的正例时间全部自动得到唯一正确时间，反例零自动提醒；同一Obsidian任务连续同步3次调度数不增、时间不变，取消后不复活。
2. 全部既有与新增测试、APK构建签名通过，普通闪念/显式提醒/手动提醒无回归；每条在对话贴实际命令输出，红→绿证据也要贴，只说完成不算。提交BLOCKED.md，空也写“无”；最多执行12轮，满轮即停并如实汇报。
```
