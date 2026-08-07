package com.dabawei.flashnote;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class CloudReminderClient {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT_MILLIS = 6000;
    private static final int READ_TIMEOUT_MILLIS = 8000;
    private static final String ENDPOINT = "/v1/reminders/reconcile";

    private CloudReminderClient() {
    }

    public static Result reconcile(
            Context context,
            FlashNoteDatabase database,
            List<TodoSyncItem> incoming) {
        CloudReminderSettings settings = CloudReminderSettings.load(context);
        if (!settings.isEnabled()) {
            CloudReminderSettings.recordSync(context, true, "云端提醒已关闭");
            return Result.success("云端提醒已关闭");
        }
        if (!settings.isReady()) {
            CloudReminderSettings.recordSync(context, false, "云端提醒未配置");
            return Result.failed("云端提醒未配置");
        }
        String payload = buildPayload(context, database, incoming);
        return post(context, settings, payload);
    }

    public static Result clear(Context context) {
        CloudReminderSettings settings = CloudReminderSettings.load(context);
        if (!CloudReminderSettings.isValidBaseUrl(settings.getBaseUrl())
                || settings.getApiToken().length() < 24
                || settings.getCertSha256().length() != 64) {
            CloudReminderSettings.recordSync(context, false, "云端提醒未配置，无法清理");
            return Result.failed("云端提醒未配置，无法清理");
        }
        String payload = "{\"device_id\":\"" + escapeJson(deviceId(context))
                + "\",\"observed_task_ids\":[],\"active_reminders\":[]}";
        return post(context, settings, payload);
    }

    private static String buildPayload(
            Context context,
            FlashNoteDatabase database,
            List<TodoSyncItem> incoming) {
        Set<String> observed = new HashSet<>();
        StringBuilder observedJson = new StringBuilder();
        if (incoming != null) {
            for (TodoSyncItem item : incoming) {
                if (item == null || item.getTaskId().trim().length() == 0) {
                    continue;
                }
                String taskId = item.getTaskId().trim();
                if (!observed.add(taskId)) {
                    continue;
                }
                if (observedJson.length() > 0) {
                    observedJson.append(',');
                }
                observedJson.append('\"').append(escapeJson(taskId)).append('\"');
            }
        }

        long now = System.currentTimeMillis();
        StringBuilder activeJson = new StringBuilder();
        Set<String> activeTaskIds = new HashSet<>();
        List<ReminderRecord> records = database.getRemoteReminders();
        for (ReminderRecord record : records) {
            if (record == null || !observed.contains(record.getTaskId())
                    || record.getRemindAt() <= now
                    || !(ReminderRecord.STATUS_SCHEDULED.equals(record.getStatus())
                    || ReminderRecord.STATUS_SNOOZED.equals(record.getStatus()))
                    || !activeTaskIds.add(record.getTaskId())) {
                continue;
            }
            if (activeJson.length() > 0) {
                activeJson.append(',');
            }
            String timeZoneId = record.getTimeZoneId();
            if (timeZoneId == null || timeZoneId.trim().length() == 0) {
                timeZoneId = TimeZone.getDefault().getID();
            }
            activeJson.append('{')
                    .append("\"task_id\":\"").append(escapeJson(record.getTaskId())).append("\",")
                    .append("\"text\":\"").append(escapeJson(record.getTaskText())).append("\",")
                    .append("\"source_path\":\"").append(escapeJson(record.getSourcePath())).append("\",")
                    .append("\"remind_at_epoch_ms\":").append(record.getRemindAt()).append(',')
                    .append("\"time_zone\":\"").append(escapeJson(timeZoneId)).append("\"}");
        }
        return "{\"device_id\":\"" + escapeJson(deviceId(context))
                + "\",\"observed_task_ids\":[" + observedJson
                + "],\"active_reminders\":[" + activeJson + "]}";
    }

    private static String deviceId(Context context) {
        android.content.SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences("dabawei_cloud_reminder", Context.MODE_PRIVATE);
        String stored = prefs.getString("device_id", "");
        if (stored != null && stored.trim().length() > 0) {
            return stored.trim();
        }
        String generated = java.util.UUID.randomUUID().toString();
        prefs.edit().putString("device_id", generated).apply();
        return generated;
    }

    private static Result post(Context context, CloudReminderSettings settings, String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(settings.getBaseUrl() + ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            if (connection instanceof HttpsURLConnection) {
                configurePinnedTls((HttpsURLConnection) connection, settings.getCertSha256());
            }
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + settings.getApiToken());
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
            String response = readResponse(statusCode >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream());
            if (statusCode < 200 || statusCode >= 300 || !response.contains("\"ok\":true")) {
                Result failed = Result.failed("云端返回 HTTP " + statusCode);
                CloudReminderSettings.recordSync(context, false, failed.getMessage());
                return failed;
            }
            Result success = Result.success("云端已接收");
            CloudReminderSettings.recordSync(context, true, success.getMessage());
            return success;
        } catch (Exception e) {
            Result failed = Result.failed("云端请求失败");
            CloudReminderSettings.recordSync(context, false, failed.getMessage());
            return failed;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void configurePinnedTls(
            HttpsURLConnection connection,
            String expectedFingerprint) throws Exception {
        final String expected = CloudReminderSettings.formatFingerprint(expectedFingerprint);
        if (expected.length() != 64) {
            return;
        }
        TrustManager[] managers = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                if (chain == null || chain.length == 0) {
                    throw new CertificateException("云端证书为空");
                }
                try {
                    String actual = toHex(MessageDigest.getInstance("SHA-256")
                            .digest(chain[0].getEncoded()));
                    if (!expected.equals(actual)) {
                        throw new CertificateException("云端证书指纹不匹配");
                    }
                } catch (CertificateException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CertificateException("云端证书校验失败");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, managers, new SecureRandom());
        SSLSocketFactory factory = context.getSocketFactory();
        connection.setSSLSocketFactory(factory);
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
                if (output.size() > 8192) {
                    break;
                }
            }
            return new String(output.toByteArray(), UTF_8);
        } finally {
            stream.close();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return builder.toString().toUpperCase(Locale.US);
    }

    private static String escapeJson(String value) {
        String text = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"': builder.append("\\\""); break;
                case '\\': builder.append("\\\\"); break;
                case '\b': builder.append("\\b"); break;
                case '\f': builder.append("\\f"); break;
                case '\n': builder.append("\\n"); break;
                case '\r': builder.append("\\r"); break;
                case '\t': builder.append("\\t"); break;
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

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static Result success(String message) {
            return new Result(true, message);
        }

        static Result failed(String message) {
            return new Result(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
