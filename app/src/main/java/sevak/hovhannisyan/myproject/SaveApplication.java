package sevak.hovhannisyan.myproject;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import sevak.hovhannisyan.myproject.ui.ThemeManager;
import sevak.hovhannisyan.myproject.worker.RecurringTransactionWorker;

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

            scheduleRecurringWorker();

        } catch (Exception e) {
            Log.e("SaveApplication", "Initialization error: " + e.getMessage());
        }
    }

    private void scheduleRecurringWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                RecurringTransactionWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RecurringTransactionWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.d("SaveApplication", "RecurringTransactionWorker scheduled.");
    }
}
