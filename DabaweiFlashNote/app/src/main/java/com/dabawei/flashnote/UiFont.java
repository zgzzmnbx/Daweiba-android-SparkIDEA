package com.dabawei.flashnote;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;

/** Pure UI font resolver; it never changes persisted business data. */
final class UiFont {
    static final String SYSTEM = "system";
    static final String CLAUDE = "claude";
    static final String PINGFANG = "pingfang";
    static final String PREF_STYLE_KEY = "font_style";
    static final String PREF_CLAUDE_KEY = "claude_font_enabled";

    private UiFont() {
    }

    static String loadPreference(SharedPreferences prefs) {
        String stored = prefs.getString(PREF_STYLE_KEY, null);
        if (PINGFANG.equals(stored) || CLAUDE.equals(stored) || SYSTEM.equals(stored)) {
            return stored;
        }
        return prefs.getBoolean(PREF_CLAUDE_KEY, false) ? CLAUDE : SYSTEM;
    }

    static Typeface body(Context context, String style) {
        if (CLAUDE.equals(style)) {
            return Typeface.create("serif", Typeface.NORMAL);
        }
        if (PINGFANG.equals(style) && Build.VERSION.SDK_INT >= 26) {
            return Typeface.create(context.getResources().getFont(R.font.pingfang_regular), Typeface.NORMAL);
        }
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    static Typeface medium(Context context, String style) {
        if (CLAUDE.equals(style)) {
            return Typeface.create("serif", Typeface.NORMAL);
        }
        if (PINGFANG.equals(style) && Build.VERSION.SDK_INT >= 26) {
            return Typeface.create(context.getResources().getFont(R.font.pingfang_regular), Typeface.NORMAL);
        }
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }
}
