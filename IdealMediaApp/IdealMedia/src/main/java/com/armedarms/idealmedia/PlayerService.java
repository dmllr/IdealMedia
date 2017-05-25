package com.armedarms.idealmedia;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.armedarms.idealmedia.domain.IPlayerController;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tools.MakePlaylistFS;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.MediaUtils;
import com.armedarms.idealmedia.utils.NotificationUtils;
import com.un4seen.bass.BASS;

import java.io.File;
import java.util.ArrayList;

public class PlayerService extends Service implements AudioManager.OnAudioFocusChangeListener {

    public static String PLAY = "com.armedarms.idealmedia.MediaControlReceiver.PLAY";
    public static String NEXT = "com.armedarms.idealmedia.MediaControlReceiver.NEXT";
    public static String PREV = "com.armedarms.idealmedia.MediaControlReceiver.PREV";
    public static String VOLUME_UP = "com.armedarms.idealmedia.MediaControlReceiver.VOLUME_UP";
    public static String VOLUME_DOWN = "com.armedarms.idealmedia.MediaControlReceiver.VOLUME_DOWN";

    // Notification
	private Notification notification;
	
	// Pending Intent to be called if a user click on the notification
	private PendingIntent pendIntent;
    private boolean repeat;
    private int errorCount = 0;

    // Media button counter
    private long mediabtnLastEventTime=0;
    private int mediabtnPressCounter=0;

    private String playlistSnapshot;

    private int[] fxBandHandles;
    private float[] fxBandGains;

    private volatile int stream;

    //TrackList
    private ArrayList<Track> tracks = new ArrayList<Track>();

    Track currentTrack = null;

    //currentPosition
    private int position = 0;

    public int getPlayingPosition() {
        return position;
    }

    // Activity with implemented BassInterface
    private IPlayerController playerInterface;
    private int screenHeight, screenWidth;

    // our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClient remoteControlClient = null;
    //MediaSession mediaSession = null;


    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle){
        this.shuffle = shuffle;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public void setActivityStarted(boolean activityStarted) {
        this.activityStarted = activityStarted;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    public boolean isSnapshotMismatch(String snapshot) {
        return playlistSnapshot == null || snapshot == null || !playlistSnapshot.equals(snapshot);
    }
    public boolean isSnapshotMatch(String snapshot) {
        return !isSnapshotMismatch(snapshot);
    }

    // Bass Service Binder Class
	public class BassServiceBinder extends Binder {
		public PlayerService getService() {
            return PlayerService.this;
        }
    }
	
    public void setController(IPlayerController player) {
		this.playerInterface = player;

		if(playerInterface != null) {
            playerInterface.onPluginsLoaded(plugins);
            playerInterface.onFileLoaded(null, 0, "", "", 0, 0);
            playerInterface.onProgressChanged(0, 0, 0);
		}
	}

	// Properties: BassInterface
	private String plugins;
	private double duration = 0.0;
	private double progress = 0.0;
    private boolean shuffle = false;
    private boolean firstVolumeUpFlag;
    public long startVolumeUpFlag;
    private boolean activityStarted;
    private boolean isUnpluggedFlag;

	// Bass Service Binder
	private final IBinder mBinder = new BassServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

    private TelephonyManager tm;
    private ServiceBroadcastReceiver myBroadcastReceiver;
    private AudioManager mAudioManager;

    enum RING_STATE {
        STATE_RINGING, STATE_OFFHOOK, STATE_NORMAL
    }

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if(BASS.BASS_ChannelIsActive(stream) == BASS.BASS_ACTIVE_PLAYING) {
                if(playerInterface != null && activityStarted) {

                    progress = BASS.BASS_ChannelBytes2Seconds(stream, BASS.BASS_ChannelGetPosition(stream, BASS.BASS_POS_BYTE));
                    playerInterface.onProgressChanged(position, progress, duration);
                }
            }
            timerHandler.postDelayed(this, 200); //looks like laggy timer on more then 200 values
        }
    };

    @Override
	public void onCreate() {
		super.onCreate();

		// initialize default output device
		if (!BASS.BASS_Init(-1, 44100, 0)) {
			return;
		}

		// look for plugins
		plugins = "";
        String path = getApplicationInfo().nativeLibraryDir;
		String[] list = new File(path).list();
		for (String s: list) {
			int plug = BASS.BASS_PluginLoad(path+"/"+s, 0);
			if (plug != 0) { // plugin loaded...
				plugins += s + "\n"; // add it to the list
			}
		}
		if (plugins.equals(""))
            plugins = "no plugins - visit the BASS webpage to get some\n";
		if(playerInterface != null) {
			playerInterface.onPluginsLoaded(plugins);
		}

        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 1000);
        Log.w("BASS.BASS_CONFIG_BUFFER", "" + BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER));

		// Pending Intend
		Intent intent = new Intent(this, NavigationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pendIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //tracklist
        loadTracks();
        loadEqualizerValues();

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(telephone, PhoneStateListener.LISTEN_CALL_STATE);

        myBroadcastReceiver = new ServiceBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(myBroadcastReceiver, intentFilter);

        ComponentName rcvMedia = new ComponentName(getPackageName(), MediaControlReceiver.class.getName());
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mAudioManager.registerMediaButtonEventReceiver(rcvMedia);

        // Use the remote control APIs (if available) to set the playback state
        if (Build.VERSION.SDK_INT >= 14 && remoteControlClient == null) {
            registerRemoteControl(rcvMedia);
        }
	}

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void registerRemoteControl(ComponentName rcvMedia) {
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(rcvMedia);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                0, mediaButtonIntent, 0);
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);

        remoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
        );
        mAudioManager.registerRemoteControlClient(remoteControlClient);
    }

    public void loadTracks(){
        tracks = (ArrayList<Track>) FileUtils.read(getString(R.string.key_tracks), getApplicationContext());
        playlistSnapshot = "loaded from cache";
    }

    public void setTracks(ArrayList<Track> data, String snapshot){
        if(data != null){
            tracks = data;
            if(position >= 0){
                int newPlayingTrackIndex = data.indexOf(currentTrack);
                if(newPlayingTrackIndex >= 0){
                    position = newPlayingTrackIndex;
                }
            }
        } else {
            tracks = new ArrayList<Track>();
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                FileUtils.write(getString(R.string.key_tracks), getApplicationContext(), tracks);
                return null;
            }
        }.execute();

        playlistSnapshot = snapshot;
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    private PhoneStateListener telephone = new PhoneStateListener() {
        boolean onhook = false;
        RING_STATE callstaet;

        public void onCallStateChanged(int state, String number) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING: {
                    callstaet = RING_STATE.STATE_RINGING;
                    if (isPlaying()) {
                        pause();
                        onhook = true;
                        //setResumeStop(CALL_RESUME);
                    }
                }
                break;
                case TelephonyManager.CALL_STATE_OFFHOOK: {
                    if (callstaet == RING_STATE.STATE_RINGING) {
                        callstaet = RING_STATE.STATE_OFFHOOK;
                    } else {
                        callstaet = RING_STATE.STATE_NORMAL;
                        if (isPlaying()) {
                            pause();
                            onhook = true;
                            //setResumeStop(CALL_RESUME);
                        }
                    }
                }
                break;
                case TelephonyManager.CALL_STATE_IDLE: {
                    if (onhook) {
                        onhook = false;
                        if (isPaused())
                            playFromPause();
                        //setResumeStart(5, CALL_RESUME);
                    }
                    callstaet = RING_STATE.STATE_NORMAL;
                }
                break;
                default: {

                }
            }
        }
    };

	@Override
	public void onDestroy() {
        if (tm != null) {
            tm.listen(telephone, PhoneStateListener.LISTEN_NONE);
            tm = null;
        }
        if (myBroadcastReceiver != null) {
            unregisterReceiver(myBroadcastReceiver);
        }
        if (mAudioManager != null) {
            mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaControlReceiver.class.getName()));
        }
        if (Build.VERSION.SDK_INT >= 14 && remoteControlClient != null) {
            unregisterRemoteControl();
        }
		// "free" the output device and all plugins
		BASS.BASS_Free();
		BASS.BASS_PluginFree(0);

		// Stop foreground
		stopForeground(true);
        stopUpdateProgress();

		super.onDestroy();
	}

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void unregisterRemoteControl() {
        mAudioManager.unregisterRemoteControlClient(remoteControlClient);
    }

    final BASS.SYNCPROC EndSync=new BASS.SYNCPROC() {
        public void SYNCPROC(int handle, int channel, int data, Object user) {
            saveEqualizerValues();
            if (!isRepeat())
                playNext();
            else
                play(position);
        }
    };

    private void saveEqualizerValues() {
        if (fxBandGains != null)
            FileUtils.write(getString(R.string.key_equalizer), getApplicationContext(), fxBandGains);
    }

    private void loadEqualizerValues() {
        if (fxBandGains == null || fxBandGains.length < 10)
            fxBandGains = (float[])FileUtils.read(getString(R.string.key_equalizer), getApplicationContext());
        if (fxBandGains == null || fxBandGains.length < 10)
            fxBandGains = new float[10];
    }

    // Play file
	public void play(int pos) {
        if (tracks == null)
            return;

        if (pos < 0)
            return;

        if (playerInterface == null)
            return;

        startUpdateProgress();
        playerInterface.onProgressChanged(pos, 0, 0);

		// Play File
        String path = "";
        String url = "";
        if (tracks != null && tracks.size() > pos) {
            currentTrack = tracks.get(pos);
            path = currentTrack.getPath();
            url = currentTrack.getUrl();
        }

        if ((path == null || path.equals("")) && (url == null || url.equals(""))) {
            onPlayError("empty");
            return;
        }

        int newStream = 0;
        if (path != null && !path.equals(""))
            newStream = BASS.BASS_StreamCreateFile(path, 0, 0, 0);
        else if (url != null && !url.equals(""))
            newStream = BASS.BASS_StreamCreateURL(url.replace("https://", "http://"), 0, 0, null, 0);

        synchronized (this) {
            BASS.BASS_StreamFree(stream);
            stream = newStream;
        }

        if (stream == 0) {
            onPlayError(path + url);

            // Stop Foreground
            stopForeground(true);

            return;
        }

        setupFx();

		// Play File
        BASS.BASS_ChannelSetSync(stream, BASS.BASS_SYNC_END, 0, EndSync, 0);
        BASS.BASS_ChannelPlay(stream, false);

        this.position = pos;

        // Update Properties
		this.duration = BASS.BASS_ChannelBytes2Seconds(stream, BASS.BASS_ChannelGetLength(stream, BASS.BASS_POS_BYTE));
		this.progress = 0.0;

		// Notify Activity
		if(playerInterface != null) {
			playerInterface.onFileLoaded(currentTrack, this.duration,
                    currentTrack.getArtist(),
                    currentTrack.getTitle(),
                    position,
                    currentTrack.getAlbumId());
			playerInterface.onProgressChanged(position, 0, 0);
            onUpdatePlayPause();
		}

        updateWidgets();

		// Start foreground
        fireNotification();

        //Remote include_player_controller
        if (Build.VERSION.SDK_INT >= 14 && remoteControlClient != null) {
            updateRemoteControl();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void updateRemoteControlState(int state) {
        remoteControlClient.setPlaybackState(state);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void updateRemoteControl() {
        updateRemoteControlState(RemoteControlClient.PLAYSTATE_PLAYING);
        remoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                        );

        // Update the remote controls
        Bitmap bitmap = MediaUtils.getArtworkQuick(this, currentTrack, screenWidth / 2, screenWidth / 2);
        /*
        int redTop = 0, greenTop=0, blueTop = 0,pixelsTop = 0;
        int redBtm = 0, greenBtm=0, blueBtm = 0,pixelsBtm = 0;
        int colorTop = 0, colorBtm = 0;

        if (bitmap != null) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            for (int i = 0; i < w; i++) {
                try {
                    colorTop = bitmap.getPixel(i, 0);
                    redTop += Color.red(colorTop);
                    greenTop += Color.green(colorTop);
                    blueTop += Color.blue(colorTop);
                    pixelsTop += 1;

                    colorBtm = bitmap.getPixel(i, h-1);
                    redBtm += Color.red(colorBtm);
                    greenBtm += Color.green(colorBtm);
                    blueBtm += Color.blue(colorBtm);
                    pixelsBtm += 1;
                }
                catch (Exception e) {}
            }
            if (pixelsTop > 0 && pixelsBtm > 0) {
                colorTop = Color.rgb(redTop / pixelsTop, greenTop / pixelsTop, blueTop / pixelsTop); //EDE7E9
                colorBtm = Color.rgb(redBtm / pixelsBtm, greenBtm / pixelsBtm, blueBtm / pixelsBtm);
                Shader shader = new LinearGradient(w/2,0,w/2,h,colorTop,colorBtm,Shader.TileMode.CLAMP);
                Bitmap bitmapBgr = Bitmap.createBitmap(w, screenHeight/2, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmapBgr);
                Paint paint = new Paint();
                paint.setShader(shader);
                canvas.drawRect(0, 0, w, screenHeight/2, paint);
                canvas.drawBitmap(bitmap,0,(screenHeight/2-screenWidth/2)/2,null);
                bitmap.recycle();
                bitmap = bitmapBgr;
            }
        }
        else {
            //create random color
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            Random rnd = new Random();
            bitmap.eraseColor(Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)));
        }
        */
        remoteControlClient.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, currentTrack.getArtist())
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, currentTrack.getAlbum())
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, currentTrack.getTitle())
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,currentTrack.getDuration())
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap)
                .apply();
    }

    private void fireNotification() {
        if (notification == null)
            notification = NotificationUtils.getNotification(this, pendIntent, (tracks != null && tracks.size() > position) ? tracks.get(position) : null, isPlaying());
        else
            notification.contentView = NotificationUtils.getNotificationViews(currentTrack, this, isPlaying());

        startForeground(1, notification);

        if (isPaused() || !isPlaying())
            stopForeground(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (PLAY.equals(action)) {
            if (mediabtnLastEventTime == 0) {
                mediabtnLastEventTime = System.currentTimeMillis();
                mediabtnPressCounter ++;
                Handler  mediaHandler = new Handler();
                Runnable mediaRunnable = new Runnable() {

                    @Override
                    public void run() {

                        if (mediabtnPressCounter<=1) {
                            if (isPaused())
                                playFromPause();
                            else
                                pause();
                        }
                        else {
                            playNext();
                        }
                        updateWidgets();
                        fireNotification();
                        mediabtnPressCounter = 0;
                        mediabtnLastEventTime = 0;
                    }
                };
                mediaHandler.postDelayed(mediaRunnable, 500);
            }
            else {
                if ((System.currentTimeMillis() - mediabtnLastEventTime) < 500) {
                    //в течение секунды жмаки идут
                    mediabtnLastEventTime = System.currentTimeMillis();
                    mediabtnPressCounter ++;
                }
                else {
                    //обнуляем
                    mediabtnLastEventTime = 0;
                    mediabtnPressCounter = 0;
                }
            }
        } else if (NEXT.equals(action)){
            playNext();
        } else if (PREV.equals(action)){
            playPrev();
        } else if (VOLUME_UP.equals(action)){
            volumeUp();
        } else if(VOLUME_DOWN.equals(action)){
            volumeDown();
        }

        //updateWidgets();
        //fireNotification();

        return START_NOT_STICKY;
    }

    public void playNext(){
        if (tracks == null)
            return;

        if (tracks.size()>(position+1))
            play(position + 1);
        else if (tracks.size() > 0) {
            stop();
        }
    }

    public void playPrev(){
        if (tracks == null)
            return;

        if ((position - 1) >= 0) {
            position = position - 1;
        }
        else {
            return;
        }

        play(position);
    }

    public void onPlayError(String e) {
        // Update Properties
        this.duration = 0.0;
        this.progress = 0.0;

        // Notify playerInterface
        if(playerInterface != null) {
            playerInterface.onFileLoaded(tracks.get(position), this.duration, "", "", 0,0);
            playerInterface.onProgressChanged(position, 0, 0);
            onUpdatePlayPause();
        }

        stopUpdateProgress();

        //skip 1st n errors on play
        if (errorCount < 3) {
            errorCount++;
            playNext();
        } else {
            stop();
        }
    }

    private void onUpdatePlayPause() {
        if (playerInterface != null)
            playerInterface.onUpdatePlayPause();
        fireNotification();
    }

    public void updateWidgets() {
        Intent smallWidgetIntent = new Intent(getApplicationContext(), SmallWidgetProvider.class);
        smallWidgetIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int smallWidgetIds[] = AppWidgetManager.getInstance(getApplicationContext()).getAppWidgetIds(new ComponentName(getApplicationContext(), SmallWidgetProvider.class));
        smallWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, smallWidgetIds);
        getApplicationContext().sendBroadcast(smallWidgetIntent);
    }
	
	// Seek to position
	public void seekTo(int seconds) {
		BASS.BASS_ChannelSetPosition(stream, BASS.BASS_ChannelSeconds2Bytes(stream, seconds), BASS.BASS_POS_BYTE);
	}

    public void pause() {
        BASS.BASS_ChannelPause(stream);
        stopForeground(false);
        stopUpdateProgress();
        saveEqualizerValues();

        // Notify playerInterface
        onUpdatePlayPause();

        // Tell any remote controls that our playback state is 'paused'.
        if (remoteControlClient != null) {
            updateRemoteControlState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }

    public void stop() {
        BASS.BASS_ChannelStop(stream);
        stopUpdateProgress();
        saveEqualizerValues();

        onUpdatePlayPause();

        if (remoteControlClient != null) {
            updateRemoteControlState(RemoteControlClient.PLAYSTATE_STOPPED);
        }

    }

    public void stopUpdateProgress() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    public void startUpdateProgress() {
        //start update progress
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public boolean isPlaying() {
        if (!(BASS.BASS_ACTIVE_PLAYING == BASS.BASS_ChannelIsActive(stream))) {
            //stopForeground(false);
            stopUpdateProgress();
        }
        if (BASS.BASS_ACTIVE_PLAYING == BASS.BASS_ChannelIsActive(stream)) {
            startUpdateProgress();
        }
        return BASS.BASS_ACTIVE_PLAYING ==BASS.BASS_ChannelIsActive(stream);
    }

    public boolean isPaused() {
        return BASS.BASS_ACTIVE_PAUSED == BASS.BASS_ChannelIsActive(stream);
    }

    public void playFromPause() {
        BASS.BASS_ChannelPlay(stream, false);
        startUpdateProgress();

        // Notify playerInterface
        onUpdatePlayPause();

        if (remoteControlClient != null)
            updateRemoteControlState(RemoteControlClient.PLAYSTATE_PLAYING);

        updateWidgets();
        fireNotification();
    }

    public void volumeUp() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null)
            return;

        int currVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currVolume = currVolume +(maxVolume/10);
        if (currVolume > maxVolume)
            currVolume = maxVolume;

        am.setStreamVolume(AudioManager.STREAM_MUSIC, currVolume, AudioManager.FLAG_SHOW_UI);
    }

    public void volumeDown() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null)
            return;

        int currVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currVolume = currVolume - (maxVolume/10);
        if (currVolume < 0)
            currVolume = 0;

        am.setStreamVolume(AudioManager.STREAM_MUSIC, currVolume, AudioManager.FLAG_SHOW_UI);
    }

    private void setupFx()
    {
        loadEqualizerValues();

        fxBandHandles = new int[10];
        fxBandHandles[0] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[1] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[2] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[3] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[4] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[5] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[6] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[7] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[8] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);
        fxBandHandles[9] = BASS.BASS_ChannelSetFX(stream, BASS.BASS_FX_DX8_PARAMEQ, 0);

        BASS.BASS_DX8_PARAMEQ p = new BASS.BASS_DX8_PARAMEQ();

        p.fCenter = 32;
        p.fBandwidth = 3f;
        p.fGain = fxBandGains[0];
        BASS.BASS_FXSetParameters(fxBandHandles[0], p);

        p.fCenter=64;
        p.fBandwidth = 4f;
        p.fGain = fxBandGains[1];
        BASS.BASS_FXSetParameters(fxBandHandles[1], p);

        p.fCenter=125;
        p.fBandwidth = 5f;
        p.fGain = fxBandGains[2];
        BASS.BASS_FXSetParameters(fxBandHandles[2], p);

        p.fCenter=250;
        p.fBandwidth = 6f;
        p.fGain = fxBandGains[3];
        BASS.BASS_FXSetParameters(fxBandHandles[3], p);

        p.fCenter=500;
        p.fBandwidth = 8f;
        p.fGain = fxBandGains[4];
        BASS.BASS_FXSetParameters(fxBandHandles[4], p);

        p.fCenter=1000;
        p.fBandwidth = 10f;
        p.fGain = fxBandGains[5];
        BASS.BASS_FXSetParameters(fxBandHandles[5], p);

        p.fCenter=2000;
        p.fBandwidth = 12f;
        p.fGain = fxBandGains[6];
        BASS.BASS_FXSetParameters(fxBandHandles[6], p);

        p.fCenter=4000;
        p.fBandwidth = 12f;
        p.fGain = fxBandGains[7];
        BASS.BASS_FXSetParameters(fxBandHandles[7], p);

        p.fCenter=8000;
        p.fBandwidth = 18f;
        p.fGain = fxBandGains[8];
        BASS.BASS_FXSetParameters(fxBandHandles[8], p);

        p.fCenter=16000;
        p.fBandwidth = 36f;
        p.fGain = fxBandGains[9];
        BASS.BASS_FXSetParameters(fxBandHandles[9], p);
    }

    public void updateFXBand(int n, float progress) {
        if (n < 0 || n >= fxBandGains.length)
            return;

        fxBandGains[n] = progress - 15;
        if (fxBandHandles != null) {
            BASS.BASS_DX8_PARAMEQ p = new BASS.BASS_DX8_PARAMEQ();
            BASS.BASS_FXGetParameters(fxBandHandles[n], p);
            p.fGain = fxBandGains[n];
            BASS.BASS_FXSetParameters(fxBandHandles[n], p);
        }
        saveEqualizerValues();
    }

    public float getFXBandValue(int n)
    {
        if (fxBandGains == null)
            loadEqualizerValues();

        return fxBandGains[n] + 15;
    }

    public float[] getFXBandValues() {
        if (fxBandGains == null)
            loadEqualizerValues();

        return fxBandGains;
    }


    public class UpdateAllFiles extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                new MakePlaylistFS(getApplicationContext(),true).getArrTracks();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent==null)
                return;

            if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
                //on power disconnect scan for new files
                new UpdateAllFiles().execute(new ArrayList<String>());
            }
            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){

                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am == null)
                    return;

                int currVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                if (currVolume == maxVolume && (System.currentTimeMillis()- startVolumeUpFlag) > 2000) {
                    if (firstVolumeUpFlag) {
                        firstVolumeUpFlag = false;
                        if (isPlaying()) {
                            startVolumeUpFlag = System.currentTimeMillis();
                            playNext();
                        }
                    }
                    else {
                        firstVolumeUpFlag = true;
                    }

                }
                else {
                    startVolumeUpFlag = System.currentTimeMillis();
                }
            }
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state){
                    case 0:
                        if (isPlaying()) {
                            isUnpluggedFlag = true;
                            pause();

                        }
                        break;
                    case 1:
                        if (isUnpluggedFlag && isPaused()) {
                            isUnpluggedFlag = false;
                            playFromPause();

                        }

                        break;
                    default:

                        break;
                }
            }

        }
    }
}
