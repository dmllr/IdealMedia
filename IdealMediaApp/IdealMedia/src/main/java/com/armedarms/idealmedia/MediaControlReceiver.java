package com.armedarms.idealmedia;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event == null)
                return;
            if (event.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (event.getKeyCode())
            {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    sendMessage(context, PlayerService.PLAY);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    sendMessage(context, PlayerService.PLAY);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                    sendMessage(context, PlayerService.PLAY);
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    sendMessage(context, PlayerService.NEXT);
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    sendMessage(context, PlayerService.PREV);
                    break;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    sendMessage(context, PlayerService.VOLUME_UP);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    sendMessage(context, PlayerService.VOLUME_DOWN);
                    break;
            }
        }

    }

    void sendMessage(Context context, String msg){
        Intent sendIntent = null;
        PendingIntent pendingIntent = null;

        sendIntent = new Intent(msg);
        sendIntent.setComponent(new ComponentName(context, PlayerService.class));
        pendingIntent = PendingIntent.getService(context,0, sendIntent, 0);

        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }
}
