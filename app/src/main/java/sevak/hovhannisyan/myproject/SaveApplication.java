package sevak.hovhannisyan.myproject;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import sevak.hovhannisyan.myproject.ui.ThemeManager;

/**
 * Application class for SAVE app.
 * Required for Hilt dependency injection and global initialization.
 */
@HiltAndroidApp
public class SaveApplication extends Application {

    @Inject
    ThemeManager themeManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SaveApplication", "App starting, initializing components...");
        
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            
            // Apply saved theme safely
            if (themeManager != null) {
                themeManager.applySavedTheme();
            }
        } catch (Exception e) {
            Log.e("SaveApplication", "Initialization error: " + e.getMessage());
        }
    }
}
