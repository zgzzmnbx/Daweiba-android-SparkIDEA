# 大尾巴闪念 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first Android MVP for fast local thought capture.

**Architecture:** Keep the existing single-Activity native Android project and manual SDK build. Add a small SQLite data layer plus a pure Java Markdown exporter that can be tested from PowerShell.

**Tech Stack:** Java, Android SDK 35, SQLiteOpenHelper, PowerShell test/build scripts.

---

## Files

- Create `DabaweiFlashNote/app/src/main/java/com/dabawei/flashnote/FlashNote.java` for note data.
- Create `DabaweiFlashNote/app/src/main/java/com/dabawei/flashnote/FlashNoteDatabase.java` for local persistence.
- Create `DabaweiFlashNote/app/src/main/java/com/dabawei/flashnote/MarkdownExporter.java` for Markdown formatting.
- Create `DabaweiFlashNote/app/src/test/java/com/dabawei/flashnote/MarkdownExporterTest.java` for pure Java tests.
- Create `DabaweiFlashNote/tools/test-markdown-exporter.ps1` for compiling and running pure Java tests.
- Modify `DabaweiFlashNote/app/src/main/java/com/dabawei/flashnote/MainActivity.java` for UI behavior.
- Modify `DabaweiFlashNote/app/src/main/res/layout/activity_main.xml` for the capture UI.
- Modify `DabaweiFlashNote/app/src/main/res/values/strings.xml`, `colors.xml`, and `styles.xml`.
- Modify `DabaweiFlashNote/tools/test-hello.ps1` into an app resource sanity test.
- Modify `DabaweiFlashNote/README.md` and root `README.md` with current state.

## Tasks

- [x] Add failing Markdown exporter test for date grouping and blank content filtering.
- [x] Implement `FlashNote` and `MarkdownExporter` until the test passes.
- [x] Add resource sanity test for App name and key strings.
- [x] Update strings, colors, and layout for the first MVP.
- [x] Implement SQLite persistence and MainActivity save/search/export behavior.
- [x] Run resource test, Markdown exporter test, APK build, and signature verification.
- [ ] Install and adb launch on vivo X200 Pro after device reconnects.
- [x] Update README handoff notes with changed files, validation, and version/log status.

## Self-Review

- Spec coverage: all five confirmed features have a corresponding implementation task.
- Placeholder scan: no TBD/TODO placeholders.
- Scope check: no cloud sync, AI, tagging, rich text, or account work included.
