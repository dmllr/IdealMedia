package com.armedarms.idealmedia.utils;

import android.text.TextUtils;

import com.armedarms.idealmedia.domain.Track;

public class StringUtils {

    public static String capitalizeFully(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        String [] s = TextUtils.split(str, " ");
        if (s.length == 0) {
            return capitalize(str);
        }
        else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String ss:s){
                stringBuilder.append(capitalize(ss));
                stringBuilder.append(" ");
            }
            return stringBuilder.toString();
        }
    }

    public static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuilder(strLen)
                .append(Character.toTitleCase(str.charAt(0)))
                .append(str.substring(1).toLowerCase())
                .toString();
    }

    public static String getFileName(Track track, boolean withAlbum) {

        final String filename = (track.getArtist() != null ? track.getArtist().toLowerCase().trim() : "unknown") + (withAlbum && track.getAlbum() != null ? ("_" + track.getAlbum().toLowerCase().trim()) : "");
        StringBuilder builder = new StringBuilder();
        for (char c : filename.toCharArray()) {
            if (Character.isJavaIdentifierPart(c)) {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}
