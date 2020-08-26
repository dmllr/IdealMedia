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
import android.widget.Toast;

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


public class SearchResultsFragment extends PagingPlayingFragment implements IHasColor {

    private String searchQuery;

    private Playlist playlistResultsLocal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        playlistResultsLocal = new Playlist();
        playlistResultsLocal.setTitle(getString(R.string.menu_on_device));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PlayerAdapter(activity, this, playlistResultsLocal);

        listView = view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);
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
        setTracks(playlistResultsLocal);
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
        }
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