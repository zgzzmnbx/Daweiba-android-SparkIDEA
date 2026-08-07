package com.dabawei.flashnote;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

public final class FeishuWebhookClient {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 5000;
    private static final String REMINDER_SOURCE = "大尾巴闪念.手机端";

    private FeishuWebhookClient() {
    }

    public static boolean isValidWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().length() == 0) {
            return false;
        }
        try {
            URL url = new URL(webhookUrl.trim());
            return "https".equalsIgnoreCase(url.getProtocol())
                    && url.getHost() != null
                    && url.getHost().trim().length() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String buildTextPayload(String text) {
        return "{\"msg_type\":\"text\",\"content\":{\"text\":\""
                + escapeJson(text)
                + "\"}}";
    }

    public static String buildReminderCardPayload(String taskText, String reminderTime) {
        String body = "📝 **待办内容**\\n"
                + safeText(taskText, "未填写待办内容")
                + "\\n\\n⏰ **提醒时间**\\n"
                + safeText(reminderTime, "未记录")
                + "\\n\\n📱 **来源**\\n"
                + REMINDER_SOURCE;
        return "{\"msg_type\":\"interactive\",\"card\":{"
                + "\"schema\":\"2.0\","
                + "\"config\":{\"update_multi\":true},"
                + "\"body\":{"
                + "\"direction\":\"vertical\","
                + "\"padding\":\"12px 12px 12px 12px\","
                + "\"elements\":[{"
                + "\"tag\":\"markdown\","
                + "\"content\":\"" + escapeJson(body) + "\","
                + "\"text_align\":\"left\","
                + "\"text_size\":\"normal_v2\","
                + "\"margin\":\"0px 0px 0px 0px\""
                + "}]},"
                + "\"header\":{"
                + "\"title\":{\"tag\":\"plain_text\",\"content\":\"大尾巴闪念\"},"
                + "\"subtitle\":{\"tag\":\"plain_text\",\"content\":\"手机端 · 待办提醒\"},"
                + "\"template\":\"blue\","
                + "\"padding\":\"12px 12px 12px 12px\""
                + "}}}";
    }

    public static Result send(String webhookUrl, String text) {
        if (!isValidWebhookUrl(webhookUrl)) {
            return Result.failed("飞书 Webhook 地址不可用");
        }
        return sendPayload(webhookUrl, buildTextPayload(text));
    }

    public static Result sendReminderCard(String webhookUrl, String taskText, String reminderTime) {
        if (!isValidWebhookUrl(webhookUrl)) {
            return Result.failed("飞书 Webhook 地址不可用");
        }
        return sendPayload(webhookUrl, buildReminderCardPayload(taskText, reminderTime));
    }

    private static Result sendPayload(String webhookUrl, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl.trim());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] body = payload.getBytes(UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream output = connection.getOutputStream();
            try {
                output.write(body);
                output.flush();
            } finally {
                output.close();
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            String response = readResponse(responseStream);
            if (statusCode < 200 || statusCode >= 300) {
                return Result.failed("飞书返回 HTTP " + statusCode);
            }
            if (response.length() == 0 || response.contains("\"code\":0")
                    || response.contains("\"code\": 0")
                    || response.contains("\"StatusCode\":0")
                    || response.contains("\"StatusCode\": 0")) {
                return Result.success(statusCode);
            }
            return Result.failed("飞书机器人未接受消息");
        } catch (Exception ignored) {
            return Result.failed("飞书网络请求失败");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        return value.trim();
    }

    private static String readResponse(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[512];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), UTF_8);
        } finally {
            stream.close();
        }
    }

    private static String escapeJson(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        builder.append(String.format(Locale.US, "\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    public static final class Result {
        private final boolean success;
        private final String message;
        private final int statusCode;

        private Result(boolean success, String message, int statusCode) {
            this.success = success;
            this.message = message;
            this.statusCode = statusCode;
        }

        private static Result success(int statusCode) {
            return new Result(true, "推送成功", statusCode);
        }

        private static Result failed(String message) {
            return new Result(false, message, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
