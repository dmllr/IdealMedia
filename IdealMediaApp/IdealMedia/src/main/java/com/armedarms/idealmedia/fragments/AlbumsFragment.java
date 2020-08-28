package com.armedarms.idealmedia.fragments;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tasks.TaskGetAlbums;
import com.armedarms.idealmedia.adapters.AlbumsAdapter;
import com.armedarms.idealmedia.utils.FileUtils;

import java.util.ArrayList;

import libs.CircularProgressButton;

public class AlbumsFragment extends BaseFragment implements TaskGetAlbums.OnTaskGetAlbums, TaskGetAlbums.OnProgressUpdateMy {

    public AlbumsAdapter adapter;

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

        list = view.findViewById(android.R.id.list);
        list.setItemAnimator(new DefaultItemAnimator());
        setLayoutManager();

        progress = view.findViewById(android.R.id.progress);
        empty = view.findViewById(android.R.id.empty);

        new AsyncTask<Void, Void, ArrayList<Track>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                activity.setRefreshing(true);
            }

            @Override
            protected ArrayList<Track> doInBackground(Void... voids) {
                return (ArrayList<Track>) FileUtils.read("albumsTracks", activity);
            }

            @Override
            protected void onPostExecute(ArrayList<Track> tracks) {
                super.onPostExecute(tracks);
                activity.setRefreshing(false);
                OnTaskResult(tracks);
            }
        }.execute();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setLayoutManager();
    }

    private void setLayoutManager() {
        int iDisplayWidth = Math.max(320, getResources().getDisplayMetrics().widthPixels);
        int spanCount = Math.max(1, iDisplayWidth / 299);

        list.setLayoutManager(new GridLayoutManager(activity, spanCount));
    }

    public void update(boolean refresh) {
        if (progress != null && progress.getVisibility() == View.GONE) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(1);
        }

        TaskGetAlbums taskGetAlbums = new TaskGetAlbums(activity, refresh, this, this);
        taskGetAlbums.execute();
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
        if (tracks == null) return;
        adapter = new AlbumsAdapter(activity, tracks);
        list.setAdapter(adapter);
    }

    @Override
    public void OnAlbumsProgress(final int progress) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlbumsFragment.this.progress.setProgress(progress);
            }
        });

    }
}