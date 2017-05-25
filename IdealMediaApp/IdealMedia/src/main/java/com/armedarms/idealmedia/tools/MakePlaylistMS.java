package com.armedarms.idealmedia.tools;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.armedarms.idealmedia.domain.Track;

import java.io.File;

public class MakePlaylistMS extends MakePlaylistAbstract {

    public MakePlaylistMS(Context context, boolean refresh) {
        super(context,refresh);
    }

    @Override
    public void getAllTracks(Context context,boolean refresh) {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.COMPOSER,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        if (cursor == null)
            return;

        t = System.currentTimeMillis();

        while(cursor.moveToNext()) {
            try {
                String folder = "";
                String path = cursor.getString(7);
                String[] pathArray = path.split(
                        TextUtils.equals(System.getProperty("file.separator"), "")?"/":System.getProperty("file.separator")
                );
                if (pathArray.length > 1) {
                    folder = pathArray[pathArray.length-2];
                }

                allTracks.add(new Track(cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4),
                        cursor.getInt(5),
                        cursor.getInt(6)/1000,
                        cursor.getString(7),
                        folder,
                        new File(path).lastModified(),
                        cursor.getInt(8)
                ));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        cursor.close();
    }
}
