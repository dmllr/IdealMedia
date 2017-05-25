package com.armedarms.idealmedia.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.domain.Track;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiAudio;
import com.vk.sdk.api.model.VKList;
import com.vk.sdk.util.VKUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TaskGetPlaylistVK extends AsyncTask<String, Void, ArrayList<Track>> {

    public static final long VK_PAGE_SIZE = 20L;

    public static final String VK_METHOD_GET = "get";
    public static final String VK_METHOD_GET_POPULAR = "getPopular";
    public static final String VK_METHOD_GET_RECOMMENDATIONS = "getRecommendations";
    public static final String VK_METHOD_SEARCH = "search";

    private final WeakReference<Activity> mActivity;
    private final OnTaskGetPlaylistListener listener;

    private String method;
    String query = "";
    boolean isForeignPopular = false;
    final Object waitObj = new Object();

    public static interface OnTaskGetPlaylistListener {
        public void OnTaskResult(ArrayList<Track> result);
    }

    public TaskGetPlaylistVK(Activity activity, OnTaskGetPlaylistListener onTaskGetPlaylistListener) {
        mActivity = new WeakReference<Activity>(activity);
        listener = onTaskGetPlaylistListener;
    }

    @Override
    protected ArrayList<Track> doInBackground(String... params) {
        Activity activity = mActivity.get();
        if (null == activity) {
            return null;
        }

        method = params[0];
        long page = Long.valueOf(params[1]);
        if (params.length > 2) {
            if (TaskGetPlaylistVK.VK_METHOD_SEARCH.equals(method))
                query = params[2];
            if (TaskGetPlaylistVK.VK_METHOD_GET_POPULAR.equals(method))
                isForeignPopular = Boolean.parseBoolean(params[2]);
        }

        List<VKApiAudio> audios = new ArrayList<VKApiAudio>();

        if (VK_METHOD_GET.equals(method))
            audios = getAudio("audio.get", "", VK_PAGE_SIZE, VK_PAGE_SIZE * (page - 1));
        if (VK_METHOD_GET_POPULAR.equals(method))
            audios = getAudio("audio.getPopular", "", VK_PAGE_SIZE, VK_PAGE_SIZE * (page - 1));
        if (VK_METHOD_GET_RECOMMENDATIONS.equals(method))
            audios = getAudio("audio.getRecommendations", "", VK_PAGE_SIZE, VK_PAGE_SIZE * (page - 1));
        if (VK_METHOD_SEARCH.equals(method))
            audios = getAudio("audio.search", query, VK_PAGE_SIZE, VK_PAGE_SIZE * (page - 1));

        ArrayList<Track> tracks = new ArrayList<Track>();
        for (VKApiAudio audio : audios)
            tracks.add(Track.fromVKApiAudio(audio));

        Tracker t = ((NavigationActivity)activity).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder().setCategory("VK").setAction(method).build());

        return tracks;
    }

    private List<VKApiAudio> getAudio(final String method, final String q, long count, long offset) {
        final VKList<VKApiAudio> audioArray = new VKList<VKApiAudio>();
        synchronized(waitObj) {
            VKParameters params = new VKParameters(VKUtil.mapFrom(
                    VKApiConst.Q, q,
                    "only_eng", isForeignPopular ? 1 : 0,
                    VKApiConst.COUNT, count,
                    VKApiConst.OFFSET, offset
            ));
            VKRequest vkr = new VKRequest(method, params);

            vkr.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                    synchronized(waitObj) {

                        VKList<VKApiAudio> list = new VKList<VKApiAudio>(response.json, VKApiAudio.class);
                        audioArray.addAll(list);

                        try {
                            waitObj.notify();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            try {
                waitObj.wait(5000);
                return audioArray;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return new ArrayList<VKApiAudio>();
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<VKApiAudio>();
            }
        }
    }

    protected void onPostExecute(ArrayList<Track> result) {
        Activity activity = mActivity.get();

        if (null != listener)
            listener.OnTaskResult(result);

        if (null != activity) {
            ((NavigationActivity) activity).setRefreshing(false);

            if ("search".equals(method))
                ((NavigationActivity) activity).searchDone(result != null && result.size() > 0, query);
        }
    }
}