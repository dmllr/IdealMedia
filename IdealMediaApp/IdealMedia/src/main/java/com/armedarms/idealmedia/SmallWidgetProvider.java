package com.armedarms.idealmedia;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.MediaUtils;

public class SmallWidgetProvider extends AppWidgetProvider {
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mContext = context;
        final App app = (App)context.getApplicationContext();
        final PlayerService service = app.getService();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.notification);

                ComponentName componentName = new ComponentName(mContext, PlayerService.class);

                for (int currentAppWidgetId : appWidgetIds) {
                    Intent intentNext = new Intent(PlayerService.NEXT);
                    intentNext.setComponent(componentName);
                    views.setOnClickPendingIntent(R.id.action_next, PendingIntent.getService(mContext, 0, intentNext, 0));

                    Intent intentPrev = new Intent(PlayerService.PREV);
                    intentPrev.setComponent(componentName);
                    views.setOnClickPendingIntent(R.id.action_prev, PendingIntent.getService(mContext, 0, intentPrev, 0));

                    Intent intentPlay = new Intent(PlayerService.PLAY);
                    intentPlay.setComponent(componentName);
                    views.setOnClickPendingIntent(R.id.action_play, PendingIntent.getService(mContext, 0, intentPlay, 0));

                    Intent intentOpen = new Intent(context, NavigationActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intentOpen, 0);
                    views.setOnClickPendingIntent(R.id.notifAlbum, pendingIntent);
                    views.setOnClickPendingIntent(R.id.notifTitle, pendingIntent);
                    views.setOnClickPendingIntent(R.id.notifArtist, pendingIntent);

                    if (service != null) {
                        Track track = service.currentTrack;

                        views.setImageViewResource(R.id.action_play, service.isPlaying() ? R.drawable.ic_pause_normal : R.drawable.ic_play_normal);

                        if (track != null) {
                            views.setTextViewText(R.id.notifTitle, track.getTitle());
                            views.setTextViewText(R.id.notifArtist, track.getArtist());

                            Bitmap cover =  MediaUtils.getArtworkQuick(context, track, 180, 180);
                            if (cover != null)
                                views.setImageViewBitmap(R.id.notifAlbum, cover);
                            else
                                views.setImageViewResource(R.id.notifAlbum,  R.drawable.ic_launcher);
                        }
                    } else {
                        views.setTextViewText(R.id.notifTitle, "-");
                        views.setTextViewText(R.id.notifArtist, "");
                        views.setImageViewResource(R.id.notifAlbum,  R.drawable.ic_launcher);
                        views.setImageViewResource(R.id.action_play, R.drawable.ic_play_normal);
                    }

                    try {
                        appWidgetManager.updateAppWidget(currentAppWidgetId, views);
                    } catch (Exception ignored) { }
                }

                return null;
            }
        }.execute();
    }
}
