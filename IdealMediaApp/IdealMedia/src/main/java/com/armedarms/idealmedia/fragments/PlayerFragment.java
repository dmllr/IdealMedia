package com.armedarms.idealmedia.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.adapters.PlayerAdapter;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.ResUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.r0adkll.postoffice.PostOffice;
import com.r0adkll.postoffice.model.Design;

import java.util.ArrayList;


public class PlayerFragment extends BasePlayingFragment implements IHasColor {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PlayerAdapter(activity, this, items);
        listView = (RecyclerView) view.findViewById(android.R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setItemAnimator(new DefaultItemAnimator());

        listView.setAdapter(adapter);

        Tracker t = ((NavigationActivity)getActivity()).getTracker(NavigationActivity.TrackerName.APP_TRACKER);
        t.setScreenName("Now playing");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onStart() {
        super.onStart();

        update();
    }

    public void update() {
        if (items != null) {
            items.clear();
            adapter.notifyDataSetChanged();
        }

        TaskGetPlaylist taskGetPlaylist = new TaskGetPlaylist();
        taskGetPlaylist.setContext(activity.getApplicationContext());
        taskGetPlaylist.execute();
    }

    @Override
    public int getColor() {
        return ResUtils.color(activity, R.attr.colorPlaylists);
    }

    public class TaskGetPlaylist extends AsyncTask<Void, Void, ArrayList<Track>> {
        private Context context;

        public void setContext(Context context) {
            this.context = context;
        }

        @Override
        protected ArrayList<Track>doInBackground(Void... params) {
            if (activity != null && activity.getService() != null && activity.getService().getTracks() != null && activity.getService().getTracks().size() > 0)
                return activity.getService().getTracks();
            else
                return (ArrayList<Track>)FileUtils.read(context.getString(R.string.key_tracks), activity);
        }

        @Override
        protected void onPostExecute(ArrayList<Track> result) {
            if (!PlayerFragment.this.isAdded())
                return;

            if (result != null) {
                sourceItems = result;
                shuffleItems();
            }
            else {
                activity.placeFoldersFragment();
                /*
                if (PreferenceManager.getDefaultSharedPreferences(activity).getString(getString(R.string.key_mediapath), "").equals("")) {

                    PostOffice.newMail(context)
                            .setTitle(R.string.scan_filesystem_title)
                            .setMessage(R.string.scan_filesystem_question)
                            .setDesign(Design.MATERIAL_LIGHT)
                            .setButtonTextColor(Dialog.BUTTON_POSITIVE, ResUtils.resolve(activity, R.attr.colorPositive))
                            .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    activity.putFoldersFragment();
                                }
                            })
                            .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(getString(R.string.key_mediapath), "/").commit();
                                }
                            })
                            .build()
                            .show(getFragmentManager());
                }
                */
            }
        }
    }

}