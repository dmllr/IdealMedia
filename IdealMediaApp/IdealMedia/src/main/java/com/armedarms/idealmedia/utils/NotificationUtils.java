package com.armedarms.idealmedia.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.PlayerService;
import com.armedarms.idealmedia.domain.Track;

public class NotificationUtils {

    public static Notification getNotification(Context context, PendingIntent pendingIntent, Track track, boolean isPlaying) {

        Notification notification = new Notification();
        if (track != null) {
            notification.contentView = getNotificationViews(track, context, isPlaying);
        }
        else {
            notification.setLatestEventInfo(context, "", "", pendingIntent);
        }
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        notification.contentIntent = pendingIntent;
        notification.icon = R.drawable.ic_notification;

        return notification;
    }

    public static RemoteViews getNotificationViews(final Track track, final Context context,  boolean isPlaying) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification);

        views.setTextViewText(R.id.notifTitle, track.getTitle());
        views.setTextViewText(R.id.notifArtist, track.getArtist());

        Bitmap cover =  MediaUtils.getArtworkQuick(context, track, 180, 180);
        if (cover != null)
            views.setImageViewBitmap(R.id.notifAlbum, cover);
        else
            views.setImageViewResource(R.id.notifAlbum,  R.drawable.ic_launcher);

        if (Build.VERSION.SDK_INT < 11) {
            views.setViewVisibility(R.id.action_prev, View.GONE);
            views.setViewVisibility(R.id.action_play, View.GONE);
            views.setViewVisibility(R.id.action_next, View.GONE);
        }

        views.setImageViewResource(R.id.action_play, isPlaying ? R.drawable.selector_pause_button : R.drawable.selector_play_button);

        ComponentName componentName = new ComponentName(context, PlayerService.class);

        Intent intentPlay = new Intent(PlayerService.PLAY);
        intentPlay.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.action_play, PendingIntent.getService(context, 0, intentPlay, 0));

        Intent intentNext = new Intent(PlayerService.NEXT);
        intentNext.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.action_next, PendingIntent.getService(context, 0, intentNext, 0));

        Intent intentPrevious = new Intent(PlayerService.PREV);
        intentPrevious.setComponent(componentName);
        views.setOnClickPendingIntent(R.id.action_prev, PendingIntent.getService(context, 0, intentPrevious, 0));

        return views;
    }
}
