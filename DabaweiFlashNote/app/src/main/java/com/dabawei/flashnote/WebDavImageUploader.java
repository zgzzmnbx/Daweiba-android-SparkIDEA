package com.dabawei.flashnote;

import android.util.Base64;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public final class WebDavImageUploader {
    public Result upload(byte[] bytes, String remotePath, String contentType, SyncSettings settings) {
        if (!settings.isReady()) {
            return Result.failed("WebDAV sync is not configured.");
        }
        if (bytes == null || bytes.length == 0) {
            return Result.failed("Image is empty.");
        }

        HttpURLConnection putConnection = null;
        try {
            URL url = new URL(WebDavUrlBuilder.build(settings.getBaseUrl(), remotePath));
            putConnection = open(url, "PUT", settings);
            putConnection.setDoOutput(true);
            putConnection.setRequestProperty("Content-Type", contentType == null ? "application/octet-stream" : contentType);
            putConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            OutputStream out = putConnection.getOutputStream();
            out.write(bytes);
            out.close();

            int putCode = putConnection.getResponseCode();
            if (putCode >= 200 && putCode < 300) {
                return Result.uploaded();
            }
            if (putCode == 409) {
                return Result.failed("图片目录不存在，请先在坚果云创建 OBS/Damon/assets");
            }
            return Result.failed("PUT image failed: HTTP " + putCode);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        } finally {
            if (putConnection != null) {
                putConnection.disconnect();
            }
        }
    }

    private static HttpURLConnection open(URL url, String method, SyncSettings settings) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        String auth = settings.getUsername() + ":" + settings.getPassword();
        String encoded = Base64.encodeToString(auth.getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        return connection;
    }

    public static final class Result {
        private final boolean uploaded;
        private final String message;

        private Result(boolean uploaded, String message) {
            this.uploaded = uploaded;
            this.message = message;
        }

        static Result uploaded() { return new Result(true, "Uploaded"); }
        static Result failed(String message) { return new Result(false, message); }

        public boolean isUploaded() { return uploaded; }
        public String getMessage() { return message; }
    }
}
