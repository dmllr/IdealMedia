package com.armedarms.idealmedia.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.MediaUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class TaskGetArtwork extends AsyncTask<Track, Void, Void> {
    Context context;
    IGetArtworkListener listener;
    static HashMap<String, TaskGetArtwork> activeTasks = new HashMap<String, TaskGetArtwork>();

    public TaskGetArtwork(Context context) {
        this.context = context;
    }

    public TaskGetArtwork withListener(IGetArtworkListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    protected Void doInBackground(Track... tracks) {
        final Track track = tracks[0];
        getArtwork(track);

        return null;
    }

    public void getArtwork(Track track) {
        final String currentArtist = "" + track.getArtist();

        if (currentArtist.equals(""))
            return;

        if (MediaUtils.getArtworkQuick(context, track, 300, 300) != null)
            return;

        if (activeTasks.size() > 2)
            return;

        if (activeTasks.containsKey(currentArtist))
            return;

        activeTasks.put(currentArtist, this);

        AQuery aq = new AQuery(context);
        String url = String.format("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&api_key=683548928700d0cdc664691b862a8d21&artist=%s&format=json", Uri.encode(currentArtist));
        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
        cb.url(url).type(JSONObject.class).fileCache(true).expire(3600 * 60 * 1000);
        aq.sync(cb);
        JSONObject result = cb.getResult();

        if (result != null) {
            JSONObject jsonObject = null;
            String albumArtImageLink = null;
            try {
                if (!result.has("artist"))
                    return;

                jsonObject = result.getJSONObject("artist");
                JSONArray image = jsonObject.getJSONArray("image");
                for (int i=0;i<image.length();i++) {
                    jsonObject = image.getJSONObject(i);
                    if (jsonObject.getString("size").equals("extralarge")) {
                        albumArtImageLink = Uri.decode(jsonObject.getString("#text"));
                    }
                }
                if (!(albumArtImageLink != null && albumArtImageLink.equals(""))) {
                    String path = MediaUtils.getArtistPath(track);
                    if (path != null) {
                        File file = new File(path);

                        AjaxCallback<File> cbFile = new AjaxCallback<File>();
                        cbFile.url(albumArtImageLink).type(File.class).targetFile(file);
                        aq.sync(cbFile);

                        AjaxStatus status = cbFile.getStatus();
                        if (status.getCode() == 200) {
                            if (listener != null)
                                listener.onNewArtwork();
                        } else {
                            file.delete();
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        activeTasks.remove(currentArtist);
    }

    public static interface IGetArtworkListener {
        public void onNewArtwork();
    }

}
