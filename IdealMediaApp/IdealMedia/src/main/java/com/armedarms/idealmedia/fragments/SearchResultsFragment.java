package com.armedarms.idealmedia.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tasks.TaskGetPlaylistVK;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.FillMediaStoreTracks;
import com.armedarms.idealmedia.utils.ResUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vk.sdk.VKSdk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;


public class SearchResultsFragment extends PagingPlayingFragment implements IHasColor {

    private String searchQuery;

    private Playlist playlistResultsLocal;
    private Playlist playlistResultsVK;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playlistResultsLocal = new Playlist();
        playlistResultsLocal.setTitle(getString(R.string.menu_on_device));

        playlistResultsVK = new Playlist();
        playlistResultsVK.setTitle(getString(R.string.menu_vk));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PlayerAdapter(activity, this, playlistResultsLocal, playlistResultsVK);

        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);

        Tracker t = ((NavigationActivity)getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.setScreenName("Playlist");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onStart() {
        super.onStart();

        update();
    }

    public void update() {
        if (!activity.isRefreshing())
            activity.setRefreshing(true);

        playlistResultsLocal.getTracks().clear();
        playlistResultsVK.getTracks().clear();
        updateTracks();

        //noinspection unchecked
        new AsyncTask<ArrayList<Track>, Track, ArrayList<Track>>() {

            @Override
            protected ArrayList<Track> doInBackground(ArrayList<Track>... trackLists) {
                return getAllTracks();
            }

            @Override
            protected void onPostExecute(ArrayList<Track> tracks) {
                super.onPostExecute(tracks);

                updateTracks();
            }
        }.execute();

        page = 0L;
        if (VKSdk.isLoggedIn() || VKSdk.wakeUpSession()) {
            new TaskGetPlaylistVK(activity, new TaskGetPlaylistVK.OnTaskGetPlaylistListener() {
                @Override
                public void OnTaskResult(ArrayList<Track> tracks) {
                    playlistResultsVK.getTracks().clear();

                    if (null == tracks || tracks.size() == 0)
                        Toast.makeText(activity, R.string.vk_empty_list, Toast.LENGTH_LONG).show();
                    else
                        appendTracks(tracks);
                }
            }).execute(TaskGetPlaylistVK.VK_METHOD_SEARCH, String.valueOf(page += 1), searchQuery);
        } else {
            Toast.makeText(activity, R.string.not_logged_in, Toast.LENGTH_LONG).show();

            activity.setRefreshing(false);
        }
    }

    private ArrayList<Track> getAllTracks() {
        ArrayList<Track> allTracks = (ArrayList<Track>) FileUtils.read("alltracksfs", activity);
        ArrayList<Track> allTracksMS = (ArrayList<Track>) FileUtils.read("alltracksms", activity);
        ArrayList<Track> result = new ArrayList<Track>(5);

        if (allTracksMS == null || allTracksMS.size() == 0)
            allTracksMS =  new FillMediaStoreTracks(activity).getTracks();

        result.addAll(search(searchQuery, allTracks));
        result.addAll(search(searchQuery, allTracksMS));

        // remove duplicates
        Set<Track> trackSet = new TreeSet<Track>(new Comparator<Track>() {
            @Override
            public int compare(Track track1, Track track2) {
                return track1.getDisplay().compareTo(track2.getDisplay());
            }
        });
        trackSet.addAll(result);

        playlistResultsLocal.getTracks().clear();
        playlistResultsLocal.getTracks().addAll(trackSet);

        return result;
    }

    private void updateTracks() {
        updateSnapshot();
        setTracks(playlistResultsLocal, playlistResultsVK);
        adapter.notifyDataSetChanged();
    }

    private ArrayList<Track> search(String searchQuery, ArrayList<Track> tracks) {
        ArrayList<Track> result = new ArrayList<Track>(5);

        if (searchQuery == null)
            return result;

        if (tracks != null) {
            for (Track t : tracks) {
                if (t != null && t.getArtist() != null && t.getTitle() != null && t.getDisplay().toLowerCase().contains(searchQuery.toLowerCase()))
                    result.add(t);
            }
        }

        return result;
    }

    @Override
    public void onLastListItemScrolling() {
        if (!isComplete && !activity.isRefreshing()) {
            activity.setRefreshing(true);
            isLoading = true;

            new TaskGetPlaylistVK(activity, new TaskGetPlaylistVK.OnTaskGetPlaylistListener() {
                @Override
                public void OnTaskResult(ArrayList<Track> result) {
                    appendTracks(result);
                }
            }).execute(TaskGetPlaylistVK.VK_METHOD_SEARCH, String.valueOf(page += 1), searchQuery);
        }
    }

    private void appendTracks(ArrayList<Track> tracks) {
        isLoading = false;

        if (null != tracks && isAdded()) {
            isComplete = tracks.size() < TaskGetPlaylistVK.VK_PAGE_SIZE;

            playlistResultsVK.getTracks().addAll(tracks);

            setTracks(playlistResultsLocal, playlistResultsVK);
            adapter.notifyDataSetChanged();
        }

        if (null == tracks || tracks.size() == 0)
            Toast.makeText(activity, R.string.vk_empty_list, Toast.LENGTH_LONG).show();

        activity.setRefreshing(false);
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorFolders);
    }

    public void setQuery(String q) {
        if (searchQuery != null && searchQuery.equals(q)) {
            setNeedUpdate(false);
        } else {
            setNeedUpdate(true);
            searchQuery = q;
        }

        if (isAdded())
            update();
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}