# 云端飞书提醒中继

该服务只接收手机在成功读取 WebDAV 后上报的完整提醒快照，不读取 WebDAV、不保存 WebDAV 凭据。服务使用 Python 标准库、SQLite 和 HTTPS，监听独立的 1291 端口，不修改现有 OpenClaw、Caddy 或 FRP 服务。

服务器部署时把 `dabawei-cloud-reminder.env.example` 复制为权限为 `0600` 的 `/etc/dabawei-cloud-reminder.env`，填入随机设备 Token 和飞书 Webhook；证书与私钥放在 `/etc/dabawei-cloud-reminder/`。公网只有 IP 时，手机端固定服务证书 SHA-256 指纹；取得域名后应切换为标准 CA 证书并更新 APK 默认地址。

接口：`GET /healthz` 为无认证健康检查；`POST /v1/reminders/reconcile` 使用 `Authorization: Bearer <token>`，接收 `device_id`、完整 `observed_task_ids` 和 `active_reminders`。相同 `(device_id, task_id)` 快照幂等，完成、删除或取消提醒会从 active 列表移除并取消云端发送。

日志只记录接口路径、设备 ID、任务 ID、尝试次数和通用错误，不记录 Token、Webhook 或任务正文。
