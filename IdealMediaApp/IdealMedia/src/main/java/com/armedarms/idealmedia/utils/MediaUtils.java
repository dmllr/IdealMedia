package com.armedarms.idealmedia.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.domain.Track;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaUtils {

    public static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    public static final String ALBUM_FOLDER = "albumthumbs";
    private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();


    public static Bitmap getArtworkQuick(Context context, Track track, int w, int h) {
        // NOTE: There is in fact a 1 pixel frame in the ImageView used to
        // display this drawable. Take it into account now, so we don't have to
        // scale later.
        Bitmap b = null;
        if (track == null)
            return null;

        String path = MediaUtils.getAlbumPath(track);
        if (path != null && new File(path).exists()) {
            File file = new File(path);
            if (file.exists()) {
                b = getBitmap(context, file, null, w, h);
            }
        } else {
            final int album_id = track.getAlbumId();
            if (album_id != 0) {
                Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
                if (uri != null) {
                    b = getBitmap(context, null, uri,  w,  h);
                }
                else {
                    b = getArtistQuick(context,track,w,h);
                }
            }
            else {
                b = getArtistQuick(context,track,w,h);
            }
        }
        return b;
    }

    public static Bitmap getArtistQuick(Context context, Track track, int w, int h) {
        Bitmap b = null;

        if (track == null)
            return null;

        String path = MediaUtils.getArtistPath(track);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                b = getBitmap(context, file, null, w, h);
            }
        }

        return b;
    }

    public static Bitmap getBitmap(Context context, File file,Uri uri, int w, int h) {
        ParcelFileDescriptor fd = null;
        Bitmap b = null;
        try {
            if (file != null) {
                fd = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                ContentResolver res = context.getContentResolver();
                fd = res.openFileDescriptor(uri, "r");
            }

            int sampleSize = 1;

            // Compute the closest power-of-two scale factor
            // and pass that to sBitmapOptionsCache.inSampleSize, which will
            // result in faster decoding and better quality
            sBitmapOptionsCache.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);
            int nextWidth = sBitmapOptionsCache.outWidth >> 1;
            int nextHeight = sBitmapOptionsCache.outHeight >> 1;
            while (nextWidth > w && nextHeight>h) {
                sampleSize <<= 1;
                nextWidth >>= 1;
                nextHeight >>= 1;
            }

            sBitmapOptionsCache.inSampleSize = sampleSize;
            sBitmapOptionsCache.inJustDecodeBounds = false;
            b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);

            if (b != null) {
                // finally rescale to exactly the size we need
                if (sBitmapOptionsCache.outWidth != w || sBitmapOptionsCache.outHeight != h) {
                    Bitmap tmp = Bitmap.createBitmap(b);
                    if (tmp != b) {
                        b.recycle();
                    }
                    b = tmp;
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            try {
                if (fd != null)
                    fd.close();
            } catch (IOException e) {
            }
        }
        return b;
    }

    public static Bitmap fastblur(Bitmap sentBitmap, int radius) {
        try {
            Bitmap bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true);

            if (radius < 1) {
                return (null);
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            int[] pix = new int[w * h];

            bitmap.getPixels(pix, 0, w, 0, 0, w, h);

            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;

            int r[] = new int[wh];
            int g[] = new int[wh];
            int b[] = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int vmin[] = new int[Math.max(w, h)];

            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int dv[] = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }

            yw = yi = 0;

            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {

                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;

                    sir = stack[i + radius];

                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];

                    rbs = r1 - Math.abs(i);

                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;

                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi += w;
                }
            }

            bitmap.setPixels(pix, 0, w, 0, 0, w, h);

            return (bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return sentBitmap;
        }
    }

    public static void setRingtone(Context context,Track track)
    {

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, track.getPath());
        values.put(MediaStore.MediaColumns.TITLE, track.getTitle());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.Media.ARTIST, track.getArtist());
        values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.Media.IS_ALARM, false);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(track.getPath());

        if (context.getContentResolver() == null)
        {
            Toast.makeText(context, context.getString(R.string.set_as_ringtone_error), Toast.LENGTH_SHORT).show();
            return;
        }

        context.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + track.getPath() + "\"", null);
        Uri newUri = context.getContentResolver().insert(uri, values);

        if (newUri == null) {
            Toast.makeText(context, context.getString(R.string.set_as_ringtone_error), Toast.LENGTH_SHORT).show();
        } else {
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText(context,context.getString(R.string.set_as_ringtone),Toast.LENGTH_SHORT).show();
        }
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
                               String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }

    }

    public static String getAlbumPath (Track track,boolean withAlbum) {
        final String directoryPath = FileUtils.getSdCardPath() + ALBUM_FOLDER;
        File directory = new File(directoryPath);
        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdirs();
        }
        if (!success) {
            return null;
        }
        else {
            return directoryPath + "/" + StringUtils.getFileName(track, withAlbum) + ".jpg";
        }
    }

    public static String getAlbumPath (Track track) {
        return getAlbumPath(track, true);
    }

    public static String getArtistPath (Track track) {
        return getAlbumPath(track,false);
    }

    public static void nomedia() {
        final String directoryPath = FileUtils.getSdCardPath() + ALBUM_FOLDER;
        File directory = new File(directoryPath);
        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdirs();
        }

        if (!success) {
            return;
        }
        else {
            File nomediaFile = new File(directory, ".nomedia");
            try {
                nomediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getBlurredEdgesBitmap(Bitmap source, int reqWidth, int reqHeight){
        if (source == null)
            return null;

        int w = source.getWidth();
        int h = source.getHeight();

        if (w > h) {
            // landscape
            float ratio = (float)w / reqWidth;
            w = reqWidth;
            h = (int)(h / ratio);
        } else if (h > w) {
            // portrait
            float ratio = (float)h / reqHeight;
            h = reqHeight;
            w = (int)(w / ratio);
        } else {
            // square
            h = reqHeight;
            w = reqWidth;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(source, w, h, true);

        w = scaled.getWidth();
        h = scaled.getHeight();

        final int gradientHeight = h / 10;

        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(overlay);
        final Paint paint = new Paint();

        canvas.drawBitmap(scaled, 0, 0, null);

        Shader shader = new LinearGradient(
                0, 0, 0, gradientHeight,
                0x00FFFFFF, 0xFFFFFFFF,
                Shader.TileMode.CLAMP
        );

        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        canvas.drawRect(0, 0, w, gradientHeight, paint);

        return overlay;
    }

    public static Bitmap modifyTrackCellArtworkBitmap(Bitmap source){
        if (source == null)
            return null;

        int w = source.getWidth();
        int h = source.getHeight();

        Bitmap scaled = Bitmap.createScaledBitmap(source, w, h, true);

        w = scaled.getWidth();
        h = scaled.getHeight();

        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(overlay);
        final Paint paint = new Paint();

        canvas.drawBitmap(scaled, 0, 0, null);

        Shader shader = new LinearGradient(
                0, 0, h, 0,
                0x00FFFFFF, 0x77FFFFFF,
                Shader.TileMode.CLAMP
        );

        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        canvas.drawRect(0, 0, w, h, paint);

        return overlay;
    }

    public static Bitmap getTrackCellArtwork(Context context, Track track) {
        String path = MediaUtils.getCellArtworkPath(track);

        Bitmap bitmap = null;

        if (path != null) {
            File file = new File(path);
            if (file.exists())
                bitmap = getBitmap(context, file, null, 160, 90);
            else {
                Bitmap fullArtwork = getArtworkQuick(context, track, 160, 90);
                if (fullArtwork != null) {
                    bitmap = cropBitmapCenter(fullArtwork, (int)(80 * context.getResources().getDisplayMetrics().density));
                    bitmap = modifyTrackCellArtworkBitmap(bitmap);

                    if (bitmap != null)
                        saveBitmap(path, bitmap);
                }
            }
        }

        return bitmap;
    }

    public static void saveBitmap(String path, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Bitmap cropBitmapCenter(Bitmap source, int height) {
        int y = source.getHeight() / 2 - height / 2;

        if (y <= 0)
            return null;

        return Bitmap.createBitmap(
                source,
                0, y,
                source.getWidth(), height
        );
    }

    private static String getCellArtworkPath(Track track) {
        if (track == null)
            return null;

        final String directoryPath = FileUtils.getSdCardPath() + ALBUM_FOLDER;
        File directory = new File(directoryPath);
        boolean success = true;
        if (!directory.exists())
            success = directory.mkdirs();
        if (!success)
            return null;

        return directoryPath + "/small_" + StringUtils.getFileName(track, true) + ".png";
    }
}
