package com.dabawei.flashnote;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class WebDavMarkdownSync {
    public Result sync(FlashNote note, SyncSettings settings) {
        ArrayList<FlashNote> notes = new ArrayList<>();
        notes.add(note);
        return sync(notes, settings);
    }

    public Result sync(FlashNote note, SyncSettings settings, FlashNoteDatabase database) {
        ArrayList<FlashNote> notes = new ArrayList<>();
        notes.add(note);
        return sync(notes, settings, database);
    }

    public Result sync(List<FlashNote> notes, SyncSettings settings) {
        return sync(notes, settings, null);
    }

    public Result sync(List<FlashNote> notes, SyncSettings settings, FlashNoteDatabase database) {
        if (!settings.isReady()) {
            return Result.skipped("WebDAV sync is not configured.");
        }
        if (notes == null || notes.isEmpty()) {
            return Result.skipped("No pending notes.");
        }

        HttpURLConnection getConnection = null;
        HttpURLConnection putConnection = null;
        try {
            URL url = new URL(WebDavUrlBuilder.build(settings.getBaseUrl(), settings.getRemotePath()));
            getConnection = open(url, "GET", settings);
            int getCode = getConnection.getResponseCode();
            if (getCode < 200 || getCode >= 300) {
                return Result.failed("GET failed: HTTP " + getCode);
            }

            String etag = getConnection.getHeaderField("ETag");
            String markdown = readUtf8(getConnection.getInputStream());
            ArrayList<String> lines = new ArrayList<>();
            for (FlashNote note : notes) {
                ReminderRecord localReminder = database == null
                        ? null
                        : database.getReminderForLocalNote(note.getId());
                lines.add(MarkdownAnchorInserter.formatNoteLine(
                        note.getContent(),
                        note.getCreatedAtMillis(),
                        TimeZone.getDefault(),
                        note.getNoteType(),
                        note.getNoteType() == FlashNote.TYPE_TODO
                                ? ReminderIds.localTaskId(note.getId())
                                : "",
                        localReminder == null ? "" : localReminder.getDueAtText(),
                        localReminder == null
                                || ReminderRecord.STATUS_CANCELLED.equals(localReminder.getStatus())
                                ? ""
                                : localReminder.getRemindAtText().length() > 0
                                ? localReminder.getRemindAtText()
                                : TodoDateTime.format(localReminder.getRemindAt())));
            }
            MarkdownAnchorInserter.Result inserted = MarkdownAnchorInserter.insertLinesBelowAnchor(
                    markdown,
                    settings.getAnchor(),
                    lines);
            if (!inserted.isInserted()) {
                return Result.failed(inserted.getErrorMessage());
            }

            byte[] payload = inserted.getMarkdown().getBytes(Charset.forName("UTF-8"));
            putConnection = open(url, "PUT", settings);
            putConnection.setDoOutput(true);
            putConnection.setRequestProperty("Content-Type", "text/markdown; charset=utf-8");
            putConnection.setRequestProperty("Content-Length", String.valueOf(payload.length));
            if (etag != null && etag.length() > 0) {
                putConnection.setRequestProperty("If-Match", etag);
            }
            OutputStream out = putConnection.getOutputStream();
            out.write(payload);
            out.close();

            int putCode = putConnection.getResponseCode();
            if (putCode >= 200 && putCode < 300) {
                return Result.synced();
            }
            if (putCode == 412) {
                return Result.failed("Remote file changed. Please retry after Nutstore sync settles.");
            }
            return Result.failed("PUT failed: HTTP " + putCode);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        } finally {
            if (getConnection != null) {
                getConnection.disconnect();
            }
            if (putConnection != null) {
                putConnection.disconnect();
            }
        }
    }

    private static HttpURLConnection open(URL url, String method, SyncSettings settings) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
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
        private final boolean synced;
        private final boolean skipped;
        private final String message;

        private Result(boolean synced, boolean skipped, String message) {
            this.synced = synced;
            this.skipped = skipped;
            this.message = message;
        }

        static Result synced() { return new Result(true, false, "Synced"); }
        static Result skipped(String message) { return new Result(false, true, message); }
        static Result failed(String message) { return new Result(false, false, message); }

        public boolean isSynced() { return synced; }
        public boolean isSkipped() { return skipped; }
        public String getMessage() { return message; }
    }
}
