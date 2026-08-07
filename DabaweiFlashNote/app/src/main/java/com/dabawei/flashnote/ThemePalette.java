package com.dabawei.flashnote;

/**
 * Pure-Java theme contract shared by the Android UI and the resource checks.
 * The legacy entries remain readable for migration tests, but are never offered
 * as visual themes after v0.6.0.
 */
public final class ThemePalette {
    private static final ThemePalette PAPER = new ThemePalette(
            "paper", "纸张", "#FBFAF7", "#FFFFFF", "#1F2328", "#8B9097",
            "#1C4E35", "#1C4E35", "#EDECE7", "#FFFFFF",
            "#F0F7F4", "#EEF6F2", "#2D5A47", "#2D5A47", "#1C4E35");
    private static final ThemePalette INK = new ThemePalette(
            "ink", "夜墨", "#111614", "#1B2320", "#EEF4EE", "#A9B5AE",
            "#8FCBA9", "#5EA17D", "#34413B", "#18201D",
            "#22362D", "#1F3038", "#BDE8CD", "#9ECFEB", "#5EA17D");
    private static final ThemePalette FOREST = new ThemePalette(
            "forest", "森绿", "#E6EFE7", "#F9FCF8", "#10251B", "#60736A",
            "#2E7D56", "#1E5B3F", "#B9CDBF", "#EEF6ED",
            "#DDEDE4", "#EAF4EA", "#1F5B3F", "#2A6A4F", "#1E5B3F");
    private static final ThemePalette APPLE = new ThemePalette(
            "apple", "Apple", "#F5F5F7", "#FFFFFF", "#1D1D1F", "#7A7A7A",
            "#0066CC", "#0057B8", "#E0E0E0", "#FAFAFC",
            "#EAF4FF", "#EDF8F1", "#0A5EA8", "#1D7A43", "#34C759");
    private static final ThemePalette LINEAR = new ThemePalette(
            "linear", "Linear", "#010102", "#0F1011", "#F7F8F8", "#8A8F98",
            "#5E6AD2", "#4D58B8", "#34343A", "#141516",
            "#20233A", "#1F2B28", "#AEB6FF", "#8FD6B0", "#26A269");
    private static final ThemePalette NOTION = new ThemePalette(
            "notion", "Notion", "#F6F5F4", "#FFFFFF", "#37352F", "#787671",
            "#5645D4", "#4534B3", "#E5E3DF", "#FAFAF9",
            "#F2EFE8", "#EEF2ED", "#5F4B32", "#4F6658", "#0F7B6C");
    private static final ThemePalette RAYCAST = new ThemePalette(
            "raycast", "Raycast", "#18191D", "#22242A", "#F3F4F7", "#A7ABB5",
            "#FF6363", "#E54D4D", "#3A3D46", "#202228",
            "#342629", "#332B22", "#FFB1A8", "#FFC285", "#E54D4D");
    private static final ThemePalette OBSIDIAN = new ThemePalette(
            "obsidian", "Obsidian", "#1E1B2E", "#28243A", "#F2ECFF", "#B7A9D6",
            "#8B5CF6", "#6D42D9", "#463B62", "#241F34",
            "#302848", "#2B3148", "#C9B8FF", "#9DB7FF", "#7C3AED");

    private static final ThemePalette SYSTEM_OPTION = new ThemePalette(
            "system", "跟随系统", "#FFFFFF", "#FFFFFF", "#171717", "#64748B",
            "#2563EB", "#1D4ED8", "#E2E8F0", "#FFFFFF",
            "#F8FAFC", "#EFF6FF", "#334155", "#1E3A8A", "#EFF6FF");
    private static final ThemePalette LIGHT = new ThemePalette(
            "light", "浅色", "#FFFFFF", "#FFFFFF", "#171717", "#64748B",
            "#2563EB", "#1D4ED8", "#E2E8F0", "#FFFFFF",
            "#F8FAFC", "#EFF6FF", "#334155", "#1E3A8A", "#EFF6FF");
    private static final ThemePalette DARK = new ThemePalette(
            "dark", "深色", "#0A0A0A", "#171717", "#FAFAFA", "#A3A3A3",
            "#60A5FA", "#2563EB", "#404040", "#262626",
            "#262626", "#172554", "#F8FAFC", "#DBEAFE", "#172554");

    private static final ThemePalette[] LEGACY_THEMES = new ThemePalette[]{
            PAPER, INK, FOREST, APPLE, LINEAR, NOTION, RAYCAST, OBSIDIAN};
    private static final ThemePalette[] PREFERENCE_OPTIONS = new ThemePalette[]{
            SYSTEM_OPTION, LIGHT, DARK};

    private final String key;
    private final String label;
    private final String screenColor;
    private final String surfaceColor;
    private final String primaryTextColor;
    private final String secondaryTextColor;
    private final String accentColor;
    private final String accentDarkColor;
    private final String borderColor;
    private final String inputColor;
    private final String saveButtonColor;
    private final String todoButtonColor;
    private final String saveButtonTextColor;
    private final String todoButtonTextColor;
    private final String syncButtonColor;

    private ThemePalette(
            String key,
            String label,
            String screenColor,
            String surfaceColor,
            String primaryTextColor,
            String secondaryTextColor,
            String accentColor,
            String accentDarkColor,
            String borderColor,
            String inputColor,
            String saveButtonColor,
            String todoButtonColor,
            String saveButtonTextColor,
            String todoButtonTextColor,
            String syncButtonColor) {
        this.key = key;
        this.label = label;
        this.screenColor = screenColor;
        this.surfaceColor = surfaceColor;
        this.primaryTextColor = primaryTextColor;
        this.secondaryTextColor = secondaryTextColor;
        this.accentColor = accentColor;
        this.accentDarkColor = accentDarkColor;
        this.borderColor = borderColor;
        this.inputColor = inputColor;
        this.saveButtonColor = saveButtonColor;
        this.todoButtonColor = todoButtonColor;
        this.saveButtonTextColor = saveButtonTextColor;
        this.todoButtonTextColor = todoButtonTextColor;
        this.syncButtonColor = syncButtonColor;
    }

    /** Legacy list retained so existing pure-Java compatibility tests stay meaningful. */
    public static ThemePalette[] all() {
        return LEGACY_THEMES.clone();
    }

    /** Only the three preference choices are shown in Settings. */
    public static ThemePalette[] preferences() {
        return PREFERENCE_OPTIONS.clone();
    }

    public static ThemePalette findByKey(String key) {
        for (ThemePalette theme : LEGACY_THEMES) {
            if (theme.key.equals(key)) {
                return theme;
            }
        }
        if ("light".equals(key)) {
            return LIGHT;
        }
        if ("dark".equals(key)) {
            return DARK;
        }
        if ("system".equals(key)) {
            return SYSTEM_OPTION;
        }
        return PAPER;
    }

    public static ThemePalette next(String key) {
        ThemePalette current = findByKey(key);
        for (int i = 0; i < LEGACY_THEMES.length; i++) {
            if (LEGACY_THEMES[i].key.equals(current.key)) {
                return LEGACY_THEMES[(i + 1) % LEGACY_THEMES.length];
            }
        }
        return PAPER;
    }

    public static String migratePreference(String key) {
        if (key == null || key.trim().length() == 0) {
            return "system";
        }
        if ("system".equals(key) || "light".equals(key) || "dark".equals(key)) {
            return key;
        }
        if ("paper".equals(key) || "forest".equals(key) || "apple".equals(key) || "notion".equals(key)) {
            return "light";
        }
        if ("ink".equals(key) || "linear".equals(key) || "raycast".equals(key) || "obsidian".equals(key)) {
            return "dark";
        }
        return "system";
    }

    public static ThemePalette resolve(String key, boolean systemIsDark) {
        return "dark".equals(migratePreference(key)) || ("system".equals(migratePreference(key)) && systemIsDark)
                ? DARK
                : LIGHT;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getScreenColor() {
        return screenColor;
    }

    public String getSurfaceColor() {
        return surfaceColor;
    }

    public String getPrimaryTextColor() {
        return primaryTextColor;
    }

    public String getSecondaryTextColor() {
        return secondaryTextColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getAccentDarkColor() {
        return accentDarkColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public String getInputColor() {
        return inputColor;
    }

    public String getSaveButtonColor() {
        return saveButtonColor;
    }

    public String getTodoButtonColor() {
        return todoButtonColor;
    }

    public String getSaveButtonTextColor() {
        return saveButtonTextColor;
    }

    public String getTodoButtonTextColor() {
        return todoButtonTextColor;
    }

    public String getSyncButtonColor() {
        return syncButtonColor;
    }

    public String getForegroundColor() {
        return primaryTextColor;
    }

    public String getMutedForegroundColor() {
        return secondaryTextColor;
    }

    public String getCardColor() {
        return surfaceColor;
    }

    public String getAccentForegroundColor() {
        return "dark".equals(key) ? "#DBEAFE" : "#1E3A8A";
    }

    public String getPrimaryButtonTextColor() {
        return "dark".equals(key) ? "#FFFFFF" : "#FFFFFF";
    }

    public String getDestructiveColor() {
        return "dark".equals(key) ? "#F87171" : "#DC2626";
    }

    public String getDestructiveTextColor() {
        return "dark".equals(key) ? "#450A0A" : "#FFFFFF";
    }

    public String getSuccessColor() {
        return "dark".equals(key) ? "#14532D" : "#DCFCE7";
    }

    public String getSuccessTextColor() {
        return "dark".equals(key) ? "#BBF7D0" : "#166534";
    }

    public String getWarningColor() {
        return "dark".equals(key) ? "#1E3A8A" : "#DBEAFE";
    }

    public String getWarningTextColor() {
        return "dark".equals(key) ? "#DBEAFE" : "#1E40AF";
    }
}
