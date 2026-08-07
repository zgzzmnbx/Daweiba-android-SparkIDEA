package com.dabawei.flashnote;

public final class ThemePaletteTest {
    public static void main(String[] args) {
        hasEightNamedThemes();
        cyclesThemesInOrder();
        fallsBackToPaperTheme();
        actionButtonsUseThemeColors();
        hasOnlyThreeThemePreferences();
        migratesLegacyThemeKeys();
        resolvesSystemToTwoVisualThemes();
        System.out.println("ThemePalette tests passed.");
    }

    private static void hasEightNamedThemes() {
        ThemePalette[] themes = ThemePalette.all();
        assertEquals(8, themes.length, "theme count");
        assertEquals("paper", themes[0].getKey(), "first theme key");
        assertEquals("ink", themes[1].getKey(), "second theme key");
        assertEquals("forest", themes[2].getKey(), "third theme key");
        assertEquals("apple", themes[3].getKey(), "fourth theme key");
        assertEquals("linear", themes[4].getKey(), "fifth theme key");
        assertEquals("notion", themes[5].getKey(), "sixth theme key");
        assertEquals("raycast", themes[6].getKey(), "seventh theme key");
        assertEquals("obsidian", themes[7].getKey(), "eighth theme key");
    }

    private static void cyclesThemesInOrder() {
        assertEquals("ink", ThemePalette.next("paper").getKey(), "paper next");
        assertEquals("forest", ThemePalette.next("ink").getKey(), "ink next");
        assertEquals("apple", ThemePalette.next("forest").getKey(), "forest next");
        assertEquals("linear", ThemePalette.next("apple").getKey(), "apple next");
        assertEquals("notion", ThemePalette.next("linear").getKey(), "linear next");
        assertEquals("raycast", ThemePalette.next("notion").getKey(), "notion next");
        assertEquals("obsidian", ThemePalette.next("raycast").getKey(), "raycast next");
        assertEquals("paper", ThemePalette.next("obsidian").getKey(), "obsidian next");
    }

    private static void fallsBackToPaperTheme() {
        assertEquals("paper", ThemePalette.findByKey("missing").getKey(), "missing fallback");
        assertEquals("paper", ThemePalette.findByKey(null).getKey(), "null fallback");
    }

    private static void actionButtonsUseThemeColors() {
        for (ThemePalette theme : ThemePalette.all()) {
            assertHexColor(theme.getSaveButtonColor(), theme.getKey() + " save button color");
            assertHexColor(theme.getTodoButtonColor(), theme.getKey() + " todo button color");
            assertHexColor(theme.getSaveButtonTextColor(), theme.getKey() + " save text color");
            assertHexColor(theme.getTodoButtonTextColor(), theme.getKey() + " todo text color");
            assertHexColor(theme.getSyncButtonColor(), theme.getKey() + " sync button color");
            assertNotEquals(theme.getSaveButtonColor(), theme.getTodoButtonColor(), theme.getKey() + " save/todo colors");
        }
        ThemePalette paper = ThemePalette.findByKey("paper");
        assertEquals("#F0F7F4", paper.getSaveButtonColor(), "paper save action");
        assertEquals("#EEF6F2", paper.getTodoButtonColor(), "paper todo action");
        assertEquals("#2D5A47", paper.getSaveButtonTextColor(), "paper save text");
        assertEquals("#2D5A47", paper.getTodoButtonTextColor(), "paper todo text");
        ThemePalette apple = ThemePalette.findByKey("apple");
        assertEquals("#EAF4FF", apple.getSaveButtonColor(), "apple save action");
        assertEquals("#EDF8F1", apple.getTodoButtonColor(), "apple todo action");
        assertEquals("#34C759", apple.getSyncButtonColor(), "apple sync action");
        ThemePalette raycast = ThemePalette.findByKey("raycast");
        assertEquals("#342629", raycast.getSaveButtonColor(), "raycast save action");
        assertEquals("#332B22", raycast.getTodoButtonColor(), "raycast todo action");
    }

    private static void hasOnlyThreeThemePreferences() {
        ThemePalette[] preferences = ThemePalette.preferences();
        assertEquals(3, preferences.length, "theme preference count");
        assertEquals("system", preferences[0].getKey(), "system preference");
        assertEquals("light", preferences[1].getKey(), "light preference");
        assertEquals("dark", preferences[2].getKey(), "dark preference");
    }

    private static void migratesLegacyThemeKeys() {
        for (String key : new String[]{"paper", "forest", "apple", "notion"}) {
            assertEquals("light", ThemePalette.migratePreference(key), key + " light migration");
        }
        for (String key : new String[]{"ink", "linear", "raycast", "obsidian"}) {
            assertEquals("dark", ThemePalette.migratePreference(key), key + " dark migration");
        }
        assertEquals("system", ThemePalette.migratePreference(null), "missing preference defaults to system");
        assertEquals("system", ThemePalette.migratePreference("unknown"), "unknown preference defaults to system");
    }

    private static void resolvesSystemToTwoVisualThemes() {
        assertEquals("light", ThemePalette.resolve("system", false).getKey(), "system light resolution");
        assertEquals("dark", ThemePalette.resolve("system", true).getKey(), "system dark resolution");
        assertEquals("light", ThemePalette.resolve("paper", true).getKey(), "legacy light ignores system");
        assertEquals("dark", ThemePalette.resolve("ink", false).getKey(), "legacy dark ignores system");
        assertEquals("#FFFFFF", ThemePalette.resolve("light", true).getPrimaryButtonTextColor(), "light button text");
        assertEquals("#F87171", ThemePalette.resolve("dark", false).getDestructiveColor(), "dark destructive token");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertNotEquals(Object unexpected, Object actual, String label) {
        if (unexpected.equals(actual)) {
            throw new AssertionError(label + ": values should differ, both were " + actual);
        }
    }

    private static void assertHexColor(String value, String label) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) {
            throw new AssertionError(label + ": invalid color " + value);
        }
    }
}
