package com.armedarms.idealmedia;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class App extends Application {
    private PlayerService mBoundService = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((PlayerService.BassServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }

    };

    public PlayerService getService() {
        return mBoundService;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        startService(new Intent(this, PlayerService.class));

        bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        unbindService(mConnection);

        super.onTerminate();
    }
}
