# 大尾巴闪念待办提醒功能开发提示词

把下面整段复制到一个新的 Codex 任务中使用。

```text
请继续开发以下本地 Android 项目：

D:\Codex-Temp\260606-android-dev

本次目标：实现“大尾巴闪念”手机端待办提醒 P0，不做 P1/P2 扩展。

开始前必须完整阅读并遵守：

1. D:\Codex-Temp\260606-android-dev\AGENTS.md
2. D:\Codex-Temp\260606-android-dev\README.md
3. D:\Codex-Temp\260606-android-dev\DabaweiFlashNote\README.md
4. D:\Codex-Temp\260606-android-dev\DabaweiFlashNote\PRD.md，重点阅读 7.5“手机端待办提醒”

当前工程事实：

- 原生 Android XML + Java。
- 使用 Android SDK 手工构建，minSdk 23、targetSdk 35。
- 当前包名 com.dabawei.flashnote。
- 当前 APK 基线 versionCode=47、versionName=0.47-legacy-media-permission。
- App 当前从 OBS\Damon\【MOC】大尾巴待办同步.md 的 DABAWEI_TODO_SYNC_BEGIN/END 区块单向读取待办。
- 当前待办字段只有正文、完成状态、来源路径、行号、块 ID、备注，还没有提醒字段。
- Obsidian 插件位于 C:\OBS\Damon\.obsidian\plugins\dabawei-todo-sync。
- 不要扫描整个 vault，不要引入常驻服务器，不要把手机端改造成完整任务管理系统。

实施前先检查真实源码、Manifest、数据库结构、同步解析器、Obsidian 插件和同步文件，不能仅按提示词猜测。检查当前目录是否有用户未提交或未备份的改动；本项目未启用 Git 时，修改核心代码前先运行现有核心代码备份脚本，构建前仍保留自动备份。

必须实现的 P0 范围：

1. 提醒协议
   - Obsidian 原始任务支持子字段：提醒时间:: YYYY-MM-DD HH:mm。
   - 同步区块新增任务ID::、截止日期::、提醒时间::。
   - 解析器兼容旧同步区块，缺字段的旧待办仍可展示。
   - 任务 ID 能用于同步后识别新增、更新、完成、删除和移出同步范围的任务。

2. 本地提醒数据
   - 新增独立 reminders SQLite 表，不把远端待办强行塞入现有 notes 表。
   - 至少保存 task_id、正文、来源、截止时间、提醒时间、稍后时间、状态、notification_id、last_synced_at。
   - 同步采用对账机制：新增即调度、时间变化即更新、完成/删除/移出范围即取消。

3. 手机端交互
   - 保留现有“保存为待办”的快速流程。
   - 保存成功后提供“添加提醒”。
   - 提供 1 小时后、今天 18:00、明天 09:00、自定义日期时间、不提醒/取消提醒。
   - 待办卡片展示提醒时间和铃铛状态，并能修改或取消。
   - 首版不实现自然语言时间识别。

4. Android 调度与通知
   - 使用系统 AlarmManager、BroadcastReceiver、NotificationManager 和 SQLite，优先保持当前无大型依赖的构建方式。
   - 创建“待办提醒”通知通道。
   - Android 13 及以上正确申请 POST_NOTIFICATIONS。
   - Android 12 及以上按 PRD处理精确提醒特殊权限；请求前解释用途，未授权时降级为非精确提醒并显示可能延迟。
   - 通知正文展示任务、提醒时间和来源。
   - 提供“稍后10分钟”“稍后1小时”“查看待办”。
   - 首版不提供“完成”按钮，不回写 Obsidian 完成状态。
   - App 退出、锁屏或暂时断网后，已保存到手机的提醒仍可触发。
   - 增加 BOOT_COMPLETED 恢复；处理系统时间、时区和精确提醒权限变化后的重新调度。
   - 过期历史提醒不得逐条补发，使用合并提示或仅在待办页标记。

5. Obsidian 插件
   - 在不破坏当前 Tasks 查询范围规则、分组输出和旧手机解析兼容性的前提下，输出提醒字段。
   - 不要无提示地批量改写原始笔记。
   - 修改后运行插件现有 Node 测试；如协议新增逻辑，补充测试。

明确不做：

- 每日待办概览。
- 后台定期 WebDAV 同步。
- 自然语言识别。
- 重复提醒和强提醒。
- 通知中完成任务。
- 服务器、FCM 或实时推送。
- React Native、Flutter、Compose 迁移。

测试要求：

- 为提醒协议解析、旧协议兼容、任务对账、时间计算、通知 ID 和稍后提醒补充可重复的自动测试。
- 运行项目现有全部纯 Java 测试、资源检查、APK 构建和签名验证。
- 检查最终 APK Manifest 中通知、精确提醒、开机恢复相关权限和组件。
- vivo X200 Pro 在线时，实际安装 APK 并验证启动。
- 真机至少验证：普通触发、锁屏触发、App 被划掉后触发、稍后提醒、取消提醒、任务时间修改、任务删除、通知权限拒绝、精确提醒权限拒绝、重启恢复、时区变化、Doze 场景。
- 如设备不在线或某项系统测试无法完成，明确列出未验证项，不得声称已完成。

版本与文档：

- 根据当前基线合理提升 versionCode/versionName，建议从 versionCode=48 开始，versionName 可使用 0.48-todo-reminders；实施前以真实文件为准。
- 构建新的 APK 前按项目规则备份核心代码。
- 实现并验证后，将 PRD 7.5 中实际完成的 P0 状态更新为“已实现”，未完成项保留准确状态。
- 同步更新 DabaweiFlashNote\README.md；如项目已有 CHANGELOG，则更新 CHANGELOG，不要捏造不存在的历史。
- 遇到会反复影响项目的 Android/vivo 提醒权限、后台限制或构建坑点，简短写入项目 AGENTS.md 的“项目经验 / 注意事项”。

完成后按以下顺序汇报：

1. 实际实现了什么。
2. 自动测试、构建、签名和真机验证结果。
3. APK 路径、versionCode、versionName。
4. PRD、README、AGENTS、CHANGELOG 的更新情况。
5. 未完成、受系统限制或需要人工确认的问题。

不要只给方案；在确认项目现状后直接完成 P0 实现、测试、构建和可行的真机安装验证。
```
