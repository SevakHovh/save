package sevak.hovhannisyan.myproject;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import sevak.hovhannisyan.myproject.di.AppModule;

public class GoalReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "goal_notifications";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(AppModule.GOAL_PREFS, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        
        if (isEnabled) {
            showNotification(context);
        }
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Goal Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, GoalActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Daily Savings Check-in")
                .setContentText("Did you save any money today? Update your goal progress!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }
}
