package com.dabawei.flashnote;

public final class ObsidianImageAsset {
    public static final String ASSET_FOLDER = "assets";
    public static final int DISPLAY_WIDTH = 200;

    private ObsidianImageAsset() {
    }

    public static String buildAssetRemotePath(String noteRemotePath, String fileName) {
        String safeFileName = sanitizeFileName(fileName);
        String path = noteRemotePath == null ? "" : noteRemotePath.trim().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slash = path.lastIndexOf('/');
        if (slash < 0) {
            return ASSET_FOLDER + "/" + safeFileName;
        }
        return path.substring(0, slash + 1) + ASSET_FOLDER + "/" + safeFileName;
    }

    public static String buildAssetCollectionPath(String noteRemotePath) {
        String assetPath = buildAssetRemotePath(noteRemotePath, "placeholder");
        int slash = assetPath.lastIndexOf('/');
        return slash < 0 ? ASSET_FOLDER : assetPath.substring(0, slash);
    }

    public static String buildObsidianEmbed(String fileName) {
        return "![[" + ASSET_FOLDER + "/" + sanitizeFileName(fileName) + "|" + DISPLAY_WIDTH + "]]";
    }

    public static String sanitizeFileName(String fileName) {
        String safe = fileName == null ? "" : fileName.trim();
        if (safe.length() == 0) {
            safe = "flash-image.jpg";
        }
        safe = safe.replace('\\', '-').replace('/', '-').replace(':', '-');
        safe = safe.replace('*', '-').replace('?', '-').replace('"', '-');
        safe = safe.replace('<', '-').replace('>', '-').replace('|', '-');
        while (safe.contains("..")) {
            safe = safe.replace("..", ".");
        }
        return safe;
    }
}
