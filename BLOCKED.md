代码、纯 Java、资源、构建和签名无阻塞。
真机验证已部分完成：2026-08-06 v0.50 已安装并启动于 vivo V2405/V2405A，已验证自然语言自动提醒、撤销及重启不复活；随后手机在通话状态下断开 ADB。
待设备重新连接后补测：2 分钟到点通知、1 小时、三天默认 08:00、Obsidian 同步、锁屏/息屏、重启、时区变化、Doze 和后台 JobScheduler。
## 2026-08-07 v0.6.0-shadcn-rhea-ui

- 真机安装阻塞：vivo V2405A（`10AEAZ35S30014C`）在线且状态为 `device`，`tools/install-apk.ps1` 与 `adb install --no-incremental -r` 均 exit=1，系统返回 `INSTALL_FAILED_ABORTED: User rejected permissions`。
- 该阻塞只影响 v52 真机安装、浅/深主题截图和现场包信息核验；未修改 Manifest、权限、数据、同步、提醒或推送实现，继续完成纯 Java、资源、静态契约和五项回归。
- 收尾复核：`adb start-server` exit=0，但随后 `adb devices -l` 仍无设备；因此未生成或伪造 16 张验收截图，设备恢复连接后才可继续现场验收。
- 后续解除：设备重新连接后在系统安装器勾选“已了解应用的风险检测结果”并点击“继续安装”，`adb install --no-incremental -r` exit=0，包信息为 v52 / `0.6.0-shadcn-rhea-ui`；进入截图验收。
- 反馈修订后的再次阻塞：新包构建完成后重新安装返回 `adb.exe: no devices/emulators found`；`adb start-server`、`adb reconnect` 和 30 秒轮询均未发现设备，浅/深截图暂缓。
- 反馈修订阻塞解除：设备重新连接后系统风险确认完成，修订包安装成功并核验为 v52 / `0.6.0-shadcn-rhea-ui`，进入真机截图。
- 2026-08-07 布局/苹方修订后再次安装：ADB 仍显示 vivo V2405A 为 `device`，但系统 `mInputRestricted=true`、屏幕处于锁屏；安装返回 `INSTALL_FAILED_ABORTED: User rejected permissions`。未绕过锁屏或修改权限，待设备解锁后继续安装与真机复验。
- 2026-08-07 设备解锁后已成功安装修订包并完成浅色首页、历史、待办、设置顶部/底部 5 张截图；从桌面继续组件/快速捕捉采集时 USB ADB 再次断开，连续两轮 30 秒重连均无设备，未生成或伪造剩余截图。
- 苹方字体文件来自用户指定的本地字体包，随 APK 打包的再分发授权未在当前工作区确认；代码按用户要求接入，正式对外分发前需确认字体授权。
- 2026-08-07 逾期标签短文案修订包已构建并核验 v52；设备虽在线为 `device`，但 `adb install -r`、`adb install --no-incremental -r` 和 `adb shell pm install -r -g` 三次均返回 `INSTALL_FAILED_ABORTED: User rejected permissions`，因此本轮短文案未完成现场截图核验，未改动应用数据。
- 2026-08-07 16:13 现场排查下午 3 点待办：设备时间为 16:13；应用设置诊断显示“通知权限已允许、精确提醒已开启、飞书推送已开启、已调度提醒 0 条”。系统通知诊断同时显示应用级 `moreNotificationsEnabled=false`，AlarmManager 仅见历史 `REMINDER_FIRE` 唤醒、未见当前待触发的应用闹钟。由此确认本地通知被 vivo 应用级通知开关拦截；15:00 任务当前也没有排程，尚缺业务排程/触发证据。`ReminderReceiver` 对飞书请求的返回值未记录，因此无法从现有日志证明 HTTP 成功或失败。按本任务边界不修改提醒、同步、数据库或飞书实现，待单独授权业务修复并开启系统通知后复测。
- 2026-08-07 16:45–16:48 真机复测：保留数据安装 v52 成功；新建的临时 `CodexReminderTest` 在设置为 16:45 后，AlarmManager 的 `REMINDER_FIRE` 唤醒次数由 3 增至 5，`dumpsys notification --noredact` 出现 `todo_reminders` 活跃通知，标题与正文均匹配测试待办，证明本地提醒链路通过。设置诊断为“通知权限：已允许、精确提醒：已开启、飞书推送：已开启”；此前仅凭 `moreNotificationsEnabled=false` 推断通知被拦截不再作为结论。飞书验收请求在不输出 Webhook 的前提下返回 HTTP 200、业务 code=0；但 `ReminderReceiver` 调用 `FeishuWebhookClient.sendReminderCard` 时丢弃 `Result`，因此无法从应用日志单独证明该次异步调用的响应。临时测试待办及其测试通知已清除，原有记录、同步状态和设置未改动。
