package com.kalmar.blockcalls;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.kalmar.blockcalls.services.WidgetCounterService;
import com.kalmar.blockcalls.utils.PauseParams;


public class BlockPauseWidget extends AppWidgetProvider {
    public static final String TIMER_CLICK = "com.kalmar.blockcalls.APPWIDGET_CLICK";
    public static final String RECEIVE_PROGRESS = "com.kalmar.blockcalls.RECEIVE_PROGRESS";
    public static final String RECEIVE_END_COUNT = "com.kalmar.blockcalls.RECEIVE_END_COUNT";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.block_pause_widget
        );

        views.setViewVisibility(R.id.widget_text_view, View.GONE);
        views.setViewVisibility(R.id.widget_progress_bar, View.GONE);
        views.setViewVisibility(R.id.widget_progress_bar_placeholder, View.VISIBLE);

        views.setOnClickPendingIntent(
                R.id.widget_container,
                getPendingSelfIntent(context, appWidgetId, TIMER_CLICK));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager
                .getAppWidgetIds(new ComponentName(context, BlockPauseWidget.class));

        if (TIMER_CLICK.equals(intent.getAction())) {
            widgetActive(context, appWidgetManager, appWidgetIds);

            PauseParams pauseParams = PauseParams.getFromPreferences(context);
            pauseParams.increaseStep();
            pauseParams.saveToPreferences(context);

            if(!WidgetCounterService.isServiceRunning(context)) {
                context.stopService(new Intent(context, WidgetCounterService.class));
                context.startForegroundService(new Intent(context, WidgetCounterService.class));
            }

            Intent serviceIntent = new Intent(context, WidgetCounterService.class);
            serviceIntent.setAction(WidgetCounterService.COUNTER_UPDATE);
            context.sendBroadcast(serviceIntent);

            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(
                        context.getPackageName(),
                        R.layout.block_pause_widget
                );

                views.setTextViewText(
                        R.id.widget_text_view,
                        String.valueOf(pauseParams.getRemainingMinutes())
                );

                views.setProgressBar(
                        R.id.widget_progress_bar,
                        100,
                        pauseParams.getCurrentProgress(),
                        false
                );

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }

        if(RECEIVE_PROGRESS.equals(intent.getAction())) {
            PauseParams pauseParams = PauseParams.getFromPreferences(context);

            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(
                        context.getPackageName(),
                        R.layout.block_pause_widget
                );

                views.setTextViewText(
                        R.id.widget_text_view,
                        String.valueOf(pauseParams.getRemainingMinutes())
                );
                
                views.setProgressBar(
                        R.id.widget_progress_bar,
                        100, 
                        pauseParams.getCurrentProgress(),
                        false
                );

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }

        if(RECEIVE_END_COUNT.equals(intent.getAction())) {
            PauseParams.getCleared().saveToPreferences(context);

            widgetInactive(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private void widgetActive(Context context, 
                              AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.block_pause_widget
            );

            views.setViewVisibility(R.id.widget_text_view, View.VISIBLE);
            views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE);
            views.setViewVisibility(R.id.widget_progress_bar_placeholder, View.GONE);

            views.setOnClickPendingIntent(
                    R.id.widget_container,
                    getPendingSelfIntent(context, appWidgetId, TIMER_CLICK));


            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private void widgetInactive(
            Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.block_pause_widget
            );

            views.setViewVisibility(R.id.widget_text_view, View.GONE);
            views.setViewVisibility(R.id.widget_progress_bar, View.GONE);
            views.setViewVisibility(R.id.widget_progress_bar_placeholder, View.VISIBLE);

            views.setTextViewText(R.id.widget_text_view, "0");

            views.setOnClickPendingIntent(
                    R.id.widget_container,
                    getPendingSelfIntent(context, appWidgetId, TIMER_CLICK));

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    static protected PendingIntent getPendingSelfIntent(
            Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, BlockPauseWidget.class);
        intent.putExtra("AppWidgetId", appWidgetId);
        intent.setAction(action);

        return PendingIntent.getBroadcast(context, 0,
                intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
