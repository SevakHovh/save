package sevak.hovhannisyan.myproject.ui;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.di.AppModule;

/**
 * Singleton to manage app theme (light / dark / system) and persist user choice.
 */
@Singleton
public class ThemeManager {

    private final SharedPreferences prefs;

    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private static final String KEY_THEME_MODE = "theme_mode";

    @Inject
    public ThemeManager(@Named(AppModule.THEME_PREFS) SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void applySavedTheme() {
        int mode = getSavedMode();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public void setTheme(int mode) {
        saveMode(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public int getSavedMode() {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    private void saveMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }
}
