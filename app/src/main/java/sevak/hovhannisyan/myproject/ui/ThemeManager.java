package sevak.hovhannisyan.myproject.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Simple helper to manage app theme (light / dark) and persist user choice.
 */
public class ThemeManager {

    private static final String PREFS_NAME = "save_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;

    private ThemeManager() {
        // no-op
    }

    public static void applySavedTheme(Context context) {
        int mode = getSavedMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void setTheme(Context context, int mode) {
        saveMode(context, mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private static int getSavedMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_LIGHT);
    }

    private static void saveMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
}

