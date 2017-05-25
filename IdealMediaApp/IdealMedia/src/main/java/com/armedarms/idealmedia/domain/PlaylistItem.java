package com.armedarms.idealmedia.domain;

public class PlaylistItem {
    public enum ItemType {
        TITLE,
        TRACK,
        AD,
        MESSAGE_BECOME_PREMIUM,
        MESSAGE_EMPTY,
        MESSAGE_NOT_LOGGED_IN,
    }

    public int index;
    public ItemType type;
    public Object data;

    public PlaylistItem(ItemType type) {
        this.type = type;
    }

    public static PlaylistItem title(String title) {
        PlaylistItem pi = new PlaylistItem(ItemType.TITLE);

        pi.data = title;

        return pi;
    }

    public static PlaylistItem track(int index, Track track) {
        PlaylistItem pi = new PlaylistItem(ItemType.TRACK);

        pi.index = index;
        pi.data = track;

        return pi;
    }

    public static PlaylistItem ad() {
        return new PlaylistItem(ItemType.AD);
    }

    public static PlaylistItem messageEmpty() {
        return new PlaylistItem(ItemType.MESSAGE_EMPTY);
    }

}
