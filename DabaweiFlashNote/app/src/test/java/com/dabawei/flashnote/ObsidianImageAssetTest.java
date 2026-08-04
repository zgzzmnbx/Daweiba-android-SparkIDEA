package com.dabawei.flashnote;

public final class ObsidianImageAssetTest {
    public static void main(String[] args) {
        buildsAssetPathBesideTargetNote();
        buildsObsidianEmbedWithFixedWidth();
        sanitizesUnsafeFileName();
        System.out.println("ObsidianImageAsset tests passed.");
    }

    private static void buildsAssetPathBesideTargetNote() {
        String path = ObsidianImageAsset.buildAssetRemotePath(
                "OBS\\Damon\\【MOC】随手记-Claw编辑版.md",
                "flash-20260611-1101.jpg");

        assertEquals("OBS/Damon/assets/flash-20260611-1101.jpg", path, "asset remote path");
    }

    private static void buildsObsidianEmbedWithFixedWidth() {
        assertEquals(
                "![[assets/flash-20260611-1101.jpg|200]]",
                ObsidianImageAsset.buildObsidianEmbed("flash-20260611-1101.jpg"),
                "obsidian embed");
    }

    private static void sanitizesUnsafeFileName() {
        assertEquals(
                "a-b-c-d-.jpg",
                ObsidianImageAsset.sanitizeFileName("a/b:c?d*.jpg"),
                "safe file name");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + "\nExpected: " + expected + "\nActual:   " + actual);
        }
    }
}
