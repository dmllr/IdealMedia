package com.armedarms.idealmedia.tasks;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.tools.MakePlaylistFS;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.MediaUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class TaskGetArtists extends AsyncTask {

    private final WeakReference<Activity> mActivity;
    private final WeakReference<OnTaskGetArtists> mOnTaskGetArtists;
    private final WeakReference<OnProgressUpdateMy> mOnProgressUpdate;
    private boolean refresh;
    private int newArtworksCounter;
    private AQuery aq;

    public static interface OnTaskGetArtists {
        public void OnTaskResult(Object result);
    }
    public static interface OnProgressUpdateMy {
        public void OnArtistsProgress(int progress);
    }

    public TaskGetArtists(Activity activity, boolean refresh, OnTaskGetArtists onTaskGetArtists, OnProgressUpdateMy onProgressUpdateMy)  {
        mActivity = new WeakReference<Activity>(activity);
        mOnTaskGetArtists = new WeakReference<OnTaskGetArtists>(onTaskGetArtists);
        mOnProgressUpdate = new WeakReference<OnProgressUpdateMy>(onProgressUpdateMy);
        this.refresh = refresh;
    }

    @Override
    protected Object doInBackground(Object... params) {
        Activity activity = mActivity.get();
        if (null == activity) {
            return null;
        }
        ArrayList<Track> artistsTracks = new ArrayList<Track>();
        if (!refresh) {
            artistsTracks = (ArrayList<Track>) FileUtils.read("artistsTracks", activity);
            if (artistsTracks != null && artistsTracks.size() > 0)
                return artistsTracks;

            artistsTracks = new ArrayList<Track>();
        }

        ArrayList<Playlist> playlists = new MakePlaylistFS(activity, refresh).getArrTracks();

        //выкидываем все дубликаты альбомов
        //сортируем по альбому
        aq = new AQuery(activity);
        for (Playlist t : playlists) {
            ArrayList<Track> allTracks = t.getTracks();

            //Создаем итератор и выкидываем дубли
            Iterator<Track> iterator = allTracks.iterator();
            String artist = "";

            int total = allTracks.size();
            int step = 0;
            newArtworksCounter = 0;
            while (iterator.hasNext()) {

                onUpdate((int) (100 * step / total));
                step++;

                Track track = iterator.next();
                final String currentArtist = ""+track.getArtist().toLowerCase();
                if (currentArtist.equals(artist) || currentArtist.equals("")) {
                    continue;
                } else {
                    artist = currentArtist;
                    boolean stop = false;
                    for (Track clsTrack : artistsTracks) {
                        if (clsTrack.getArtist().toLowerCase().equals(artist)) {
                            stop = true;
                            break;
                        }
                    }
                    if (stop) {
                        continue;
                    }
                    if (MediaUtils.getArtistQuick(activity, track, 300, 300) != null) {
                        artistsTracks.add(track);
                        continue;
                    }
                    else {
                        //noartwork (refresh or first start)
                        String url = String.format("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&api_key=683548928700d0cdc664691b862a8d21&artist=%s&format=json", Uri.encode(track.getArtist()));
                        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
                        cb.url(url).type(JSONObject.class).fileCache(true).expire(3600 * 60 * 1000);
                        aq.sync(cb);
                        JSONObject result = cb.getResult();

                        if (result != null) {
                            JSONObject jsonObject = null;
                            String albumArtImageLink = null;
                            try {
                                jsonObject = result.getJSONObject("artist");
                                JSONArray image = jsonObject.getJSONArray("image");
                                for (int i=0;i<image.length();i++) {
                                    jsonObject = image.getJSONObject(i);
                                    if (jsonObject.getString("size").equals("extralarge")) {
                                        albumArtImageLink = Uri.decode(jsonObject.getString("#text"));
                                    }
                                }
                                if (!albumArtImageLink.equals("")) {
                                    //download image

                                    String path = MediaUtils.getArtistPath(track);
                                    if (path == null) {
                                        continue;
                                    }
                                    File file = new File(path);
                                    AjaxCallback<File> cbFile = new AjaxCallback<File>();
                                    cbFile.url(albumArtImageLink).type(File.class).targetFile(file);
                                    aq.sync(cbFile);
                                    AjaxStatus status = cbFile.getStatus();
                                    if (status.getCode() == 200) {
                                        artistsTracks.add(track);
                                        newArtworksCounter++;
                                    }
                                    else {
                                        file.delete();
                                        continue;
                                    }
                                }
                                else {
                                    //art not found
                                    artistsTracks.add(track);
                                    continue;
                                }
                            }
                            catch (Exception e) {
                                iterator.remove();
                                e.printStackTrace();
                            }
                        }
                        else {
                            iterator.remove();
                        }
                    }

                }
            }
        }

        FileUtils.write("artistsTracks", activity, artistsTracks);

        return artistsTracks;
    }

    protected void onUpdate(Integer... progress) {
        OnProgressUpdateMy onProgressUpdateMy = mOnProgressUpdate.get();
        if (onProgressUpdateMy!=null){
            onProgressUpdateMy.OnArtistsProgress(progress[0]);
        }
    }

    protected void onPostExecute(Object result) {
        OnTaskGetArtists onTaskGetArtists = mOnTaskGetArtists.get();
        Activity activity = mActivity.get();
        if (null != onTaskGetArtists) {
            onTaskGetArtists.OnTaskResult(result);
        }
        if (null != activity)
            ((NavigationActivity) activity).setRefreshing(false);
    }
}