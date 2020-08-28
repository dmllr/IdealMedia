package com.armedarms.idealmedia.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.FillMediaStoreTracks;
import com.armedarms.idealmedia.utils.ResUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;


public class PlaylistFragment extends BasePlayingFragment implements IHasColor {

    private Playlist playlist;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PlayerAdapter(activity, this, items);
        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (playlist != null)
            update();
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        updateSnapshot();
        update();
    }

    public Playlist getPlaylist() {
        return this.playlist;
    }

    public void setAllMusicPlaylistAsync(NavigationActivity activity) {
        this.activity = activity;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getAllTracks();
                return null;
            }
        }.execute();
    }

    private void getAllTracks() {
        ArrayList<Track> allTracks = (ArrayList<Track>) FileUtils.read("alltracksfs", activity);
        ArrayList<Track> allTracksMS = (ArrayList<Track>) FileUtils.read("alltracksms", activity);
        ArrayList<Track> result = new ArrayList<Track>(5);

        if (activity == null)
            activity = activity();
        if (activity == null)
            activity = (NavigationActivity) getActivity();
        if (activity == null)
            return;

        if (allTracksMS == null || allTracksMS.size() == 0)
            allTracksMS =  new FillMediaStoreTracks(activity).getTracks();

        if (allTracks != null)
            result.addAll(allTracks);
        if (allTracksMS != null)
            result.addAll(allTracksMS);

        // remove duplicates
        Set<Track> trackSet = new TreeSet<Track>(new Comparator<Track>() {
            @Override
            public int compare(Track track1, Track track2) {
                return track1.getDisplay().compareTo(track2.getDisplay());
            }
        });
        trackSet.addAll(result);

        if (playlist == null)
            playlist = new Playlist();

        playlist.getTracks().clear();
        playlist.getTracks().addAll(trackSet);

        updateSnapshot();
        update();
    }

    public void update() {
        if (playlist == null)
            return;

        setTracks(playlist.getTracks());

        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.setRefreshing(false);
                }
            });
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorPlaylists);
    }

}