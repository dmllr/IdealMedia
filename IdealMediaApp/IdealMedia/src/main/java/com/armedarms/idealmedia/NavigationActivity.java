package com.armedarms.idealmedia;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidquery.AQuery;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.FragmentHistoryItem;
import com.armedarms.idealmedia.domain.IPlayerController;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.fragments.AlbumsFragment;
import com.armedarms.idealmedia.fragments.ArtistsFragment;
import com.armedarms.idealmedia.fragments.BaseFragment;
import com.armedarms.idealmedia.fragments.BasePlayingFragment;
import com.armedarms.idealmedia.fragments.EqualizerFragment;
import com.armedarms.idealmedia.fragments.FoldersFragment;
import com.armedarms.idealmedia.fragments.IHasColor;
import com.armedarms.idealmedia.fragments.NavigationDrawerFragment;
import com.armedarms.idealmedia.fragments.PlayerFragment;
import com.armedarms.idealmedia.fragments.PlaylistFragment;
import com.armedarms.idealmedia.fragments.PlaylistsFragment;
import com.armedarms.idealmedia.fragments.SearchResultsFragment;
import com.armedarms.idealmedia.fragments.SettingsDrawerFragment;
import com.armedarms.idealmedia.tasks.TaskGetArtwork;
import com.armedarms.idealmedia.tasks.TaskGetPlaylistFilesystem;
import com.armedarms.idealmedia.tools.SwipeRefreshLayout;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.MediaUtils;
import com.armedarms.idealmedia.utils.ResUtils;
import com.armedarms.idealmedia.utils.iab.IabHelper;
import com.armedarms.idealmedia.utils.iab.IabResult;
import com.armedarms.idealmedia.utils.iab.Inventory;
import com.armedarms.idealmedia.utils.iab.Purchase;
import com.r0adkll.postoffice.PostOffice;
import com.r0adkll.postoffice.model.Design;
import com.r0adkll.postoffice.styles.ListStyle;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

import com.armedarms.idealmedia.tools.MusicPlayerView;

import org.jetbrains.annotations.NotNull;

import libs.SlidingUpPanelLayout;

import static com.armedarms.idealmedia.R.id.*;


public class NavigationActivity
        extends
            androidx.appcompat.app.AppCompatActivity
        implements
            NavigationDrawerFragment.NavigationDrawerCallbacks,
            SettingsDrawerFragment.OnSettingsInteractionListener,
            IPlayerController
{

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private SettingsDrawerFragment mSettingsDrawerFragment;

    private PlayerFragment fragmentPlayer = new PlayerFragment();
    private PlaylistsFragment fragmentPlaylists = new PlaylistsFragment();
    private PlaylistFragment fragmentPlaylist = new PlaylistFragment();
    private FoldersFragment fragmentFolders = new FoldersFragment();
    private AlbumsFragment fragmentAlbums = new AlbumsFragment();
    private ArtistsFragment fragmentArtists = new ArtistsFragment();
    private SearchResultsFragment fragmentSearchResults = new SearchResultsFragment();
    private BasePlayingFragment playingFragment;
    private Stack<FragmentHistoryItem> fragmentHistory = new Stack<FragmentHistoryItem>();

    private AQuery aq;
    public static Random randomGenerator;

    ArrayList<Playlist> playlists = new ArrayList<Playlist>();
    private Playlist workingPlaylist;
    private Playlist historyPlaylist;

    private SwipeRefreshLayout swipeRefreshLayout;
    private MusicPlayerView musicPlayerView;
    private TextView textPlayerControllerTrackTitle;
    private TextView textPlayerControllerTrackArtist;
    private View layoutControls;
    private View layoutTexts;

    private IabHelper iabHelper;
    private Inventory iabInventory;

    private PlayerService mBoundService = null;

    // Bass Service Connection
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((PlayerService.BassServiceBinder)service).getService();
            onPlayerServiceConnected();

            Uri data = getIntent().getData();
            if(data != null) {
                getIntent().setData(null);
                try {
                    openFile(data);
                } catch (Exception ignored) {
                }
            } else
                putPlayerFragment();
        }

        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }

    };

    public PlayerService getService() {
        return mBoundService;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getService() != null)
            getService().setActivityStarted(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (getService() != null)
            getService().setActivityStarted(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        MediaUtils.nomedia();
        attemptLikeDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme();

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_navigation);

        if (randomGenerator == null){
            Time now = new Time();
            randomGenerator = new Random(now.toMillis(true));
        }

        aq = new AQuery(this);

        mNavigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager().findFragmentById(left_navigation_drawer);
        mSettingsDrawerFragment = (SettingsDrawerFragment) getSupportFragmentManager().findFragmentById(right_navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                left_navigation_drawer,
                (DrawerLayout) findViewById(drawer_layout));

        // Set up swipe-to-refresh.
        swipeRefreshLayout = findViewById(swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update();
            }
        });
        swipeRefreshLayout.setColorSchemeColors(ResUtils.color(this, R.attr.colorPlaylists), ResUtils.color(this, R.attr.colorFolders), ResUtils.color(this, R.attr.colorPreferences));

        musicPlayerView = findViewById(R.id.mpv);
        musicPlayerView.setAutoProgress(false);
        musicPlayerView.setInteractionListener(new MusicPlayerView.OnInteractionListener() {
            @Override
            public void onSeek(int seconds) {
                getService().seekTo(seconds);
            }
        });

        layoutControls = findViewById(R.id.layoutControls);
        layoutTexts = findViewById(R.id.layoutTexsts);
        SlidingUpPanelLayout slideUpPanel = findViewById(R.id.sliding_layout);
        slideUpPanel.addPanelSlideListener(new SlidePanelSlideListener(this));

        textPlayerControllerTrackTitle = findViewById(R.id.player_controller_track_title);
        textPlayerControllerTrackArtist = findViewById(R.id.player_controller_track_artist);

        if (!Settings.PREMIUM)
            iabStartSetup();

        loadPlaylistsAsync();

        bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        // Unbind Service
        unbindService(mConnection);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fragmentHistory.size() > 1) {
            fragmentHistory.pop();
            FragmentHistoryItem fhi = fragmentHistory.peek();
            putFragment(fhi.fragment, false);

            getSupportActionBar().setTitle(getFragmentTitle(fhi.fragment));
        } else
            super.onBackPressed();
    }

    private void iabStartSetup() {
        iabHelper = new IabHelper(this, Settings.PLAY_PUBLIC_KEY);
        iabHelper.enableDebugLogging(BuildConfig.DEBUG);
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess())
                    return;
                if (iabHelper == null)
                    return;

                queryIabInventoryAsync();
            }
        });
    }

    private void attemptLikeDialog() {
        int start = PreferenceManager.getDefaultSharedPreferences(this).getInt("start", 0);

        if (!hasPremiumPurchase() && start == 20) {
            PostOffice
                    .newMail(this)
                    .setDesign(Design.MATERIAL_LIGHT)
                    .setTitle(R.string.do_you_like_me)
                    .setMessage(R.string.settings_premium)
                    .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            purchasePremium();
                        }
                    })
                    .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .build()
                    .show(getSupportFragmentManager());
        }

        start++;
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("start", start).apply();
    }

    private void queryIabInventoryAsync() {
        iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                iabInventory = inv;

                PreferenceManager.getDefaultSharedPreferences(NavigationActivity.this).edit().putBoolean(getString(R.string.hasPremium), iabInventory.hasPurchase(Settings.SKU_PREMIUM)).apply();

                if (mSettingsDrawerFragment != null)
                    mSettingsDrawerFragment.update();
                if (mNavigationDrawerFragment != null)
                    mNavigationDrawerFragment.update();
            }
        });
    }

    private void openFile(Uri data) {
        Track track = Track.fromUri(data);
        if (track == null) {
            putPlayerFragment();
            return;
        }

        Playlist playlist = new Playlist();

        playlist.setTitle(track.getDisplay());
        playlist.getTracks().add(track);

        openPlaylist(playlist);

        getService().setTracks(playlist.getTracks(), UUID.randomUUID().toString());
        getService().play(0);
    }

    private void update() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

        if (fragment == fragmentPlayer)
            fragmentPlayer.update();
        if (fragment == fragmentFolders)
            fragmentFolders.update(true);
        else if (fragment == fragmentArtists)
            fragmentArtists.update(true);
        else if (fragment == fragmentAlbums)
            fragmentAlbums.update(true);
        else if (fragment == fragmentSearchResults)
            fragmentSearchResults.update();
        else
            setRefreshing(false);
    }

    public void updateFolders(boolean refresh) {
        fragmentFolders.update(refresh);
    }

    @Override
    public void onNavigationDrawerItemSelected(NavigationDrawerFragment.MenuLine item, String param) {
        Fragment fragment = fragmentPlayer;

        switch (item) {
            case PLAYLISTS:
                fragment = fragmentPlaylists;
                break;
            case PLAYLISTS_ALL_MUSIC:
                fragment = fragmentPlaylist;
                fragmentPlaylist.setAllMusicPlaylistAsync(this);
                setRefreshing(true);
                break;
            case PLAYLISTS_NOW_PLAYING:
                fragment = fragmentPlayer;
                break;
            case PLAYLISTS_PLAYBACK_HISTORY:
                fragment = fragmentPlaylist;
                fragmentPlaylist.setPlaylist(historyPlaylist);
                break;
            case SEARCH:
                fragmentSearchResults.setQuery(param);
                fragment = fragmentSearchResults;
                break;
            case DEVICE:
                fragment = fragmentFolders;
                break;
            case DEVICE_TRACKS:
                fragment = fragmentFolders;
                break;
            case DEVICE_ARTISTS:
                fragment = fragmentArtists;
                break;
            case DEVICE_ALBUMS:
                fragment = fragmentAlbums;
                break;
        }

        putFragment(fragment);

        mNavigationDrawerFragment.closeDrawer();
    }

    private void setTheme() {
        int themeIndex = PreferenceManager.getDefaultSharedPreferences(this).getInt(getString(R.string.key_theme), 0);
        int theme = R.style.Theme_IdealMedia_Colored;

        switch (themeIndex) {
            case 0:
                theme = R.style.Theme_IdealMedia_Colored;
                break;
            case 1:
                theme = R.style.Theme_IdealMedia_Colored_Dark;
                break;
            case 2:
                theme = R.style.Theme_IdealMedia_Dark;
                break;
        }

        setTheme(theme);
    }

    private void colorize(final int color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getActionBar() != null)
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));

                View view = findViewById(R.id.drawer_layout);
                if (view != null)
                    view.setBackgroundColor(color);
                view = findViewById(R.id.player_controller);
                if (view != null)
                    view.setBackgroundColor(color - 0x00111111);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (iabHelper == null)
            return;

        if (!iabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onPlayerServiceConnected() {
        mBoundService.setController(this);

        /*
        playingSeekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    final int prgr = progress;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mBoundService.seekTo(prgr);
                        }
                    }).start();

                }
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });
        */

        aq.id(R.id.btnFf).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mBoundService != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mBoundService.playNext();
                            updatePlayPause();
                        }
                    }).start();
                }
            }
        });
        aq.id(R.id.btnRew).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mBoundService.playPrev();
                            updatePlayPause();
                        }
                    }).start();
                }
            }
        });
        aq.id(R.id.btnSfl).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean mode = !mBoundService.isShuffle();
                            setShuffleMode(mode);
                            if (playingFragment != null) {
                                playingFragment.updateSnapshot();
                                playingFragment.synchronizeTrackList();
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(NavigationActivity.this, getString(R.string.shuffle_is) + (mBoundService.isShuffle() ? " on" : " off"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();

                }
            }
        });
        aq.id(R.id.btnRept1).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService!=null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mBoundService.setRepeat(!mBoundService.isRepeat());
                            updatePlayPause();
                        }
                    }).start();
                }
            }
        });
        aq.id(R.id.btnPlay).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBoundService != null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (mBoundService.isPlaying()) {
                                mBoundService.pause();
                            }
                            else {
                                if (mBoundService.isPaused()) {
                                    mBoundService.playFromPause();
                                    mBoundService.startVolumeUpFlag = System.currentTimeMillis();
                                }
                                else {
                                    if (playingFragment != null) {
                                        int pos = playingFragment.getSelectedPosition() > 0 ? playingFragment.getSelectedPosition() : 0;
                                        if (playingFragment.getAdapter().getItemCount() > pos) {
                                            mBoundService.play(pos);
                                            mBoundService.startVolumeUpFlag = System.currentTimeMillis();
                                        }
                                    }
                                }
                            }
                            updatePlayPause();
                        }
                    }).start();
                }

            }
        });

        //fragmentPlayer.update(); // because onStart does update()
    }

    public void updatePlayPause() {

        if (mBoundService!=null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBoundService.setActivityStarted(true);

                    if (mBoundService.isPlaying()) {
                        aq.id(R.id.btnPlay).image(R.drawable.selector_pause_button);
                    } else {
                        aq.id(R.id.btnPlay).image(R.drawable.selector_play_button);
                    }
                    if (mBoundService.isShuffle()) {
                        aq.id(R.id.btnSfl).image(R.drawable.base_shuffle_button_on);
                    } else {
                        aq.id(R.id.btnSfl).image(R.drawable.base_shuffle_button_off);
                    }
                    if (mBoundService.isRepeat()) {
                        aq.id(R.id.btnRept1).image(R.drawable.base_repeat_button_on);
                    } else {
                        aq.id(R.id.btnRept1).image(R.drawable.base_repeat_button_off);
                    }

                    if (mBoundService.isPlaying())
                        musicPlayerView.start();
                    else
                        musicPlayerView.stop();
                }
            });

        }

    }

    public void setRefreshing(boolean refreshing) {
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return swipeRefreshLayout.isRefreshing();
    }

    public void exhibit(ArrayList<Track> tracks) {
        Playlist p = new Playlist(tracks);
        p.setTitle(getString(R.string.title_selected_tracks));

        openPlaylist(p);
    }

    public void restoreActionBar() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(getFragmentTitle(fragment));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.navigation, menu);
            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPluginsLoaded(String plugins) {

    }

    @Override
    public void onFileLoaded(final Track track, double duration, final String artist, final String title, final int position, int albumId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (track != null) {
                    textPlayerControllerTrackTitle.setText(track.getTitle());
                    textPlayerControllerTrackArtist.setText(track.getArtist());

                    addToPlaybackHistory(track);
                }

                Bitmap artwork = null;

                if (!isFinishing())
                    artwork = MediaUtils.getArtworkQuick(NavigationActivity.this, track, 300, 300);

                if (artwork != null) {
                    musicPlayerView.setCoverBitmap(artwork);
                    //imageAlbum.setScaleType(ImageView.ScaleType.FIT_XY);
                    //imageAlbum.setImageBitmap(MediaUtils.getBlurredEdgesBitmap(artwork, findViewById(R.id.imageAlbum).getWidth(), findViewById(R.id.imageAlbum).getHeight()));
                } else {
                    musicPlayerView.setCoverDrawable(R.drawable.ic_default_album);
                    //imageAlbum.setScaleType(ImageView.ScaleType.CENTER);
                    //imageAlbum.setImageDrawable(getResources().getDrawable(R.drawable.ic_default_album));

                    if (track != null) {
                        new TaskGetArtwork(NavigationActivity.this).withListener(new TaskGetArtwork.IGetArtworkListener() {
                            @Override
                            public void onNewArtwork() {
                                final Bitmap newart = MediaUtils.getArtworkQuick(NavigationActivity.this, track, 300, 300);
                                if (newart != null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            musicPlayerView.setCoverBitmap(newart);
                                            //imageAlbum.setScaleType(ImageView.ScaleType.FIT_XY);
                                            //imageAlbum.setImageBitmap(MediaUtils.getBlurredEdgesBitmap(newart, findViewById(R.id.imageAlbum).getWidth(), findViewById(R.id.imageAlbum).getHeight()));
                                        }
                                    });
                                }
                            }
                        }).execute(track);
                    }
                }
            }
        });

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

        if (fragment instanceof BasePlayingFragment) {
            final BasePlayingFragment playingFragment = (BasePlayingFragment)fragment;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    playingFragment.getAdapter().notifyDataSetChanged();
                    playingFragment.scrollToTrack(position);
                }
            });
        }
    }

    public void addToPlaybackHistory(Track track) {
        if (historyPlaylist != null) {
            ArrayList<Track> hptracks = historyPlaylist.getTracks();

            if (hptracks.size() == 0)
                hptracks.add(0, track);
            else {
                Track first = hptracks.get(0);
                if (!first.getDisplay().equals(track.getDisplay()))
                    hptracks.add(0, track);
            }

            if (hptracks.size() > 100)
                hptracks.remove(100);

            savePlaylist(historyPlaylist);
        }
    }

    @Override
    public void onProgressChanged(int position, final double progress, final double duration) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                musicPlayerView.setProgress((int)progress);
                musicPlayerView.setMax((int)duration);
            }
        });

        if (fragment instanceof BasePlayingFragment) {
            BasePlayingFragment bpf =(BasePlayingFragment) fragment;
            if (bpf.isSnapshotMatch()) {
                RecyclerView list = bpf.getListView();
                if (list.getLayoutManager() instanceof LinearLayoutManager) {
                    int fvp = ((LinearLayoutManager)list.getLayoutManager()).findFirstVisibleItemPosition();
                    int lvp = ((LinearLayoutManager)list.getLayoutManager()).findLastVisibleItemPosition();

                    int vposition = bpf.getAdapter().listToVisiblePosition(position);

                    if (vposition >= fvp && vposition <= lvp) {
                        View cell = list.getChildAt(vposition - fvp);
                        if (cell != null) {
                            final PlayerAdapter.TrackViewHolder viewHolder = (PlayerAdapter.TrackViewHolder) cell.getTag();
                            if (viewHolder != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean isIndeterminate = progress == 0;
                                        viewHolder.index.setText((int) (100 * progress / duration) + "%");
                                        viewHolder.index.setProgress(isIndeterminate ? 1 : (int)(100 * progress / duration));
                                        viewHolder.index.setIndeterminateProgressMode(isIndeterminate);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUpdatePlayPause() {
        updatePlayPause();
    }

    private void setShuffleMode(boolean mode){
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("Shuffle", mode).apply();
        mBoundService.setShuffle(mode);
        if (playingFragment != null)
            playingFragment.shuffleItems();
        updatePlayPause();
    }

    private void putFragment(Fragment fragment) {
        putFragment(fragment, true);
    }

    @Override
    public void onAttachFragment(@NotNull Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BaseFragment) {
            if (fragment instanceof IHasColor)
                colorize(((IHasColor) fragment).getColor());
            else
                colorize(ResUtils.color(this, R.attr.colorDefaultBg));
        }
    }

    private void putFragment(Fragment fragment, boolean toHistory) {
        swipeRefreshLayout.clearCache();

        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        } catch (Exception ignored) {
            return;
        }

        if (fragment instanceof BasePlayingFragment)
            playingFragment = (BasePlayingFragment)fragment;

        if (toHistory)
            fragmentHistory.push(FragmentHistoryItem.get(fragment));
    }

    private String getFragmentTitle(Fragment fragment) {
        int resid = R.string.title_ideal;
        String text = "";

        try {
            if (fragment == null)
                resid = R.string.title_ideal;

            if (fragment == fragmentAlbums)
                resid = R.string.title_albums;
            else if (fragment == fragmentArtists)
                resid = R.string.title_artists;
            else if (fragment == fragmentPlaylists)
                resid = R.string.title_playlists;
            else if (fragment == fragmentPlaylist)
                text = fragmentPlaylist.getPlaylist().getTitle();
            else if (fragment == fragmentSearchResults) {
                if (!"".equals(fragmentSearchResults.getSearchQuery()))
                    text = "\"" + fragmentSearchResults.getSearchQuery() + "\"";
                resid = R.string.title_vk_search;
            } else if (fragment == fragmentPlayer)
                resid = R.string.title_now_playing;
            else if (fragment == fragmentFolders)
                resid = R.string.title_folders;
        } catch (Exception ignored) { }

        if ("".equals(text))
            return getString(resid);
        else
            return text;
    }

    public void putPlayerFragment() {
        putFragment(fragmentPlayer);
    }

    public void putFoldersFragment() {
        putFragment(fragmentFolders);
    }

    public void placeFoldersFragment() {
        if (fragmentHistory.size() > 0)
            fragmentHistory.pop();
        putFragment(fragmentFolders);
    }

    public ArrayList<Playlist> getPlaylists() {
        return playlists;
    }

    public Playlist getWorkingPlaylist() {
        return workingPlaylist;
    }

    private void loadPlaylists() {
        historyPlaylist = (Playlist)FileUtils.read(getString(R.string.key_playing_history), getApplicationContext());
        if (historyPlaylist == null) {
            historyPlaylist = new Playlist();
            historyPlaylist.id = UUID.fromString("bd6a2390-98d7-40a8-852d-7384244bd6d1");
            historyPlaylist.setTitle(getString(R.string.playback_history));
        }

        if (playlists == null)
            playlists = new ArrayList<Playlist>();

        playlists.clear();

        UUID[] ids = (UUID[]) FileUtils.read(getString(R.string.key_playlists), getApplicationContext());

        if (ids == null)
            return;

        for (UUID id : ids) {
            String playlistFileName = String.format("playlist_%s", id.toString());
            try {
                Playlist p = (Playlist) FileUtils.read(playlistFileName, getApplicationContext());
                if (p != null)
                    playlists.add(p);
            } catch (Exception e) {
                new File(playlistFileName).delete();
            }
        }
    }

    private void loadPlaylistsAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                loadPlaylists();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (fragmentPlaylists.isAdded())
                    fragmentPlaylists.invalidate();
            }
        }.execute();
    }


    @Override
    public void onMediaPathChanged(String mediaPath) {
        boolean fullScan = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.key_media_method_full), false);
        new TaskGetPlaylistFilesystem(this, fullScan ? TaskGetPlaylistFilesystem.TYPE_FS : TaskGetPlaylistFilesystem.TYPE_MS, true, new TaskGetPlaylistFilesystem.OnTaskGetPlaylist() {
            @Override
            public void OnTaskResult(ArrayList<Playlist> result) {
                updateFolders(false);
            }
        }).execute();
    }

    @Override
    public void onMediaMethodChanged(boolean isFullScan) {
        //
    }

    @Override
    public void onEqualizerPreference() {
        putFragment(new EqualizerFragment());
        mNavigationDrawerFragment.closeDrawer();
    }

    @Override
    public void switchTheme(int themeIndex) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(getString(R.string.key_theme), themeIndex).apply();

        finish();

        Intent intent = new Intent(this, NavigationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void purchasePremium() {
        mNavigationDrawerFragment.closeDrawer();

        if (iabAvailable()) {
            if (!iabInventory.hasPurchase(Settings.SKU_PREMIUM)) {
                iabHelper.launchPurchaseFlow(this, Settings.SKU_PREMIUM, 10001, mPurchaseFinishedListener);
            } else {
                if (BuildConfig.DEBUG) {
                    iabHelper.consumeAsync(iabInventory.getPurchase(Settings.SKU_PREMIUM), new IabHelper.OnConsumeFinishedListener() {
                        @Override
                        public void onConsumeFinished(Purchase purchase, IabResult result) {
                            queryIabInventoryAsync();
                            Toast.makeText(NavigationActivity.this, "Consume finished\n" + result.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        } else {
            Toast.makeText(this, R.string.toast_no_possible_purchase, Toast.LENGTH_LONG).show();
            iabStartSetup();
        }
    }

    public boolean iabAvailable() {
        return iabHelper != null && iabInventory != null;
    }

    public void openPlaylist(Playlist playlist) {
        fragmentPlaylist.setPlaylist(playlist);
        putFragment(fragmentPlaylist);
    }

    public void addToQuickPlaylist(final Track track, final boolean save, final boolean toast) {
        if (getIsWorkingPlaylistSet())
            addToPlaylist(track, workingPlaylist, save, toast);
        else {
            setWorkingPlaylistDialog(new OnWorkingPlaylistSetListener() {
                @Override
                public void OnWorkingPlaylistSet() {
                    addToPlaylist(track, workingPlaylist, save, toast);
                }
            });
        }
    }

    public boolean getIsWorkingPlaylistSet() {
        return workingPlaylist != null;
    }

    public boolean hasPremiumPurchase() {
        return iabAvailable() && iabInventory.hasPurchase(Settings.SKU_PREMIUM); // || PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.hasPremium), false);
    }

    public interface OnWorkingPlaylistSetListener {
        void OnWorkingPlaylistSet();
    }

    public void setWorkingPlaylistDialog(final OnWorkingPlaylistSetListener listener) {
        String[] pltitles = new String[playlists.size()];

        if (pltitles.length == 0) {
            putFragment(fragmentPlaylists);
            return;
        }

        int i = 0;
        for (Playlist p : playlists) {
            if (p != null) {
                pltitles[i] = p.getTitle();
                i++;
            }
        }

        PostOffice.newSimpleListMail(this, getString(R.string.title_playlist_not_set), Design.MATERIAL_LIGHT, pltitles, new ListStyle.OnItemAcceptedListener<CharSequence>() {
            @Override
            public void onItemAccepted(CharSequence charSequence, int i) {
                setPlaylistAsWorking(playlists.get(i));
                listener.OnWorkingPlaylistSet();
            }
        }).show(getSupportFragmentManager());
    }

    private void addToPlaylist(Track track, Playlist playlist, boolean save, boolean toast) {
        playlist.getTracks().add(track);

        if (save)
            savePlaylist(playlist);

        if (toast)
            Toast.makeText(this, String.format(getString(R.string.added_to_playlist), playlist.getTitle()), Toast.LENGTH_SHORT).show();
    }

    public void setPlaylistAsWorking(Playlist playlist) {
        workingPlaylist = playlist;
    }

    public void savePlaylist(Playlist playlist) {
        FileUtils.write(String.format("playlist_%s", playlist.id.toString()), getApplicationContext(), playlist);
    }

    public void dropPlaylist(int position) {
        String fileName = String.format("playlist_%s", playlists.get(position).id.toString());
        new File(fileName).delete();

        playlists.remove(position);

        UUID[] ids = new UUID[playlists.size()];
        int i = 0;
        for (Playlist p : playlists) {
            if (p.id == null)
                p.id = UUID.randomUUID();

            ids[i] = p.id;

            i++;
        }

        FileUtils.write(getString(R.string.key_playlists), getApplicationContext(), ids);
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure())
                Toast.makeText(NavigationActivity.this, getString(R.string.toast_purchase_failed) + "\n" + result.getMessage(), Toast.LENGTH_LONG).show();
            queryIabInventoryAsync();
        }
    };

    public enum TrackerName {
        APP_TRACKER,
    }

    private class SlidePanelSlideListener implements com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener {

        private float density;
        private int originalControlsWidth = -1;
        private float multiplier;

        public SlidePanelSlideListener(Context context) {
            density = context.getResources().getDisplayMetrics().density;
        }

        boolean isStartFrame = true;

        @Override
        public void onPanelSlide(View view, float v) {

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) layoutControls.getLayoutParams();
            RelativeLayout.LayoutParams textsParams = (RelativeLayout.LayoutParams) layoutTexts.getLayoutParams();
            RelativeLayout.LayoutParams targetParams = (RelativeLayout.LayoutParams) musicPlayerView.getLayoutParams();

            v = (float)Math.sqrt(v);
            int minSize = Math.min(musicPlayerView.getHeight(), musicPlayerView.getWidth());

            if (originalControlsWidth == -1) {
                originalControlsWidth = params.width;
                multiplier = (float)musicPlayerView.getWidth() / layoutControls.getWidth() * 0.8f;
            }

            params.topMargin = (int)(
                    12 * density * (1 - v)
                    + v * targetParams.topMargin
                    + v * minSize / 2
                    - v * layoutControls.getHeight() / 2
            );
            params.rightMargin = (int)(
                    12 * density * (1 - v)
                    + v * targetParams.rightMargin
                    + v * minSize / 2
                    - v * layoutControls.getWidth() / 2
            );
            params.width = (Math.max(originalControlsWidth, (int) (v * multiplier * originalControlsWidth)) + 4) / 5 * 5;

            layoutControls.setLayoutParams(params);

            if (params.topMargin / density > 44) {
                textsParams.addRule(RelativeLayout.LEFT_OF, 0);
            } else {
                textsParams.addRule(RelativeLayout.LEFT_OF, layoutControls.getId());
            }

            layoutTexts.setLayoutParams(textsParams);
        }

        @Override
        public void onPanelStateChanged(View panel, com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState previousState, com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState newState) {

        }
    }
}
