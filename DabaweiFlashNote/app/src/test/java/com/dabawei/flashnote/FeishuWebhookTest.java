package com.dabawei.flashnote;

public final class FeishuWebhookTest {
    public static void main(String[] args) {
        buildsEscapedTextPayload();
        buildsReminderCardPayload();
        validatesHttpsWebhookOnly();
        System.out.println("Feishu webhook tests passed.");
    }

    private static void buildsEscapedTextPayload() {
        String payload = FeishuWebhookClient.buildTextPayload("line1\n\"quoted\"\\tail");
        assertEquals(
                "{\"msg_type\":\"text\",\"content\":{\"text\":\"line1\\n\\\"quoted\\\"\\\\tail\"}}",
                payload,
                "escaped text payload");
    }

    private static void buildsReminderCardPayload() {
        String payload = FeishuWebhookClient.buildReminderCardPayload(
                "整理\"报告\"", "2026-08-07 10:00");
        assertTrue(payload.contains("\"msg_type\":\"interactive\""), "interactive card type");
        assertTrue(payload.contains("\"schema\":\"2.0\""), "card schema");
        assertTrue(payload.contains("\"tag\":\"markdown\""), "markdown body");
        assertTrue(payload.contains("整理\\\"报告\\\""), "escaped task text");
        assertTrue(payload.contains("大尾巴闪念.手机端"), "mobile source");
    }

    private static void validatesHttpsWebhookOnly() {
        assertTrue(
                FeishuWebhookClient.isValidWebhookUrl(
                        "https://open.feishu.cn/open-apis/bot/v2/hook/test"),
                "HTTPS webhook accepted");
        assertTrue(
                !FeishuWebhookClient.isValidWebhookUrl(
                        "http://open.feishu.cn/open-apis/bot/v2/hook/test"),
                "HTTP webhook rejected");
        assertTrue(!FeishuWebhookClient.isValidWebhookUrl("not-a-url"), "invalid webhook rejected");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }
}
