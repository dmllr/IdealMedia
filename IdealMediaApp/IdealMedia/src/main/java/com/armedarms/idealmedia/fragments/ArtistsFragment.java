package com.armedarms.idealmedia.fragments;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.util.AQUtility;
import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.ArtistsAdapter;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tasks.TaskGetArtists;
import com.armedarms.idealmedia.utils.FileUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

import libs.CircularProgressButton;

public class ArtistsFragment extends BaseFragment implements TaskGetArtists.OnTaskGetArtists, TaskGetArtists.OnProgressUpdateMy {

    public ArtistsAdapter adapter;

    private RecyclerView list;
    private CircularProgressButton progress;
    private View empty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);

        list = (RecyclerView)view.findViewById(android.R.id.list);
        list.setItemAnimator(new DefaultItemAnimator());
        setLayoutManager();

        progress = (CircularProgressButton)view.findViewById(android.R.id.progress);
        empty = view.findViewById(android.R.id.empty);

        new AsyncTask<Void, Void, ArrayList<Track>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                activity.setRefreshing(true);
            }

            @Override
            protected ArrayList<Track> doInBackground(Void... voids) {
                return (ArrayList<Track>) FileUtils.read("artistsTracks", activity);
            }

            @Override
            protected void onPostExecute(ArrayList<Track> tracks) {
                super.onPostExecute(tracks);
                activity.setRefreshing(false);
                OnTaskResult(tracks);
            }
        }.execute();

        Tracker t = ((NavigationActivity)getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.setScreenName("Artists");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setLayoutManager();
    }

    private void setLayoutManager() {
        int iDisplayWidth = Math.max(320, getResources().getDisplayMetrics().widthPixels);
        int spanCount = Math.max(1, iDisplayWidth / 599);

        list.setLayoutManager(new GridLayoutManager(activity, spanCount));
    }

    public void update(boolean refresh) {
        if (progress != null && progress.getVisibility() == View.GONE) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(1);
        }

        TaskGetArtists taskGetArtists = new TaskGetArtists(activity, refresh, this, this);
        taskGetArtists.execute();
    }

    @Override
    public void OnTaskResult(Object result) {
        if (null != result && isAdded()) {
            ArrayList<Track> allTracks = (ArrayList<Track>) result;
            applyAdapter(allTracks);
            if (progress.getVisibility() == View.VISIBLE)
                progress.setVisibility(View.GONE);

            empty.setVisibility(allTracks.size() == 0 ? View.VISIBLE : View.GONE);
        } else {
            empty.setVisibility(View.VISIBLE);
        }
    }

    void applyAdapter(ArrayList<Track> tracks) {
        if (tracks == null)
            return;
        adapter = new ArtistsAdapter(activity, tracks);
        list.setAdapter(adapter);
    }

    @Override
    public void OnArtistsProgress(final int progress) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArtistsFragment.this.progress.setProgress(progress);
            }
        });

    }
    @Override
    public void onResume() {
        super.onResume();
        AQUtility.debug("onResume", "Albums");

    }
}