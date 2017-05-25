package com.armedarms.idealmedia.tools;

import android.content.Context;

import com.armedarms.idealmedia.NavigationActivity;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.Settings;
import com.armedarms.idealmedia.domain.Playlist;
import com.armedarms.idealmedia.domain.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public abstract class MakePlaylistAbstract {

    public ArrayList<Track> allTracks;
    private ArrayList<Track> tmpTracks = new ArrayList<Track>();
    private ArrayList<Playlist> arrTracks = new ArrayList<Playlist>();
    private static final String OTHERS = "OTHERS";
    long t = System.currentTimeMillis();

    public abstract void getAllTracks(Context context, boolean refresh);

    public MakePlaylistAbstract(Context context, boolean refresh)
    {
        allTracks = new ArrayList<Track>();
        arrTracks.clear();

        getAllTracks(context,refresh);

        Collections.sort(allTracks, new Comparator<Track>() {
            @Override
            public int compare(Track o1, Track o2) {
                return o1.getLastModified() > o2.getLastModified() ? -1 : o1.getLastModified() == o2.getLastModified() ? 0 : 1;
            }
        });

        tmpTracks.clear();
        HashMap<String,Integer> foldersMap = new HashMap<String, Integer>();
        HashMap<String,String> artistsMap = new HashMap<String, String>();

        Iterator<Track> iterator;

        if (Settings.PREMIUM || ((NavigationActivity)context).hasPremiumPurchase()) {
            iterator = allTracks.iterator();
            long firstRecentlyAddedTrack = 0;
            String artistInRecentlyAddedFolder = "";
            while (iterator.hasNext()) {
                Track next = iterator.next();

                if (firstRecentlyAddedTrack == 0)
                    firstRecentlyAddedTrack = next.getLastModified();

                if (firstRecentlyAddedTrack - next.getLastModified() <= 30 * 60 * 1000) {
                    Track track = Track.newInstance(next);
                    track.setGroup(context.getString(R.string.recently_added));
                    tmpTracks.add(track);

                    if (next.getArtist() != null && !artistInRecentlyAddedFolder.contains(next.getArtist())) {
                        if (!artistInRecentlyAddedFolder.equals("")) {
                            artistInRecentlyAddedFolder += ", ";
                        }
                        artistInRecentlyAddedFolder += next.getArtist();
                    }
                } else {
                    String currFolder = next.getFolder();
                    if (foldersMap.containsKey(currFolder)) {
                        foldersMap.put(currFolder, foldersMap.get(currFolder) + 1);
                        if (next.getArtist() != null && !artistsMap.get(currFolder).contains(next.getArtist())) {
                            artistsMap.put(currFolder, artistsMap.get(currFolder) + "," + next.getArtist());
                        }
                    } else {
                        foldersMap.put(currFolder, 1);
                        artistsMap.put(currFolder, "" + next.getArtist());
                    }
                }
            }
            addToTracks(context.getString(R.string.recently_added), artistInRecentlyAddedFolder);
        }
        
        iterator = allTracks.iterator();
        while (iterator.hasNext()) {
            Track playlist = iterator.next();
            String currFolder = playlist.getFolder();
            if (foldersMap.containsKey(currFolder)) {
                foldersMap.put(currFolder, foldersMap.get(currFolder)+1);
                if (playlist.getArtist() != null && !artistsMap.get(currFolder).contains(playlist.getArtist())) {
                    artistsMap.put(currFolder,artistsMap.get(currFolder) + "," + playlist.getArtist());
                }
            }
            else {
                foldersMap.put(currFolder, 1);
                artistsMap.put(currFolder, "" + playlist.getArtist());
            }
        }
//        arrTracks.clear();


        Collections.sort(allTracks, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                return (lhs.getFolder() + lhs.getArtist()).compareTo(rhs.getFolder() + rhs.getArtist());
            }
        });

        iterator = allTracks.iterator();
        String prevFolder = null;
        while (iterator.hasNext()) {
            Track playlist = iterator.next();

            String currFolder  = playlist.getFolder();
            if (prevFolder == null) {
                prevFolder = currFolder;

            }
            else if (!prevFolder.equals(currFolder)) {
                addToTracks(prevFolder,artistsMap.get(prevFolder));
            }
            playlist.setGroup(artistsMap.get(currFolder));
        }

        addToTracks(prevFolder, artistsMap.get(prevFolder));


        Collections.sort(allTracks, new Comparator<Track>() {
            @Override
            public int compare(Track lhs, Track rhs) {
                return (lhs.getGroup() + lhs.getFolder() + (lhs.getTrack() + 1000)).compareTo(rhs.getGroup() + rhs.getFolder() + (rhs.getTrack() + 1000));
            }
        });


        iterator = allTracks.iterator();
        prevFolder = null;
        while (iterator.hasNext()) {
            Track playlist = iterator.next();
            String currFolder  = playlist.getFolder();
            if (prevFolder == null) {
                prevFolder = currFolder;
            } else if (!prevFolder.equals(currFolder)) {
                addToTracks(prevFolder,artistsMap.get(prevFolder));
            }
            if (foldersMap.get(currFolder)>3) {
                Track track = Track.newInstance(playlist);
                track.setGroup(artistsMap.get(currFolder));
                tmpTracks.add(track);
                prevFolder = currFolder;
                //delete old
                iterator.remove();
            }
        }
        addToTracks(prevFolder, artistsMap.get(prevFolder));

        // Others
        String artistInOthersFolder = "";
        iterator = allTracks.iterator();

        while (iterator.hasNext()) {
            Track playlist = iterator.next();
            Track track = Track.newInstance(playlist);
            track.setGroup(OTHERS);
            tmpTracks.add(track);
            if (playlist.getArtist() != null && !artistInOthersFolder.contains(playlist.getArtist())) {
                if (!artistInOthersFolder.equals("")) {
                    artistInOthersFolder += ", ";
                }
                artistInOthersFolder += playlist.getArtist();
            }
        }
        addToTracks(OTHERS, artistInOthersFolder);

    }

    void addToTracks(String desc, String artists) {
        if (tmpTracks.size() > 0) {
            Collections.sort(tmpTracks, new Comparator<Track>() {
                @Override
                public int compare(Track o1, Track o2) {
                    return (o1.getPath()).compareTo(o2.getPath());
                }
            });

            Playlist playlist = new Playlist();
            playlist.setTitle(desc);
            playlist.setArtists(artists);
            playlist.setTracks(tmpTracks);
            arrTracks.add(playlist);
            tmpTracks = new ArrayList<Track>();
        }
    }

    public ArrayList<Playlist> getArrTracks (){
        return arrTracks;
    }



}
