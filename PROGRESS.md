# v0.6.0-shadcn-rhea-ui 执行进度
- 基线：HEAD=436027b，包含 087402c；vivo V2405A 为 device；原有构建/截图脏文件保留不碰。
- 回退标签：backup-20260807-before-shadcn-rhea-ui -> 436027b。
- 目标：versionCode=52、versionName=0.6.0-shadcn-rhea-ui；原生 XML+Java 全界面统一 Rhea/Neutral/Blue。
- 顺序：设计契约与红灯测试 → 令牌/主题 → 通用资源 → 页面/弹层/组件 → 回归/真机/文档/提交。
- 任务0/1完成：基线锁定；UI 专项测试旧实现 exit=1，修复后 tokens/contrast/migration/touch/masking/XML/IDs exit=0。
- 任务2续修：双主题、旧偏好迁移、原有 14 个图标已按基线恢复；提醒/同步标签和顶部同步按钮已改为浅色语义组合；待办占位与设置安全区/固定底导已修复，加入苹方字体选项。
- 回归完成：五个既有脚本均 exit=0；故障注入 UI 对比度 exit=1，恢复后 exit=0；git diff --check exit=0。
- 构建/交付：v52/0.6.0-shadcn-rhea-ui，签名 v1/v2/v3 全通过；首轮提交 02ed227，反馈修订提交 24cbc19，均已推送 origin/master。
- 反馈修订验收：本地构建/静态回归通过；已复采浅色首页/历史/待办/设置上下屏 5 张并目视通过；逾期提醒标签已压缩为“已过期 · 查看待办”，设备随后再次断开，剩余真机截图待恢复连接。
