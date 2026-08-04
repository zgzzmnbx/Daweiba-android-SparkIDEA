# 大尾巴闪念设计文档

## 目标

第一版做一个 Android 本地闪念捕捉 App。核心体验是打开即写、一键保存、时间流回看、搜索、Markdown 导出。它不是完整笔记系统，而是低摩擦想法捕捉器。

## 范围

本版本包含：

- 启动 App 后输入框自动聚焦并弹出软键盘。
- 输入文字后点击保存，立即清空输入框并把记录插入时间流顶部。
- 使用本地 SQLite 保存闪念，关闭重开后记录仍保留。
- 按关键词搜索历史闪念，搜索结果即时刷新。
- 将全部闪念导出为 Markdown 文件，按日期分组。

本版本不包含：

- 账号、云同步、标签、富文本、语音、AI 自动整理、复杂分类。

## 架构

继续基于 `DabaweiFlashNote` 的手工 Android SDK 构建链路，不引入 Gradle 依赖。应用仍使用单 `Activity`，数据层由 `SQLiteOpenHelper` 封装，Markdown 格式化逻辑放在纯 Java 类中，便于命令行单元测试。

主要组件：

- `MainActivity`：负责界面、输入、保存、搜索、导出动作。
- `FlashNoteDatabase`：负责 SQLite 建表、插入、倒序查询、搜索。
- `FlashNote`：闪念数据对象。
- `MarkdownExporter`：把闪念列表格式化为 Markdown 文本。

## 数据模型

SQLite 表 `flash_notes`：

- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `content TEXT NOT NULL`
- `created_at INTEGER NOT NULL`

时间使用毫秒时间戳保存。界面显示为 `yyyy-MM-dd HH:mm`，导出按 `yyyy-MM-dd` 分组。

## 界面

首屏使用纵向布局：

- 顶部标题“大尾巴闪念”和导出按钮。
- 搜索输入框。
- 大号闪念输入框。
- 保存按钮。
- 历史时间流列表。

保存空白内容时不写入数据库，并给出轻量提示。

## 导出

导出文件写入应用外部文件目录下的 `exports` 文件夹，文件名格式为 `大尾巴闪念-YYYYMMDD-HHmmss.md`。导出成功后用 Toast 提示完整路径。

Markdown 格式：

```markdown
# 大尾巴闪念导出

## 2026-06-06

- 09:32 第一条想法
- 10:15 第二条想法
```

## 验证

- 纯 Java 测试验证 Markdown 分组和空内容忽略。
- 资源测试验证 App 名称和关键界面文案。
- 构建脚本验证 APK 能编译、签名。
- 真机安装后用 `adb shell am start` 启动。
