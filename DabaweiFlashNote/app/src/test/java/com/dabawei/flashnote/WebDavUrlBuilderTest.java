package com.dabawei.flashnote;

public final class WebDavUrlBuilderTest {
    public static void main(String[] args) {
        buildsEncodedNutstoreUrlFromWindowsStylePath();
        trimsDuplicateSlashes();
        System.out.println("WebDavUrlBuilder tests passed.");
    }

    private static void buildsEncodedNutstoreUrlFromWindowsStylePath() {
        String url = WebDavUrlBuilder.build(
                "https://dav.jianguoyun.com/dav/",
                "OBS\\Damon\\【MOC】随手记-Claw编辑版.md");

        assertEquals(
                "https://dav.jianguoyun.com/dav/OBS/Damon/%E3%80%90MOC%E3%80%91%E9%9A%8F%E6%89%8B%E8%AE%B0-Claw%E7%BC%96%E8%BE%91%E7%89%88.md",
                url,
                "encoded Nutstore URL");
    }

    private static void trimsDuplicateSlashes() {
        String url = WebDavUrlBuilder.build(
                "https://dav.jianguoyun.com/dav/",
                "/OBS/Damon/test note.md");

        assertEquals(
                "https://dav.jianguoyun.com/dav/OBS/Damon/test%20note.md",
                url,
                "slash trimming");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + "\nExpected: " + expected + "\nActual:   " + actual);
        }
    }
}
