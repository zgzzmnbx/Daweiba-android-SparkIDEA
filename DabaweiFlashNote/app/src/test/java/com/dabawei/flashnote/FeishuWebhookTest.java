package com.dabawei.flashnote;

public final class FeishuWebhookTest {
    public static void main(String[] args) {
        buildsEscapedTextPayload();
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
