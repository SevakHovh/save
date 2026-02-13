package sevak.hovhannisyan.myproject;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;
import sevak.hovhannisyan.myproject.ui.ThemeManager;

/**
 * Application class for SAVE app.
 * Required for Hilt dependency injection.
 */
@HiltAndroidApp
public class SaveApplication extends Application {
    
    @Override
    public void onCreate() {
        // Apply saved theme before activities are created
        ThemeManager.applySavedTheme(this);
        super.onCreate();
    }
}
