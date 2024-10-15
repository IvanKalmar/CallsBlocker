package com.kalmar.blockcalls.services;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.kalmar.blockcalls.BlockPauseWidget;
import com.kalmar.blockcalls.utils.PauseParams;
import com.kalmar.blockcalls.R;

public class WidgetCounterService extends Service {
    public static final String COUNTER_UPDATE = "COUNTER_UPDATE";
    public static final String CHANNEL_ID = "COUNTER_NOTIFICATIONS";
    public static final int NOTIFICATION_ID = 1234;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private final Handler handler = new Handler();
    private Runnable runnable;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(COUNTER_UPDATE)) {
                handler.removeCallbacks(runnable);

                startCount();
            }
        }
    };

    public WidgetCounterService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Calls blocker notification channel",
                NotificationManager.IMPORTANCE_MIN
        );

        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(ContextCompat.getColor(
                        getApplicationContext(), R.color.notification_background_color
                ))
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Calls blocking is temporarily suspended")
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setVibrate(null);

        Notification notification = notificationBuilder.build();

        notificationManager.notify(NOTIFICATION_ID, notification);

        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        IntentFilter filter = new IntentFilter();
        filter.addAction(COUNTER_UPDATE);

        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);

        startCount();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(NOTIFICATION_ID);
        unregisterReceiver(receiver);
    }

    public void startCount() {
        final int delay = 10 * 1000; // 1000 milliseconds == 1 second

        runnable = new Runnable() {
            public void run() {
                PauseParams pauseParams = PauseParams.getFromPreferences(getApplicationContext());

                if(!pauseParams.isPaused()) {
                    PauseParams.getCleared().saveToPreferences(getApplicationContext());
                    Intent serviceIntent = new Intent(getApplicationContext(), BlockPauseWidget.class);
                    serviceIntent.setAction(BlockPauseWidget.RECEIVE_END_COUNT);
                    getApplicationContext().sendBroadcast(serviceIntent);
                    stopSelf();
                } else {
                    Intent serviceIntent = new Intent(getApplicationContext(), BlockPauseWidget.class);
                    serviceIntent.setAction(BlockPauseWidget.RECEIVE_PROGRESS);
                    getApplicationContext().sendBroadcast(serviceIntent);

                    notificationBuilder.setContentText(
                            "Minutes remaining: " + pauseParams.getRemainingMinutes());
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

                    handler.postDelayed(this, delay);
                }
            }
        };

        handler.post(runnable);
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (WidgetCounterService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
