package com.armedarms.idealmedia.fragments;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class BasePlayingFragment extends BaseFragment {
    protected RecyclerView listView;
    protected PlayerAdapter adapter;
    protected ArrayList<Track> items = new ArrayList<Track>();
    protected ArrayList<Track> sourceItems = new ArrayList<Track>();
    private int selectedPosition;
    private String playlistSnapshot;
    private boolean needUpdate;
    private boolean isCanShuffle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            playlistSnapshot = savedInstanceState.getString(activity.getString(R.string.snapshot), UUID.randomUUID().toString());
        if (playlistSnapshot == null)
            playlistSnapshot = UUID.randomUUID().toString();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(activity.getString(R.string.snapshot), playlistSnapshot);
    }

    @Override
    public void onStart() {
        super.onStart();

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
//                    if (listView.canScrollVertically(1))
//                        onLastListItemScrolling();
//                }
            }

        });

        /*
        listView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onClickFrontView(int i) {
                if (activity.getService().isSnapshotMismatch(playlistSnapshot))
                    activity.getService().setTracks(items, playlistSnapshot);

                selectedPosition = adapter.visibleToListPosition(i);

                if (selectedPosition < 0)
                    return;

                adapter.notifyDataSetChanged();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.getService().isShuffle()) {
                            activity.updatePlayPause();
                        }
                        activity.getService().startVolumeUpFlag = System.currentTimeMillis();
                        activity.getService().play(selectedPosition);

                        activity.updatePlayPause();

                        Tracker t = activity.getTracker(NavigationActivity.TrackerName.APP_TRACKER);
                        t.send(new HitBuilders.EventBuilder().setCategory("Track").setAction("play").build());
                    }
                }).start();
            }

            @Override
            public void onLastListItem() {
                onLastListItemScrolling();
            }
        });
        */
    }

    public NavigationActivity activity() {
        return activity;
    }

    public void onLastListItemScrolling() {

    }

    public void synchronizeTrackList(){
        if (activity.getService() != null) {
            activity.getService().setTracks(items, playlistSnapshot);

            int newPlayingTrackIndex = activity.getService().getPlayingPosition();
            if (isSnapshotMatch() && newPlayingTrackIndex != selectedPosition)
                selectedPosition = newPlayingTrackIndex;
        }
    }

    public void shuffleItems() {
        synchronized (this) {
            if(sourceItems != null){
                items.clear();
                items.addAll(sourceItems);

                if(isCanShuffle && activity != null && activity.getService() != null && activity.getService().isShuffle())
                    Collections.shuffle(items, NavigationActivity.randomGenerator);
            }
        }

        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void setTracks(ArrayList<Track> tracks) {
        isCanShuffle = true;

        sourceItems.clear();
        sourceItems.addAll(tracks);

        shuffleItems();

        if (adapter != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
    }

    public void setTracks(Playlist... playlists) {
        isCanShuffle = false;

        sourceItems.clear();

        for (Playlist p : playlists)
            sourceItems.addAll(p.getTracks());

        shuffleItems();

        if (adapter != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public int getPlayingPosition() {
        return activity.getService().getPlayingPosition();
    }

    public PlayerAdapter getAdapter() {
        return adapter;
    }

    public void scrollToTrack(int position) {
        int translated = adapter.listToVisiblePosition(position);
        if (translated < adapter.getItemCount()) {
            try {
                listView.smoothScrollToPosition(translated);
                listView.invalidate();
            } catch (Exception ignored) { }
        }
    }

    public void updateSnapshot() {
        playlistSnapshot = UUID.randomUUID().toString();
    }

    public boolean isSnapshotMatch() {
        return activity.getService() != null && activity.getService().isSnapshotMatch(playlistSnapshot);
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public boolean isNeedUpdate() {
        return needUpdate;
    }

    public void startSearch(String artist) {
        activity.onNavigationDrawerItemSelected(NavigationDrawerFragment.MenuLine.SEARCH, artist);
    }

    public void addToQuickPlaylist(Track track) {
        activity.addToQuickPlaylist(track, true, true);
    }

    public RecyclerView getListView() {
        return listView;
    }

    public void itemClick(int position) {
        if (activity.getService().isSnapshotMismatch(playlistSnapshot))
            activity.getService().setTracks(items, playlistSnapshot);

        selectedPosition = adapter.visibleToListPosition(position);

        if (selectedPosition < 0)
            return;

        adapter.notifyDataSetChanged();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (activity.getService().isShuffle()) {
                    activity.updatePlayPause();
                }
                activity.getService().startVolumeUpFlag = System.currentTimeMillis();
                activity.getService().play(selectedPosition);

                activity.updatePlayPause();

                Tracker t = activity.getTracker(NavigationActivity.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder().setCategory("Track").setAction("play").build());
            }
        }).start();
    }
}
