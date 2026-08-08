#!/usr/bin/env python3
"""Regression tests for the cloud reminder delivery state machine."""

from __future__ import annotations

import os
import sqlite3
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))

import cloud_reminder_service as service


class CloudReminderServiceTest(unittest.TestCase):
    @staticmethod
    def read_state(database_path: str) -> sqlite3.Row:
        connection = sqlite3.connect(database_path)
        try:
            connection.row_factory = sqlite3.Row
            row = connection.execute(
                "SELECT status, sent_at_ms, attempts, next_attempt_at_ms, "
                "last_error, sending_started_at_ms FROM reminders "
                "WHERE device_id='device-1' AND task_id='task-1'"
            ).fetchone()
        finally:
            connection.close()
        if row is None:
            raise AssertionError("reminder row is missing")
        return row

    def test_process_restart_does_not_requeue_in_flight_delivery(self) -> None:
        clock = [2_000_000_000_000]
        original_now_ms = service.now_ms
        service.now_ms = lambda: clock[0]
        try:
            with tempfile.TemporaryDirectory() as directory:
                database_path = os.path.join(directory, "reminders.sqlite3")
                store = service.ReminderStore(database_path)
                accepted, cancelled = store.reconcile(
                    "device-1",
                    {"task-1"},
                    [
                        {
                            "task_id": "task-1",
                            "text": "买牛奶",
                            "source_path": "待办.md",
                            "remind_at_epoch_ms": clock[0] - 1,
                            "time_zone": "Asia/Shanghai",
                        }
                    ],
                )
                self.assertEqual((1, 0), (accepted, cancelled))

                claimed = store.claim_due()
                self.assertIsNotNone(claimed)

                # Simulate Restart=always while the webhook result is still
                # uncertain. The new process must not send the same row again.
                restarted_store = service.ReminderStore(database_path)
                self.assertIsNone(restarted_store.claim_due())

                # The old implementation used a ten-minute lease, which made
                # the duplicate visible at exactly the interval reported by the
                # user. It must remain protected after that interval as well.
                clock[0] += 10 * 60 * 1000 + 1
                self.assertIsNone(restarted_store.claim_due())
        finally:
            service.now_ms = original_now_ms

    def test_confirmed_delivery_is_not_claimed_again(self) -> None:
        clock = [2_000_000_000_000]
        original_now_ms = service.now_ms
        service.now_ms = lambda: clock[0]
        try:
            with tempfile.TemporaryDirectory() as directory:
                database_path = os.path.join(directory, "reminders.sqlite3")
                store = service.ReminderStore(database_path)
                store.reconcile(
                    "device-1",
                    {"task-1"},
                    [
                        {
                            "task_id": "task-1",
                            "text": "买牛奶",
                            "source_path": "待办.md",
                            "remind_at_epoch_ms": clock[0] - 1,
                            "time_zone": "Asia/Shanghai",
                        }
                    ],
                )
                claimed = store.claim_due()
                self.assertIsNotNone(claimed)
                store.mark_sent(claimed)
                state = self.read_state(database_path)
                self.assertEqual("sent", state["status"])
                self.assertEqual(clock[0], state["sent_at_ms"])
                self.assertEqual(1, state["attempts"])
                self.assertEqual(0, state["next_attempt_at_ms"])
                self.assertEqual(0, state["sending_started_at_ms"])

                restarted_store = service.ReminderStore(database_path)
                self.assertIsNone(restarted_store.claim_due())
        finally:
            service.now_ms = original_now_ms

    def test_confirmed_failure_is_committed_and_retried_after_backoff(self) -> None:
        clock = [2_000_000_000_000]
        original_now_ms = service.now_ms
        service.now_ms = lambda: clock[0]
        try:
            with tempfile.TemporaryDirectory() as directory:
                database_path = os.path.join(directory, "reminders.sqlite3")
                store = service.ReminderStore(database_path)
                store.reconcile(
                    "device-1",
                    {"task-1"},
                    [
                        {
                            "task_id": "task-1",
                            "text": "买牛奶",
                            "source_path": "待办.md",
                            "remind_at_epoch_ms": clock[0] - 1,
                            "time_zone": "Asia/Shanghai",
                        }
                    ],
                )
                claimed = store.claim_due()
                self.assertIsNotNone(claimed)
                store.mark_failed(claimed, "network error")

                state = self.read_state(database_path)
                self.assertEqual("scheduled", state["status"])
                self.assertEqual(1, state["attempts"])
                self.assertEqual(clock[0] + 60_000, state["next_attempt_at_ms"])
                self.assertEqual("network error", state["last_error"])
                self.assertEqual(0, state["sending_started_at_ms"])
                self.assertIsNone(store.claim_due())

                clock[0] += 60_000
                retried = store.claim_due()
                self.assertIsNotNone(retried)
                self.assertEqual(2, retried["attempts"])
        finally:
            service.now_ms = original_now_ms


if __name__ == "__main__":
    unittest.main()
