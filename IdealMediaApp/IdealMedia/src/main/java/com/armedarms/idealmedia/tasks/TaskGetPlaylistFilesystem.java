package com.armedarms.idealmedia.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.tools.MakePlaylistFS;
import com.armedarms.idealmedia.tools.MakePlaylistMS;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class TaskGetPlaylistFilesystem extends AsyncTask<Void, Void, ArrayList<Playlist>> {

    private final WeakReference<Activity> mActivity;
    private final WeakReference<OnTaskGetPlaylist> mOnTaskGetPlaylist;
    private int type;
    private boolean refresh;

    public static final int TYPE_MS = 0;
    public static final int TYPE_FS = 1;

    public static interface OnTaskGetPlaylist {
        public void OnTaskResult(ArrayList<Playlist> result);
    }

    public TaskGetPlaylistFilesystem(Activity activity, int type, boolean refresh, OnTaskGetPlaylist onTaskGetPlaylist)  {
        mActivity = new WeakReference<Activity>(activity);
        mOnTaskGetPlaylist = new WeakReference<OnTaskGetPlaylist>(onTaskGetPlaylist);
        this.type = type;
        this.refresh = refresh;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Activity activity = mActivity.get();
        if (activity != null && activity instanceof NavigationActivity)
            ((NavigationActivity)activity).setRefreshing(true);
    }

    @Override
    protected ArrayList<Playlist> doInBackground(Void... params) {
        Activity activity = mActivity.get();
        if (null == activity)
            return null;

        switch (this.type){
            case TYPE_MS:
                return new MakePlaylistMS(activity, refresh).getArrTracks();
            case TYPE_FS:
                return new MakePlaylistFS(activity ,refresh).getArrTracks();
            default:
                return new MakePlaylistMS(activity, refresh).getArrTracks();
        }
    }

    protected void onPostExecute(ArrayList<Playlist> result) {
        OnTaskGetPlaylist onTaskGetPlaylist = mOnTaskGetPlaylist.get();

        Activity activity = mActivity.get();

        if (null != onTaskGetPlaylist)
            onTaskGetPlaylist.OnTaskResult(result);

        if (activity != null && activity instanceof NavigationActivity)
            ((NavigationActivity) activity).setRefreshing(false);
    }
}