package com.dabawei.flashnote;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class WebDavUrlBuilder {
    private WebDavUrlBuilder() {
    }

    public static String build(String baseUrl, String remotePath) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = remotePath == null ? "" : remotePath.trim().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.length() == 0) {
            return base;
        }
        return base + "/" + encodePath(path);
    }

    private static String encodePath(String path) {
        String[] parts = path.split("/");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("/");
            }
            builder.append(encodeSegment(part));
        }
        return builder.toString();
    }

    private static String encodeSegment(String segment) {
        try {
            return URLEncoder.encode(segment, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
