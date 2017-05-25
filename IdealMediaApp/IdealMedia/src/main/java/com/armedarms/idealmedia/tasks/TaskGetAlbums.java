package com.armedarms.idealmedia.tasks;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
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

public class TaskGetAlbums extends AsyncTask {

    private final WeakReference<Activity> mActivity;
    private final WeakReference<OnTaskGetAlbums> mOnTaskGetAlbums;
    private final WeakReference<OnProgressUpdateMy> mOnProgressUpdate;
    private boolean refresh;
    private int newArtworksCounter;
    private AQuery aq;

    public static interface OnTaskGetAlbums {
        public void OnTaskResult(Object result);
    }
    public static interface OnProgressUpdateMy {
        public void OnAlbumsProgress(int progress);
    }

    public TaskGetAlbums(Activity activity, boolean refresh, OnTaskGetAlbums onTaskGetAlbums, OnProgressUpdateMy onProgressUpdateMy)  {
        mActivity = new WeakReference<Activity>(activity);
        mOnTaskGetAlbums = new WeakReference<OnTaskGetAlbums>(onTaskGetAlbums);
        mOnProgressUpdate = new WeakReference<OnProgressUpdateMy>(onProgressUpdateMy);
        this.refresh = refresh;
    }

    @Override
    protected Object doInBackground(Object... params) {
        Activity activity = mActivity.get();
        if (null == activity) {
            return null;
        }
        ArrayList<Track> albumsTracks = new ArrayList<Track>();
        if (!refresh) {
            albumsTracks = (ArrayList<Track>) FileUtils.read("albumsTracks", activity);
            if (albumsTracks != null && albumsTracks.size() > 0)
                return albumsTracks;
            albumsTracks = new ArrayList<Track>();
        }

        ArrayList<Playlist> playlists = new MakePlaylistFS(activity, refresh).getArrTracks();

        //выкидываем все дубликаты альбомов
        //сортируем по альбому
        aq = new AQuery(activity);
        for (Playlist t : playlists) {
            ArrayList<Track> allTracks = t.getTracks();

            //Создаем итератор и выкидываем дубли
            Iterator<Track> iterator = allTracks.iterator();
            String album = "";

            int total = allTracks.size();
            int step = 0;
            newArtworksCounter = 0;
            while (iterator.hasNext()) {

                onUpdate((int) (100 * step / total));
                step++;

                Track track = iterator.next();
                final String currentAlbum = ""+track.getAlbum().toLowerCase();
                if (currentAlbum.equals(album) || currentAlbum.equals("")) {
                    continue;
                }
                else {
                    album = currentAlbum;
                    boolean stop = false;
                    for (Track clsTrack:albumsTracks) {
                        if (clsTrack.getAlbum().toLowerCase().equals(album)) {
                            stop = true;
                            break;
                        }
                    }
                    if (stop) {
                        continue;
                    }
                    if (MediaUtils.getArtworkQuick(activity, track, 300, 300)!=null) {
                        albumsTracks.add(track);
                        continue;
                    }
                    else {
                        //noartwork (refresh or first start)
                        String url = String.format("http://ws.audioscrobbler.com/2.0/?method=album.getinfo&api_key=683548928700d0cdc664691b862a8d21&artist=%s&album=%s&format=json", Uri.encode(track.getArtist()),Uri.encode(currentAlbum));
                        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
                        cb.url(url).type(JSONObject.class).fileCache(true).expire(3600 * 60 * 1000);
                        aq.sync(cb);
                        JSONObject result = cb.getResult();

                        if (result!=null) {
                            JSONObject jsonObject = null;
                            String albumArtImageLink = null;
                            try {
                                if (!result.has("album"))
                                    continue;

                                jsonObject = result.getJSONObject("album");
                                JSONArray image = jsonObject.getJSONArray("image");
                                for (int i=0;i<image.length();i++) {
                                    jsonObject = image.getJSONObject(i);
                                    if (jsonObject.getString("size").equals("extralarge")) {
                                        albumArtImageLink = Uri.decode(jsonObject.getString("#text"));
                                    }
                                }
                                if (!albumArtImageLink.equals("")) {
                                    //download image

                                    String path = MediaUtils.getAlbumPath(track);
                                    if (path == null) {
                                        continue;
                                    }
                                    File file = new File(path);
                                    AjaxCallback<File> cbFile = new AjaxCallback<File>();
                                    cbFile.url(albumArtImageLink).type(File.class).targetFile(file);
                                    aq.sync(cbFile);
                                    AjaxStatus status = cbFile.getStatus();
                                    if (status.getCode() == 200) {
                                        ContentResolver res = activity.getContentResolver();
                                        ContentValues values = new ContentValues();
                                        values.put("album_id", track.getAlbumId());
                                        values.put("_data", path);

                                        Uri newuri = res.insert(MediaUtils.sArtworkUri, values);
                                        albumsTracks.add(track);
                                        newArtworksCounter++;
                                    }
                                    else {
                                        file.delete();
                                        continue;
                                    }
                                }
                                else {
                                    //art not found
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
        FileUtils.write("albumsTracks", activity, albumsTracks);
        return albumsTracks;
    }

    protected void onUpdate(Integer... progress) {
        OnProgressUpdateMy onProgressUpdateMy = mOnProgressUpdate.get();
        if (onProgressUpdateMy!=null){
            onProgressUpdateMy.OnAlbumsProgress(progress[0]);
        }
    }

    protected void onPostExecute(Object result) {
        OnTaskGetAlbums onTaskGetAlbums = mOnTaskGetAlbums.get();
        Activity activity = mActivity.get();
        if (null != onTaskGetAlbums) {
            onTaskGetAlbums.OnTaskResult(result);
        }
        if (null != activity)
            ((NavigationActivity) activity).setRefreshing(false);
    }
}