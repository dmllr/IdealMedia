package com.armedarms.idealmedia.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.androidquery.AQuery;
import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.MediaUtils;

import java.util.ArrayList;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder> {

    private final AQuery listAq;
    ArrayList<Track> data;
    Activity activity;
    Animation fadeIn;

    public AlbumsAdapter(Activity activity, ArrayList<Track> data){
        this.data = data;
        this.activity = activity;

        listAq = new AQuery(activity);

        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(100);
        fadeIn.setInterpolator(new DecelerateInterpolator());
    }

    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.cell_album, parent, false);
        return new AlbumViewHolder(view, parent);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(AlbumViewHolder holder, final int position) {
        final Track track = data.get(position);
        AQuery aq = listAq.recycle(holder.itemView);
        Bitmap artwork = MediaUtils.getArtworkQuick(activity, track, 300, 300);

        if (artwork != null)
            aq.id(holder.image).image(artwork).animate(fadeIn);
        else
            aq.id(holder.image).image(R.drawable.ic_default_album).animate(fadeIn);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Track track = data.get(position);
                final String album = track.getAlbum();
                final String artist = track.getArtist();
                ArrayList<Track> tracks = (ArrayList<Track>) FileUtils.read("alltracksfs", activity);


                ArrayList<Track> tracksFiltered = new ArrayList<Track>();
                for(Track t: tracks) {
                    if (t.getAlbum().equals(album) && t.getArtist().equals(artist)) {
                        tracksFiltered.add(t);
                    }
                }
                ((NavigationActivity)activity).exhibit(tracksFiltered);
            }
        });
    }

    public class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;

        final ViewGroup parent;

        public AlbumViewHolder(View view, ViewGroup parent) {
            super(view);
            this.parent = parent;

            image = (ImageView)view.findViewById(android.R.id.icon);
        }

    }
}
