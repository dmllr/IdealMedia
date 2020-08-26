package com.armedarms.idealmedia.fragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.FoldersAdapter;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.dialogs.DialogSelectDirectory;
import com.armedarms.idealmedia.tasks.TaskGetPlaylistFilesystem;
import com.armedarms.idealmedia.utils.ResUtils;

import java.util.ArrayList;

public class FoldersFragment extends BaseFragment implements IHasColor, TaskGetPlaylistFilesystem.OnTaskGetPlaylist {

    private String mediaPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private RecyclerView listView;
    public FoldersAdapter adapter;
    public ArrayList<Playlist> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folders, container, false);
    }

    @Override
    public void onActivityCreated(Bundle state) {
        super.onActivityCreated(state);
        items = new ArrayList<Playlist>();
        adapter = new FoldersAdapter(activity, items);
        listView.setAdapter(adapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());
    }

    public void update(boolean refresh) {
        if (items != null) {
            items.clear();
            adapter.notifyDataSetChanged();
        }

        boolean fullScan = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.key_media_method_full), false);

        new TaskGetPlaylistFilesystem(activity, fullScan ? TaskGetPlaylistFilesystem.TYPE_FS : TaskGetPlaylistFilesystem.TYPE_MS, refresh, this).execute();
    }

    @Override
    public void onStart() {
        super.onStart();

        mediaPath = PreferenceManager.getDefaultSharedPreferences(activity).getString(getString(R.string.key_mediapath), "/");
        if (mediaPath.equals("")) // never, yeah
            new DialogSelectDirectory(
                    activity,
                    getFragmentManager(),
                    new DialogSelectDirectory.Result() {
                        @Override
                        public void onChooseDirectory(String dir) {
                            mediaPath = dir;
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(getString(R.string.key_mediapath), dir).commit();
                            update(true);
                        }

                        @Override
                        public void onCancelChooseDirectory() {
                            PreferenceManager.getDefaultSharedPreferences(activity).edit().putString(getString(R.string.key_mediapath), "/").commit();
                        }
                    },
                    "/");
    }

    @Override
    public void onResume() {
        super.onResume();

        //ArrayList<Track> allTracks = (ArrayList<Track>) FileUtils.read("alltracksfs", activity);
        //ArrayList<Track> allTracksMediaStore = (ArrayList<Track>) FileUtils.read("alltracksms", activity);

        update(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void OnTaskResult(ArrayList<Playlist> result) {
        if (null != result && isAdded()) {
            items.addAll(result);

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorFolders);
    }
}