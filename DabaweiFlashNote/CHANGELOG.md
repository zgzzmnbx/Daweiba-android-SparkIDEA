# CHANGELOG

## 0.51-feishu-reminder-push（2026-08-07）

- 待办提醒触发后，新增异步飞书机器人文本推送。
- 设置页新增飞书推送开关和 HTTPS webhook 配置，首次构建默认开启。
- 飞书网络请求失败不影响手机本地提醒；默认 webhook 只从工作区外的本地构建输入注入 APK，不写入 Git。
- 新增 `tools/test-feishu.ps1`，完成 payload 转义、HTTPS 校验、资源和提醒触发链路静态检查。
- 纯 Java 提醒回归测试、资源检查、APK 构建和 v1/v2/v3 签名校验通过；已安装到 vivo V2405/V2405A，设置页默认配置加载通过。

## 0.50-auto-natural-reminders（2026-08-06）

- 完成待办自然语言自动提醒、稳定基准、防漂移、防复活、撤销/修改反馈和提醒诊断。
- 完成 APK 构建、签名和 vivo V2405/V2405A 上的自然语言提醒与撤销验证。
