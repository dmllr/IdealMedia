package com.armedarms.idealmedia.domain;

import android.net.Uri;
import android.text.TextUtils;

import com.un4seen.bass.BASS;
import com.un4seen.bass.TAGS;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Track implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String artist;
    private String title;
    private String album;
    private String composer;
    private int year;
    private int track;
    private int duration;
    private String path;
    private String folder;
    private long lastModified;
    private String group;
    private boolean selected;
    private int albumId;
    private String url;

    public boolean isDownloadInAction;
    public long downloadID;

    public static final int[] FORMATS = {BASS.BASS_TAG_ID3V2, BASS.BASS_TAG_OGG, BASS.BASS_TAG_APE, BASS.BASS_TAG_MP4, BASS.BASS_TAG_ID3};

    public Track() {
    }

    public Track(String artist, String title, String album, String composer, int year, int track, int duration,
                 String path, String folder, long lastModified, int albumId) {
        this.artist = artist;
        this.title = title;
        this.album = album;
        this.composer = composer;
        this.year = year;
        this.track = track;
        this.duration = duration;
        this.path = path;
        this.folder = folder;
        this.lastModified = lastModified;
        this.group = "";
        this.albumId = albumId;
    }

    public String getDisplay() {
        return String.format("%s - %s", getArtist().trim(), getTitle().trim());
    }

    @Override
    public String toString() {
        return "["+getGroup()+","+getFolder()+","+getTrack()+","+getArtist()+","+getTitle()+"]";
    }
    public static Track newInstance(Track o) {
        return new Track(o.getArtist(), o.getTitle(),o.getAlbum(),o.getComposer(),o.getYear(),o.getTrack(),o.getDuration(),
                o.getPath(),o.getFolder(),o.getLastModified(),o.getAlbumId());
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public static Track fromUri(Uri data) {
        Track t = new Track();

        String path = data.getPath();

        UniversalDetector detector = new UniversalDetector(null);
        int chan = BASS.BASS_StreamCreateFile(path, 0, 0, 0);

        String tags = null;
        for (int format = 0; format < FORMATS.length; format++) {
            final ByteBuffer byteBuffer = TAGS.TAGS_ReadExByte(chan, "%ARTI@%YEAR@%TRCK@%TITL@%ALBM@%COMP" + " ", FORMATS[format]);

            final int bufferSize = byteBuffer.capacity();
            if (bufferSize < 10)
                continue;

            final ByteBuffer frameBuf = ByteBuffer.allocate(bufferSize);
            frameBuf.put(byteBuffer);

            detector.handleData(frameBuf.array(), 0, bufferSize);
            detector.dataEnd();
            final String encoding = detector.getDetectedCharset();
            boolean wrongencoding = false;
            try {
                tags = new String(frameBuf.array(), 0, bufferSize, Charset.forName(encoding));
            } catch (Exception e) {
                wrongencoding = true;
            } finally {
                detector.reset();
            }
            if (wrongencoding)
                continue;
            if (!TextUtils.isEmpty(tags)) {
                if (tags.split("@").length >= 4)
                    return null;
            }
        }

        if (TextUtils.isEmpty(tags))
            tags = TAGS.TAGS_Read(chan, "%UTF8(%ARTI)@%YEAR@%TRCK@%UTF8(%TITL)@%UTF8(%ALBM)@%UTF8(%COMP)" + " ");

        if (TextUtils.isEmpty(tags))
            return null;

        String[] tagsArray = tags.split("@");
        if (tagsArray.length <= 4)
            return null;

        tagsArray = tags.split("@");
        int duration = (int) (0.5d+BASS.BASS_ChannelBytes2Seconds(chan, BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE)));

        t.artist = tagsArray[0];
        t.title = tagsArray[3];
        t.duration = duration;
        t.path = path;

        if (t.title == null || t.title == "")
            t.setTitle(data.getLastPathSegment());

        return t;
    }
}
