package com.armedarms.idealmedia.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tasks.TaskGetPlaylistVK;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.ResUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vk.sdk.VKSdk;

import java.util.ArrayList;

public class VKAudioFragment extends PagingPlayingFragment implements IHasColor {

    private String method = TaskGetPlaylistVK.VK_METHOD_GET_POPULAR;
    private String searchQuery;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vk_audio, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PlayerAdapter(activity, this, items);
        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setAdapter(adapter);

        Tracker t = ((NavigationActivity)getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.setScreenName("VK audio");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    public void update(boolean refresh) {
        if (refresh) {
            activity.setRefreshing(true);

            items.clear();
            page = 0L;
            boolean isForeignVK = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.key_foreign_vk_popular), true);

            TaskGetPlaylistVK task = new TaskGetPlaylistVK(activity, new TaskGetPlaylistVK.OnTaskGetPlaylistListener() {
                @Override
                public void OnTaskResult(ArrayList<Track> result) {
                    setItems(result, true);
                }
            });
            if (TaskGetPlaylistVK.VK_METHOD_SEARCH.equals(method))
                task.execute(method, String.valueOf(page += 1), searchQuery);
            else
                task.execute(method, String.valueOf(page += 1), String.valueOf(isForeignVK));

            updateSnapshot();
            listView.scrollToPosition(0);
            setNeedUpdate(false);
        } else
            restoreCache();

        Tracker t = ((NavigationActivity)getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder().setCategory("UX").setAction("vkAudio").setLabel(method + (refresh ? " (refresh)" : " (cache)")).build());
    }

    public void setMethod(String method) {
        setMethod(method, "");
    }

    public void setMethod(String method, String param) {
        if (this.method == method && searchQuery == param) {
            setNeedUpdate(false);
        } else {
            setNeedUpdate(true);
            this.method = method;
            searchQuery = param;
        }

        if (isAdded())
            update(isNeedUpdate());
    }

    private void restoreCache() {
        new AsyncTask<Void, Void, ArrayList<Track>>() {
            @Override
            protected ArrayList<Track> doInBackground(Void... voids) {
                return (ArrayList<Track>) FileUtils.read(getCacheKey(), activity);
            }

            @Override
            protected void onPostExecute(ArrayList<Track> tracks) {
                if (tracks != null) {
                    page = (long)Math.ceil((double)tracks.size() / TaskGetPlaylistVK.VK_PAGE_SIZE);
                    sourceItems.clear();
                    sourceItems.addAll(tracks);
                    shuffleItems();
                }
            }
        }.execute();
        adapter.notifyDataSetChanged();
    }

    private void clearCache() {
        FileUtils.clearObject(getCacheKey(), activity);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (VKSdk.isLoggedIn() || VKSdk.wakeUpSession())
            update(isNeedUpdate());
        else
            VKSdk.authorize(NavigationActivity.vkScope, true, false);
    }

    @Override
    public void onLastListItemScrolling() {
        if (!isComplete && !activity.isRefreshing()) {
            activity.setRefreshing(true);
            isLoading = true;

            TaskGetPlaylistVK task = new TaskGetPlaylistVK(activity, new TaskGetPlaylistVK.OnTaskGetPlaylistListener() {
                @Override
                public void OnTaskResult(ArrayList<Track> result) {
                    setItems(result, false);
                }
            });

            if (TaskGetPlaylistVK.VK_METHOD_SEARCH.equals(method))
                task.execute(method, String.valueOf(page += 1), searchQuery);
            else
                task.execute(method, String.valueOf(page += 1));
        }
    }

    private void setItems(ArrayList<Track> tracks, boolean replace) {
        isLoading = false;

        if (null != tracks && isAdded()) {
            isComplete = tracks.size() < TaskGetPlaylistVK.VK_PAGE_SIZE;

            if (replace)
                sourceItems.clear();

            sourceItems.addAll(tracks);
            shuffleItems();

            FileUtils.write(getCacheKey(), activity, sourceItems);

            //adapter.notifyDataSetChanged();
        }

        if (null == tracks || tracks.size() == 0) {
            Toast.makeText(activity, R.string.vk_empty_list, Toast.LENGTH_LONG).show();
        }
    }

    private String getCacheKey() {
        return String.format("%s_%s", activity.getString(R.string.key_vk), method);
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorVK);
    }

    public String getMethod() {
        return method;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}