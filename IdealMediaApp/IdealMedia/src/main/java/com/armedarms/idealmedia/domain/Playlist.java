package com.armedarms.idealmedia.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist implements Serializable {

    public UUID id;

    private String title;
    private String artists;
    private ArrayList<Track> tracks;

    public Playlist() {
    }

    public Playlist(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    public void add(Track track) {
        tracks.add(track);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<Track> getTracks() {
        if (tracks == null)
            tracks = new ArrayList<Track>();

        return tracks;
    }

    public void setTracks(ArrayList<Track> tracks) {
        this.tracks = tracks;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public boolean checkSelection() {
        int i = 0;
        for (Track t: this.getTracks()) {
            if (t.isSelected()) {
                i++;
            }
        }
        return i > 0;
    }

    @Override
    public String toString() {
        return title;
    }

    public int size() {
        if (tracks == null)
            return 0;

        return tracks.size();
    }
}
