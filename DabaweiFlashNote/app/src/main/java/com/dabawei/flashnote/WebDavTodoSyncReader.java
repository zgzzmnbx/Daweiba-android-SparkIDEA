package com.dabawei.flashnote;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public final class WebDavTodoSyncReader {
    public Result read(SyncSettings settings) {
        if (!settings.isReady()) {
            return Result.failed("WebDAV sync is not configured.");
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(WebDavUrlBuilder.build(settings.getBaseUrl(), TodoSyncDefaults.REMOTE_PATH));
            connection = open(url, settings);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return Result.failed("GET todo failed: HTTP " + code);
            }
            String markdown = readUtf8(connection.getInputStream());
            return Result.success(TodoSyncParser.parse(markdown));
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection open(URL url, SyncSettings settings) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        String auth = settings.getUsername() + ":" + settings.getPassword();
        String encoded = Base64.encodeToString(auth.getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        return connection;
    }

    private static String readUtf8(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), Charset.forName("UTF-8"));
    }

    public static final class Result {
        private final boolean success;
        private final List<TodoSyncItem> items;
        private final String message;

        private Result(boolean success, List<TodoSyncItem> items, String message) {
            this.success = success;
            this.items = items;
            this.message = message;
        }

        static Result success(List<TodoSyncItem> items) {
            return new Result(true, items, "Synced");
        }

        static Result failed(String message) {
            return new Result(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<TodoSyncItem> getItems() {
            return items;
        }

        public String getMessage() {
            return message;
        }
    }
}
