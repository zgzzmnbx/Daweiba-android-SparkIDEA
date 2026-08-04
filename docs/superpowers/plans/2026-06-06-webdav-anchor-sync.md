# WebDAV Anchor Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Nutstore WebDAV sync that inserts flash notes below a configured Markdown anchor.

**Architecture:** Keep SQLite as the source of truth. Add a pure Java Markdown anchor inserter, a SharedPreferences-backed sync settings screen, and an Android WebDAV client that downloads the configured Markdown file, inserts a formatted note below the anchor, and uploads the whole file with conflict protection.

**Tech Stack:** Java, Android SDK 35, HttpURLConnection, SharedPreferences, SQLite.

---

## Tasks

- [ ] Add failing pure Java tests for inserting a note below `<!-- DABAWEI_FLASHNOTE_INBOX -->`.
- [ ] Implement `MarkdownAnchorInserter`.
- [ ] Add WebDAV sync settings strings/layout/activity.
- [ ] Add `WebDavMarkdownSync` and `SyncSettings`.
- [ ] Add sync status columns to SQLite and mark notes pending/synced/failed.
- [ ] Trigger sync after main save and quick capture save.
- [ ] Bump APK version to v0.4-webdav and verify tests/build/install.
- [x] Convert WebDAV sync to manual batch sync from the settings screen.
