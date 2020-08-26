package com.armedarms.idealmedia.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.androidquery.AQuery;
import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.MediaUtils;

import java.util.ArrayList;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder> {

    LayoutInflater inflater;
    private final AQuery listAq;
    ArrayList<Track> data;
    Activity activity;
    private int scrollState;
    Animation fadeIn;

    public ArtistsAdapter(Activity activity, ArrayList<Track> data){
        this.data = data;
        this.activity = activity;

        inflater = activity.getLayoutInflater();

        listAq = new AQuery(activity);

        fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(100);
        fadeIn.setInterpolator(new DecelerateInterpolator());
    }

    @Override
    public ArtistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.cell_artist, parent, false);
        return new ArtistViewHolder(view, parent);
    }

    @Override
    public void onBindViewHolder(ArtistViewHolder holder, final int position) {
        final Track track = data.get(position);

        AQuery aq = listAq.recycle(holder.itemView);

        holder.title.setText(track.getArtist());

        Bitmap artwork = MediaUtils.getArtistQuick(activity, track, 300, 300);

        if (artwork != null) {
            aq.id(holder.blurredImage).image(MediaUtils.fastblur(artwork, 6)).animate(fadeIn);
            aq.id(holder.image).image(artwork).animate(fadeIn);
        } else {
            aq.id(holder.blurredImage).image(R.drawable.ic_default_album).animate(fadeIn);
            aq.id(holder.image).image(artwork);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Track track = data.get(position);
                final String artist = track.getArtist();
                ArrayList<Track> tracks = (ArrayList<Track>) FileUtils.read("alltracksfs", activity);

                ArrayList<Track> tracksFiltered = new ArrayList<Track>();
                for(Track t: tracks) {
                    if (t.getArtist().equalsIgnoreCase(artist)) {
                        tracksFiltered.add(t);
                    }
                }
                ((NavigationActivity)activity).exhibit(tracksFiltered);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ArtistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView blurredImage;
        private final ImageView image;
        private final TextView title;

        final ViewGroup parent;

        public ArtistViewHolder(View view, ViewGroup parent) {
            super(view);
            this.parent = parent;

            blurredImage = (ImageView)view.findViewById(android.R.id.icon);
            image = (ImageView)view.findViewById(android.R.id.icon1);
            title = (TextView)view.findViewById(android.R.id.text1);
        }

    }
}
