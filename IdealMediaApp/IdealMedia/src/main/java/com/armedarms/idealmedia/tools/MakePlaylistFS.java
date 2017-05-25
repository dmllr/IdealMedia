package com.armedarms.idealmedia.tools;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.androidquery.util.AQUtility;
import com.armedarms.idealmedia.BuildConfig;
import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Track;
import com.armedarms.idealmedia.utils.FileUtils;
import com.armedarms.idealmedia.utils.FillMediaStoreTracks;
import com.armedarms.idealmedia.utils.StringUtils;
import com.un4seen.bass.BASS;
import com.un4seen.bass.TAGS;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class MakePlaylistFS extends MakePlaylistAbstract {

    private ArrayList<Track> tempAllTracks,tempAllTracksMediaStore;
    private boolean refresh;

    //encoding detector
    UniversalDetector detector;

    public MakePlaylistFS(Context context, boolean refresh) {
        super(context,refresh);
    }

    @Override
    public void getAllTracks(Context context, boolean refresh) {
        this.refresh = refresh;
        t = System.currentTimeMillis();
        String scanDir = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.key_mediapath), Environment.getExternalStorageDirectory().getAbsolutePath());
        File currentDir = new File(scanDir);

        tempAllTracks = (ArrayList<Track>) FileUtils.read("alltracksfs", context);
        tempAllTracksMediaStore = (ArrayList<Track>) FileUtils.read("alltracksms", context);

        if (refresh || tempAllTracksMediaStore == null || tempAllTracksMediaStore.size()==0) {
            tempAllTracksMediaStore =  new FillMediaStoreTracks(context).getTracks();
        }

        if (!refresh && tempAllTracks != null && tempAllTracks.size() > 0) {
            allTracks = new ArrayList<Track>(tempAllTracks);
        }
        else {
            if (refresh) {
                if (BASS.BASS_Init(-1, 44100, 0)) {
                    String nativePath = context.getApplicationInfo().nativeLibraryDir;
                    String[] listPlugins = new File(nativePath).list();
                    for (String s : listPlugins) {
                        BASS.BASS_PluginLoad(nativePath + "/" + s, 0);
                    }
                }
                detector = new UniversalDetector(null);

                walk(currentDir);

                FileUtils.write("alltracksfs", context, allTracks);
            } else {
                allTracks = new ArrayList<Track>(tempAllTracksMediaStore);
            }
        }
    }

    public void walk(File root) {

        if (BuildConfig.DEBUG)
            Log.d("WALK", root.getAbsolutePath());

        File[] list = null;

        if (root.getAbsolutePath().equals("/")) {
            String[] dirs = FileUtils.getStorageDirectories();
            list = new File[dirs.length];
            for (int i = 0; i < dirs.length; i++)
                list[i] = new File(dirs[i]);
        }
        else {
            list = root.listFiles();
        }

        if (list == null)
            return;

        int chan = 0;
        for (File f : list) {

            if (f.isDirectory()) {
                walk(f);
            }
            else {
                String path = f.getAbsolutePath();

                int lengthPath = path.length();
                if (lengthPath < 4)
                    continue;
                String endOfPath = path.substring(lengthPath-4).toLowerCase();
                if (endOfPath.equals(".mp3")
                        || endOfPath.equals("flac") || endOfPath.equals(".ogg")
                        || endOfPath.equals(".oga") || endOfPath.equals(".aac")
                        || endOfPath.equals(".m4a") || endOfPath.equals(".m4b")
                        || endOfPath.equals(".m4p") || endOfPath.equals("opus")
                        || endOfPath.equals(".wma") || endOfPath.equals(".wav")
                        || endOfPath.equals(".mpc") || endOfPath.equals(".ape")

                        ) {
                    if (!this.refresh && tempAllTracks !=null && tempAllTracks.size() > 0) {
                        Track track = null;
                        for (Track t: tempAllTracks) {
                            if (t.getPath().equals(path)) {
                                track = t;
                                break;
                            }
                        }
                        if (track!=null) {
                            allTracks.add(track);
                            continue;
                        }
                    }

                    String folder = "";
                    String[] pathArray = path.split(
                        TextUtils.equals(System.getProperty("file.separator"), "") ? "/" : System.getProperty("file.separator")
                    );

                    if (pathArray.length == 0)
                        continue;

                    if (pathArray.length > 1)
                        folder = pathArray[pathArray.length-2];

                    long lastModified = f.lastModified();

                    BASS.BASS_StreamFree(chan);
                    chan = BASS.BASS_StreamCreateFile(path, 0, 0, 0);
                    //check base tags and get encoding
                    String tags = null;
                    if (android.os.Build.VERSION.SDK_INT >=9) {
                        for (int format = 0; format < Track.FORMATS.length; format++) {
                            final ByteBuffer byteBuffer = TAGS.TAGS_ReadExByte(chan, "%ARTI@%YEAR@%TRCK@%TITL@%ALBM@%COMP" + " ", Track.FORMATS[format]);

                            final int bufferSize = byteBuffer.capacity();
                            if (bufferSize < 10) {
                                //so if no tags it return something strange, like this "??" - skip it for optimization
                                continue;
                            }
                            //byteBuffer dont have array (direct access?), so copy it
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
                            if (wrongencoding) {
                                continue;
                            }
                            if (!TextUtils.isEmpty(tags)) {
                                if (tags.split("@").length >= 4) {
                                    break;
                                }
                            }
                        }
                    }
                    if (TextUtils.isEmpty(tags)) {
                        //it may have tags from http forexample so handle it with default way (utf8 encoding)
                        tags = TAGS.TAGS_Read(chan, "%UTF8(%ARTI)@%YEAR@%TRCK@%UTF8(%TITL)@%UTF8(%ALBM)@%UTF8(%COMP)" + " ");
                    }

                    if (TextUtils.isEmpty(tags)) {
                        //continue;
                    }

                    String[] tagsArray = tags.split("@");

                    int duration = 0;
                    int albumId = 0;
                    if (tempAllTracksMediaStore !=null && tempAllTracksMediaStore.size()>0) {
                        Track track = null;
                        for (Track t: tempAllTracksMediaStore) {
                            if (t.getPath().equals(path)) {
                                duration = t.getDuration();
                                albumId = t.getAlbumId();
                                break;
                            }
                        }
                    }
                    if (duration == 0) {
                        duration = (int) (0.5d+BASS.BASS_ChannelBytes2Seconds(chan, BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE)));
                    }

                    if (tagsArray.length > 5) {
                        add2list(tagsArray[0], tagsArray[1], tagsArray[2], tagsArray[3], tagsArray[4], tagsArray[5].trim(),
                                path, folder, lastModified, pathArray[pathArray.length - 1], duration, albumId);
                    } else {
                        add2list("Unknown", "", "", pathArray[pathArray.length - 1], "", "",
                                path, folder, lastModified, pathArray[pathArray.length - 1], duration, albumId);
                    }
                }
            }
        }
    }

    public void add2list(String artist,String yearS,String trackS,String title,String album,String composer,
                         String path,String folder, long lastModified,String filename,int duration,int albumId){
        int year = 0;
        int track = 0;

        try {
            if (!yearS.equals("")) {
                if (yearS.length()>3) {
                    yearS = yearS.substring(0,4);
                }
                year  = Integer.parseInt(yearS.replaceAll("[^\\d.]", ""));
            }

            if (!trackS.equals(""))
                track = Integer.parseInt(trackS.replaceAll("[^\\d.]", ""));
        } catch (Exception e) {AQUtility.debug(e.toString());}

        allTracks.add(new Track(
                artist.equals("") ?"unknown": StringUtils.capitalizeFully(artist),
                title.equals("") ?filename:StringUtils.capitalizeFully(title),
                album,
                composer,
                year,
                track,
                duration,
                path,
                folder,
                lastModified,
                albumId));
    }
}
