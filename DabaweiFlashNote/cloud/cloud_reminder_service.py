#!/usr/bin/env python3
"""HTTPS reminder relay for Dabawei FlashNote.

The phone remains the WebDAV reader and sends a complete reminder snapshot.
This service never reads WebDAV and never logs tokens, webhooks, or task text.
"""

from __future__ import annotations

import hmac
import json
import logging
import os
import sqlite3
import ssl
import threading
import time
import urllib.error
import urllib.request
from contextlib import contextmanager
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any, Dict, Iterable, Iterator, List, Optional, Set, Tuple
from urllib.parse import urlparse
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError


SERVICE_NAME = "dabawei-cloud-reminder"
DEFAULT_HOST = "0.0.0.0"
DEFAULT_PORT = 1291
MAX_BODY_BYTES = 256 * 1024
MAX_DEVICE_ID_LENGTH = 128
MAX_TASK_ID_LENGTH = 256
MAX_TASK_TEXT_LENGTH = 4000
MAX_SOURCE_PATH_LENGTH = 2048
MAX_TIME_ZONE_LENGTH = 128
MAX_OBSERVED_TASKS = 4000
MAX_ACTIVE_REMINDERS = 2000
POLL_SECONDS = 2
SEND_TIMEOUT_SECONDS = 12
MAX_ATTEMPTS = 8
REMINDER_SOURCE = "大尾巴闪念.手机端"

LOGGER = logging.getLogger(SERVICE_NAME)


def now_ms() -> int:
    return int(time.time() * 1000)


def required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"missing required environment: {name}")
    return value


def validate_https_url(value: str, label: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme.lower() != "https" or not parsed.netloc:
        raise RuntimeError(f"{label} must be an HTTPS URL")
    return value


def get_timezone(name: str) -> Any:
    try:
        return ZoneInfo(name or "UTC")
    except ZoneInfoNotFoundError:
        return ZoneInfo("UTC")


def format_reminder_time(epoch_ms: int, time_zone: str) -> str:
    value = datetime.fromtimestamp(epoch_ms / 1000, tz=get_timezone(time_zone))
    return value.strftime("%Y-%m-%d %H:%M")


def build_feishu_payload(task_text: str, reminder_time: str) -> bytes:
    body = (
        "📝 **待办内容**\n"
        + (task_text.strip() or "未填写待办内容")
        + "\n\n⏰ **提醒时间**\n"
        + (reminder_time.strip() or "未记录")
        + "\n\n📱 **来源**\n"
        + REMINDER_SOURCE
    )
    payload = {
        "msg_type": "interactive",
        "card": {
            "schema": "2.0",
            "config": {"update_multi": True},
            "body": {
                "direction": "vertical",
                "padding": "12px 12px 12px 12px",
                "elements": [
                    {
                        "tag": "markdown",
                        "content": body,
                        "text_align": "left",
                        "text_size": "normal_v2",
                        "margin": "0px 0px 0px 0px",
                    }
                ],
            },
            "header": {
                "title": {"tag": "plain_text", "content": "大尾巴闪念"},
                "subtitle": {"tag": "plain_text", "content": "手机端 · 待办提醒"},
                "template": "blue",
                "padding": "12px 12px 12px 12px",
            },
        },
    }
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def send_feishu(webhook: str, task_text: str, reminder_time: str) -> Tuple[bool, str]:
    request = urllib.request.Request(
        webhook,
        data=build_feishu_payload(task_text, reminder_time),
        headers={"Content-Type": "application/json; charset=UTF-8"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=SEND_TIMEOUT_SECONDS) as response:
            status = response.status
            response_body = response.read(8192).decode("utf-8", errors="replace")
    except urllib.error.HTTPError as error:
        try:
            error.read(8192)
        except Exception:
            pass
        return False, f"HTTP {error.code}"
    except Exception:
        return False, "network error"

    if status < 200 or status >= 300:
        return False, f"HTTP {status}"
    if (
        not response_body.strip()
        or '"code":0' in response_body
        or '"code": 0' in response_body
        or '"StatusCode":0' in response_body
        or '"StatusCode": 0' in response_body
    ):
        return True, "sent"
    return False, "webhook rejected"


class ReminderStore:
    def __init__(self, path: str) -> None:
        self.path = path
        parent = os.path.dirname(path)
        if parent:
            os.makedirs(parent, exist_ok=True)
        with self.connect() as connection:
            connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS reminders (
                    device_id TEXT NOT NULL,
                    task_id TEXT NOT NULL,
                    task_text TEXT NOT NULL,
                    source_path TEXT NOT NULL,
                    remind_at_ms INTEGER NOT NULL,
                    time_zone TEXT NOT NULL,
                    status TEXT NOT NULL,
                    sent_at_ms INTEGER,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    next_attempt_at_ms INTEGER NOT NULL DEFAULT 0,
                    last_error TEXT NOT NULL DEFAULT '',
                    sending_started_at_ms INTEGER NOT NULL DEFAULT 0,
                    updated_at_ms INTEGER NOT NULL,
                    PRIMARY KEY (device_id, task_id)
                );
                CREATE INDEX IF NOT EXISTS reminders_due_idx
                    ON reminders(status, next_attempt_at_ms, remind_at_ms);
                """
            )

    @contextmanager
    def connect(self) -> Iterator[sqlite3.Connection]:
        connection = sqlite3.connect(self.path, timeout=5)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA journal_mode=WAL")
        connection.execute("PRAGMA synchronous=NORMAL")
        try:
            yield connection
        finally:
            connection.close()

    def reconcile(
        self,
        device_id: str,
        observed_task_ids: Set[str],
        active_reminders: Iterable[Dict[str, Any]],
    ) -> Tuple[int, int]:
        timestamp = now_ms()
        accepted = 0
        active_ids: Set[str] = set()
        with self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            for reminder in active_reminders:
                task_id = reminder["task_id"]
                active_ids.add(task_id)
                existing = connection.execute(
                    "SELECT task_text, source_path, remind_at_ms, time_zone, status "
                    "FROM reminders WHERE device_id=? AND task_id=?",
                    (device_id, task_id),
                ).fetchone()
                changed = (
                    existing is None
                    or existing["task_text"] != reminder["text"]
                    or existing["source_path"] != reminder["source_path"]
                    or int(existing["remind_at_ms"]) != reminder["remind_at_epoch_ms"]
                    or existing["time_zone"] != reminder["time_zone"]
                    or existing["status"] in ("cancelled", "failed")
                )
                if existing is None:
                    connection.execute(
                        "INSERT INTO reminders (device_id, task_id, task_text, source_path, "
                        "remind_at_ms, time_zone, status, updated_at_ms) "
                        "VALUES (?, ?, ?, ?, ?, ?, 'scheduled', ?)",
                        (
                            device_id,
                            task_id,
                            reminder["text"],
                            reminder["source_path"],
                            reminder["remind_at_epoch_ms"],
                            reminder["time_zone"],
                            timestamp,
                        ),
                    )
                elif changed:
                    connection.execute(
                        "UPDATE reminders SET task_text=?, source_path=?, remind_at_ms=?, "
                        "time_zone=?, status='scheduled', sent_at_ms=NULL, attempts=0, "
                        "next_attempt_at_ms=0, last_error='', sending_started_at_ms=0, "
                        "updated_at_ms=? WHERE device_id=? AND task_id=?",
                        (
                            reminder["text"],
                            reminder["source_path"],
                            reminder["remind_at_epoch_ms"],
                            reminder["time_zone"],
                            timestamp,
                            device_id,
                            task_id,
                        ),
                    )
                else:
                    connection.execute(
                        "UPDATE reminders SET updated_at_ms=? WHERE device_id=? AND task_id=?",
                        (timestamp, device_id, task_id),
                    )
                accepted += 1

            if active_ids:
                placeholders = ",".join("?" for _ in active_ids)
                parameters: List[Any] = [timestamp, device_id]
                parameters.extend(sorted(active_ids))
                cancelled = connection.execute(
                    "UPDATE reminders SET status='cancelled', next_attempt_at_ms=0, "
                    "sending_started_at_ms=0, updated_at_ms=? "
                    "WHERE device_id=? AND task_id NOT IN (" + placeholders + ") "
                    "AND status != 'cancelled'",
                    parameters,
                ).rowcount
            else:
                cancelled = connection.execute(
                    "UPDATE reminders SET status='cancelled', next_attempt_at_ms=0, "
                    "sending_started_at_ms=0, updated_at_ms=? "
                    "WHERE device_id=? AND status != 'cancelled'",
                    (timestamp, device_id),
                ).rowcount
            connection.commit()
        return accepted, int(cancelled)

    def claim_due(self) -> Optional[Dict[str, Any]]:
        timestamp = now_ms()
        with self.connect() as connection:
            connection.execute("BEGIN IMMEDIATE")
            # A webhook request may already have been accepted when the process
            # exits before mark_sent() runs.  Feishu webhooks do not expose an
            # idempotency key we can query, so automatically recycling an
            # in-flight row would turn an uncertain delivery into a duplicate.
            # Keep it in 'sending' until the next snapshot changes or an
            # operator explicitly repairs the row.
            row = connection.execute(
                "SELECT device_id, task_id, task_text, source_path, remind_at_ms, time_zone, attempts "
                "FROM reminders WHERE status='scheduled' AND next_attempt_at_ms <= ? "
                "AND remind_at_ms > 0 AND remind_at_ms <= ? "
                "ORDER BY remind_at_ms ASC LIMIT 1",
                (timestamp, timestamp),
            ).fetchone()
            if row is None:
                connection.commit()
                return None
            next_attempts = int(row["attempts"]) + 1
            connection.execute(
                "UPDATE reminders SET status='sending', attempts=?, sending_started_at_ms=?, "
                "updated_at_ms=? WHERE device_id=? AND task_id=? AND status='scheduled'",
                (
                    next_attempts,
                    timestamp,
                    timestamp,
                    row["device_id"],
                    row["task_id"],
                ),
            )
            connection.commit()
            result = dict(row)
            result["attempts"] = next_attempts
            return result

    def mark_sent(self, reminder: Dict[str, Any]) -> None:
        timestamp = now_ms()
        with self.connect() as connection:
            connection.execute(
                "UPDATE reminders SET status='sent', sent_at_ms=?, next_attempt_at_ms=0, "
                "last_error='', sending_started_at_ms=0, updated_at_ms=? "
                "WHERE device_id=? AND task_id=? AND status='sending' AND attempts=?",
                (
                    timestamp,
                    timestamp,
                    reminder["device_id"],
                    reminder["task_id"],
                    reminder["attempts"],
                ),
            )
            connection.commit()

    def mark_failed(self, reminder: Dict[str, Any], error: str) -> None:
        timestamp = now_ms()
        attempts = int(reminder["attempts"])
        if attempts >= MAX_ATTEMPTS:
            status = "failed"
            next_attempt = 0
        else:
            status = "scheduled"
            backoff_seconds = min(3600, 60 * (2 ** max(0, attempts - 1)))
            next_attempt = timestamp + backoff_seconds * 1000
        with self.connect() as connection:
            connection.execute(
                "UPDATE reminders SET status=?, next_attempt_at_ms=?, last_error=?, "
                "sending_started_at_ms=0, updated_at_ms=? "
                "WHERE device_id=? AND task_id=? AND status='sending' AND attempts=?",
                (
                    status,
                    next_attempt,
                    error[:160],
                    timestamp,
                    reminder["device_id"],
                    reminder["task_id"],
                    attempts,
                ),
            )
            connection.commit()


def validate_snapshot(payload: Any) -> Tuple[str, Set[str], List[Dict[str, Any]]]:
    if not isinstance(payload, dict):
        raise ValueError("payload must be an object")
    device_id = payload.get("device_id")
    if not isinstance(device_id, str) or not device_id.strip():
        raise ValueError("device_id is required")
    device_id = device_id.strip()
    if len(device_id) > MAX_DEVICE_ID_LENGTH:
        raise ValueError("device_id is too long")

    observed_raw = payload.get("observed_task_ids", [])
    if not isinstance(observed_raw, list) or len(observed_raw) > MAX_OBSERVED_TASKS:
        raise ValueError("observed_task_ids is invalid")
    observed: Set[str] = set()
    for value in observed_raw:
        if not isinstance(value, str) or not value.strip() or len(value.strip()) > MAX_TASK_ID_LENGTH:
            raise ValueError("observed_task_ids contains an invalid task id")
        observed.add(value.strip())

    active_raw = payload.get("active_reminders", [])
    if not isinstance(active_raw, list) or len(active_raw) > MAX_ACTIVE_REMINDERS:
        raise ValueError("active_reminders is invalid")
    active: List[Dict[str, Any]] = []
    active_ids: Set[str] = set()
    for item in active_raw:
        if not isinstance(item, dict):
            raise ValueError("active_reminders contains an invalid item")
        task_id = item.get("task_id")
        text = item.get("text", "")
        source_path = item.get("source_path", "")
        time_zone = item.get("time_zone", "UTC")
        remind_at = item.get("remind_at_epoch_ms")
        if (
            not isinstance(task_id, str)
            or not task_id.strip()
            or len(task_id.strip()) > MAX_TASK_ID_LENGTH
            or task_id.strip() not in observed
            or task_id.strip() in active_ids
            or not isinstance(text, str)
            or len(text) > MAX_TASK_TEXT_LENGTH
            or not isinstance(source_path, str)
            or len(source_path) > MAX_SOURCE_PATH_LENGTH
            or not isinstance(time_zone, str)
            or not time_zone.strip()
            or len(time_zone.strip()) > MAX_TIME_ZONE_LENGTH
            or not isinstance(remind_at, int)
            or isinstance(remind_at, bool)
            or remind_at <= 0
        ):
            raise ValueError("active_reminders contains an invalid reminder")
        normalized_id = task_id.strip()
        active_ids.add(normalized_id)
        active.append(
            {
                "task_id": normalized_id,
                "text": text,
                "source_path": source_path,
                "remind_at_epoch_ms": remind_at,
                "time_zone": time_zone.strip(),
            }
        )
    return device_id, observed, active


class RelayApplication:
    def __init__(self, store: ReminderStore, webhook: str) -> None:
        self.store = store
        self.webhook = webhook
        self.stop_event = threading.Event()
        self.worker = threading.Thread(target=self.worker_loop, name="reminder-worker", daemon=True)

    def start(self) -> None:
        self.worker.start()

    def stop(self) -> None:
        self.stop_event.set()
        self.worker.join(timeout=5)

    def worker_loop(self) -> None:
        while not self.stop_event.wait(POLL_SECONDS):
            reminder = self.store.claim_due()
            if reminder is None:
                continue
            success, message = send_feishu(
                self.webhook,
                reminder["task_text"],
                format_reminder_time(reminder["remind_at_ms"], reminder["time_zone"]),
            )
            if success:
                self.store.mark_sent(reminder)
                LOGGER.info("reminder sent device=%s task=%s", reminder["device_id"], reminder["task_id"])
            else:
                self.store.mark_failed(reminder, message)
                LOGGER.warning(
                    "reminder delivery failed device=%s task=%s attempt=%s reason=%s",
                    reminder["device_id"],
                    reminder["task_id"],
                    reminder["attempts"],
                    message,
                )


class RequestHandler(BaseHTTPRequestHandler):
    application: RelayApplication
    api_token: str

    def log_message(self, format_string: str, *args: Any) -> None:
        LOGGER.info("http %s %s", self.command, self.path.split("?", 1)[0])

    def send_json(self, status: int, value: Dict[str, Any]) -> None:
        body = json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def authorized(self) -> bool:
        header = self.headers.get("Authorization", "")
        expected = "Bearer " + self.api_token
        return hmac.compare_digest(header, expected)

    def do_GET(self) -> None:  # noqa: N802
        if self.path.split("?", 1)[0] == "/healthz":
            self.send_json(200, {"ok": True, "service": SERVICE_NAME})
            return
        self.send_json(404, {"ok": False, "error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        if self.path.split("?", 1)[0] != "/v1/reminders/reconcile":
            self.send_json(404, {"ok": False, "error": "not found"})
            return
        if not self.authorized():
            self.send_json(401, {"ok": False, "error": "unauthorized"})
            return
        content_length = self.headers.get("Content-Length", "")
        try:
            length = int(content_length)
        except ValueError:
            length = -1
        if length < 0 or length > MAX_BODY_BYTES:
            self.send_json(413, {"ok": False, "error": "payload too large"})
            return
        try:
            body = self.rfile.read(length)
            payload = json.loads(body.decode("utf-8"))
            device_id, observed, active = validate_snapshot(payload)
            accepted, cancelled = self.application.store.reconcile(device_id, observed, active)
        except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as error:
            self.send_json(400, {"ok": False, "error": str(error)})
            return
        except Exception:
            LOGGER.exception("reconcile failed")
            self.send_json(500, {"ok": False, "error": "server error"})
            return
        self.send_json(200, {"ok": True, "accepted": accepted, "cancelled": cancelled})


def build_server(application: RelayApplication, host: str, port: int, api_token: str, cert_file: str, key_file: str) -> ThreadingHTTPServer:
    RequestHandler.application = application
    RequestHandler.api_token = api_token
    server = ThreadingHTTPServer((host, port), RequestHandler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(certfile=cert_file, keyfile=key_file)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    return server


def main() -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    api_token = required_env("CLOUD_REMINDER_API_TOKEN")
    if len(api_token) < 24:
        raise RuntimeError("CLOUD_REMINDER_API_TOKEN is too short")
    webhook = validate_https_url(required_env("CLOUD_REMINDER_FEISHU_WEBHOOK"), "CLOUD_REMINDER_FEISHU_WEBHOOK")
    cert_file = required_env("CLOUD_REMINDER_CERT_FILE")
    key_file = required_env("CLOUD_REMINDER_KEY_FILE")
    db_path = os.environ.get("CLOUD_REMINDER_DB", "/var/lib/dabawei-cloud-reminder/reminders.sqlite3")
    host = os.environ.get("CLOUD_REMINDER_HOST", DEFAULT_HOST)
    port = int(os.environ.get("CLOUD_REMINDER_PORT", str(DEFAULT_PORT)))
    store = ReminderStore(db_path)
    application = RelayApplication(store, webhook)
    server = build_server(application, host, port, api_token, cert_file, key_file)
    application.start()
    LOGGER.info("service listening on %s:%s", host, port)
    try:
        server.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        pass
    finally:
        server.shutdown()
        server.server_close()
        application.stop()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
