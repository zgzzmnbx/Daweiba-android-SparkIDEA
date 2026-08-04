package com.dabawei.flashnote;

public final class ThemePalette {
    private static final ThemePalette PAPER = new ThemePalette(
            "paper", "纸张", "#FBFAF7", "#FFFFFF", "#1F2328", "#8B9097",
            "#1C4E35", "#1C4E35", "#EDECE7", "#FFFFFF");
    private static final ThemePalette INK = new ThemePalette(
            "ink", "夜墨", "#111614", "#1B2320", "#EEF4EE", "#A9B5AE",
            "#8FCBA9", "#5EA17D", "#34413B", "#18201D");
    private static final ThemePalette FOREST = new ThemePalette(
            "forest", "森绿", "#E6EFE7", "#F9FCF8", "#10251B", "#60736A",
            "#2E7D56", "#1E5B3F", "#B9CDBF", "#EEF6ED");
    private static final ThemePalette APPLE = new ThemePalette(
            "apple", "Apple", "#F5F5F7", "#FFFFFF", "#1D1D1F", "#7A7A7A",
            "#0066CC", "#0057B8", "#E0E0E0", "#FAFAFC");
    private static final ThemePalette LINEAR = new ThemePalette(
            "linear", "Linear", "#010102", "#0F1011", "#F7F8F8", "#8A8F98",
            "#5E6AD2", "#4D58B8", "#34343A", "#141516");
    private static final ThemePalette NOTION = new ThemePalette(
            "notion", "Notion", "#F6F5F4", "#FFFFFF", "#37352F", "#787671",
            "#5645D4", "#4534B3", "#E5E3DF", "#FAFAF9");
    private static final ThemePalette RAYCAST = new ThemePalette(
            "raycast", "Raycast", "#18191D", "#22242A", "#F3F4F7", "#A7ABB5",
            "#FF6363", "#E54D4D", "#3A3D46", "#202228");
    private static final ThemePalette OBSIDIAN = new ThemePalette(
            "obsidian", "Obsidian", "#1E1B2E", "#28243A", "#F2ECFF", "#B7A9D6",
            "#8B5CF6", "#6D42D9", "#463B62", "#241F34");

    private static final ThemePalette[] THEMES = new ThemePalette[]{
            PAPER, INK, FOREST, APPLE, LINEAR, NOTION, RAYCAST, OBSIDIAN};

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
            String inputColor) {
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
    }

    public static ThemePalette[] all() {
        return THEMES.clone();
    }

    public static ThemePalette findByKey(String key) {
        for (ThemePalette theme : THEMES) {
            if (theme.key.equals(key)) {
                return theme;
            }
        }
        return PAPER;
    }

    public static ThemePalette next(String key) {
        ThemePalette current = findByKey(key);
        for (int i = 0; i < THEMES.length; i++) {
            if (THEMES[i].key.equals(current.key)) {
                return THEMES[(i + 1) % THEMES.length];
            }
        }
        return PAPER;
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
        switch (key) {
            case "paper":
                return "#F0F7F4";
            case "ink":
                return "#22362D";
            case "forest":
                return "#DDEDE4";
            case "apple":
                return "#EAF4FF";
            case "linear":
                return "#20233A";
            case "notion":
                return "#F2EFE8";
            case "raycast":
                return "#342629";
            case "obsidian":
                return "#302848";
            default:
                return inputColor;
        }
    }

    public String getTodoButtonColor() {
        switch (key) {
            case "paper":
                return "#EEF6F2";
            case "ink":
                return "#1F3038";
            case "forest":
                return "#EAF4EA";
            case "apple":
                return "#EDF8F1";
            case "linear":
                return "#1F2B28";
            case "notion":
                return "#EEF2ED";
            case "raycast":
                return "#332B22";
            case "obsidian":
                return "#2B3148";
            default:
                return surfaceColor;
        }
    }

    public String getSaveButtonTextColor() {
        switch (key) {
            case "paper":
                return "#2D5A47";
            case "ink":
                return "#BDE8CD";
            case "forest":
                return "#1F5B3F";
            case "apple":
                return "#0A5EA8";
            case "linear":
                return "#AEB6FF";
            case "notion":
                return "#5F4B32";
            case "raycast":
                return "#FFB1A8";
            case "obsidian":
                return "#C9B8FF";
            default:
                return accentDarkColor;
        }
    }

    public String getTodoButtonTextColor() {
        switch (key) {
            case "paper":
                return "#2D5A47";
            case "ink":
                return "#9ECFEB";
            case "forest":
                return "#2A6A4F";
            case "apple":
                return "#1D7A43";
            case "linear":
                return "#8FD6B0";
            case "notion":
                return "#4F6658";
            case "raycast":
                return "#FFC285";
            case "obsidian":
                return "#9DB7FF";
            default:
                return accentDarkColor;
        }
    }

    public String getSyncButtonColor() {
        switch (key) {
            case "paper":
                return "#1C4E35";
            case "ink":
                return "#5EA17D";
            case "forest":
                return "#1E5B3F";
            case "apple":
                return "#34C759";
            case "linear":
                return "#26A269";
            case "notion":
                return "#0F7B6C";
            case "raycast":
                return "#E54D4D";
            case "obsidian":
                return "#7C3AED";
            default:
                return accentDarkColor;
        }
    }
}
