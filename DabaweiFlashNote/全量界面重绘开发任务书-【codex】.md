你是执行者，本任务书是唯一任务来源；中途没人可问，拿不准的写入根目录 `BLOCKED.md`，跳过后继续可独立完成的工作，最后一并提交。
断线或换会话时先读 `PROGRESS.md` 接着做，每完成一项立即更新，禁止重做已完成工作。
这次要在不损伤任何记录、同步、提醒和推送能力的前提下，把大尾巴闪念的全部原生 Android 界面统一成简洁、现代、紧凑的 shadcn Rhea 产品界面。
冲突时按“数据和业务不退化 > 浅/深主题完整 > 全界面一致 > 视觉精修 > 开发速度”让步。
“只允许 / 不许”是硬约束；“建议”可用更好的办法替代，但须在 `PROGRESS.md` 记明原因。

## 我替领导拍的板

- 原生适配：按 `PRD.md` 7.6 实现 preset `b1au7YYAi` 的 Rhea/Neutral/Blue 语言，不引入 React、Tailwind 或 shadcn Web 运行时；猜错会破坏现有技术链。
- 主题：只有浅色、深色两套视觉主题，偏好可选跟随系统/浅色/深色，默认跟随系统；它不是三套主题。
- 版本：交付目标为 52 / `0.6.0-shadcn-rhea-ui`；开工基线仍是 51 / `0.51-feishu-reminder-push`。
- 全界面：包含首页、历史、待办、设置、快速捕捉、桌面组件、记录/提醒/编辑/删除等弹窗，以及加载、空、错、禁用、焦点、下拉反馈；漏一类就不算全量。
- 视觉验收只能半自动：自动检查令牌/对比度/触控尺寸，最终以 vivo X200 Pro 浅深两套真机截图抽查为准。

## 界限

- 只允许修改 `app/src/main/res/**`；`MainActivity.java`、`SyncSettingsActivity.java`、`QuickCaptureActivity.java`、`ThemePalette.java`、`FlashNoteWidgetProvider.java`；新建仅限同包下主题/纯 UI helper；`ThemePaletteTest.java`；新建 UI 专项测试及 `tools/test-ui-redesign.ps1`；版本所需的 `tools/build-apk.ps1`；`PRD.md`、`README.md`、`CHANGELOG.md`、`PROGRESS.md`、`BLOCKED.md`；验收截图放 `../Codex-Temp/ui-acceptance/`。其余路径只读。
- 不许改 Manifest、数据库、同步/提醒/飞书实现、包名、协议、现有 5 个测试脚本和非主题业务测试；`ThemePaletteTest` 可按新主题规格增强，但断言数不得减少。
- 不许提交完整 webhook、账号密码、APK、`build/`、历史备份或开工前已有的截图/构建脏文件；不得删除或覆盖这些用户文件。
- 顺手修业务 bug、迁移 Compose/Flutter/React Native、加大型依赖、改权限、改数据库结构，一律记 `BLOCKED.md`，不得实施。

## 现状与任务 0

2026-08-07 实测：`master` 与 `origin/master` 同为含本任务书的 `087402c`，其父提交 `34a9c8d` 是 v51 代码基线；原生 XML+Java、minSdk 23/targetSdk 35；5 个布局、3 个 Activity、4 个底部导航入口、8 个旧主题；vivo V2405A 在线，1260×2800、560dpi，已安装 v51。工作树另有源码、脚本、build 产物与截图变化，不属于本任务，不得覆盖或纳入提交。

先运行 `git status --short --branch`、`git log -1 --oneline --decorate`、`adb devices -l` 核对；不一致就把证据写在 `BLOCKED.md` 顶部，仅做不受影响部分。一致后在当前 HEAD 建唯一可回退标签 `backup-20260807-before-shadcn-rhea-ui`，再把目标/顺序/最大风险写入 `PROGRESS.md`，不超过 10 行。

## 任务 1：先锁住设计契约

从 `PRD.md` 7.6 建立单一语义令牌、组件变体和旧主题迁移表。先新增 UI 专项测试，让它在旧实现上确实失败并贴红色输出；至少检查两套令牌齐全、关键对比度≥4.5:1、8 个旧键映射、48dp 触控目标、敏感字段遮罩、XML 可解析、主要 View ID 仍存在。不得为了变绿放松规格。

## 任务 2：完成全部原生界面重绘

按“令牌与主题状态→通用 Drawable/样式/图标→首页/历史/待办→设置→快速捕捉→弹窗/空态/桌面组件”顺序做，避免页面各自长出一套样式。颜色只来自语义令牌；图标统一 Lucide 线性语言，不用 emoji 代替操作；常驻卡片无重阴影；可点击目标≥48dp。旧主题偏好平滑迁移，其他 SharedPreferences、SQLite 和敏感配置原样保留。主题切换应即时、持久、跟随系统且不闪白；键盘、安全区、1.3倍字体和 TalkBack 不遮挡核心操作。

## 任务 3：回归、真机和发布

依次执行下列已实测可用的回归命令，五条都必须 exit 0：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test-hello.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test-markdown-exporter.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test-reminder-p0.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test-reminder-p1.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\test-feishu.ps1
```

再运行新增 UI 测试，并故意临时破坏一个对比度令牌证明它会红，立即还原后贴全绿。按 `README.md` 的构建、签名、安装步骤执行；真机包信息必须是 52 / `0.6.0-shadcn-rhea-ui`。浅/深各截 8 类图：首页、历史、待办、设置顶部、设置底部、快速捕捉、代表性弹窗、桌面组件；截图不得出现完整秘密。同步更新 README/PRD/CHANGELOG，检查 `git diff --check` 和白名单，提交后非强制推送 `origin master`。

## 规矩

禁止 skip/todo、删测试、减少断言、放宽阈值、mock 被测业务、改现有验收脚本、`|| true` 或把截图当实现；出现任一项即失败。五个现有脚本保持全绿且 skipped=0。相同验收连败 3 次就换下一项并记录；结果比基线差就回退该项并如实报告。只允许非强制 Git 操作，标签冲突时不得覆盖。

## 完成条件

1. 两套主题下 8 类界面共 16 张真机截图全部无旧样式孤岛、截断、重叠、低对比度、键盘遮挡或敏感信息，主题三种偏好均正确落到两套视觉主题。
2. 现有五条回归、新 UI 测试、构建签名和真机版本核验全绿，业务文件越界修改为 0，Git 已提交并非强制推送。

每条完成条件都要在对话贴实际命令输出，含 UI 检查红→绿证据；只说完成不算。`BLOCKED.md` 随交付提交，空也写“无”。最多 3 轮完整修复；满轮即停，如实写清卡点和剩余项。
