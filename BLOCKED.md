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
