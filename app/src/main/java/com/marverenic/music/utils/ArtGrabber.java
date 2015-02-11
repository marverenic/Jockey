package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;

import com.marverenic.music.BuildConfig;

import java.io.File;
import java.net.URL;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.cache.FileSystemCache;

public class ArtGrabber {

    private static final String TAG = "ArtGrabber";
    // API key for Last.fm. Please use your own.
    private static final String API_KEY = "a9fc65293034b84b83d20c6e2ecda4b5";
    private static boolean lastFmInitialized = false;

    public static void initLastFm() {
        Caller.getInstance().setCache(new FileSystemCache(new File(Environment.getExternalStorageDirectory() + "/.lastfm")));
        Caller.getInstance().setUserAgent("Jockey/" + BuildConfig.VERSION_NAME);

        lastFmInitialized = true;
    }

    public static Bitmap grabAlbumArtLocal(Context context, String albumId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{String.valueOf(albumId)},
                null);
        cur.moveToFirst();
        String location = cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        cur.close();
        if (location != null) {
            return BitmapFactory.decodeFile(location);
        }
        return null;
    }

    public static Bitmap grabArtistArt(Context context, String artistName) {
        if (!lastFmInitialized) initLastFm();

        Artist artist = Artist.getInfo(artistName, API_KEY);
        if (artist != null) {
            try {
                URL url = new URL(artist.getImageURL(ImageSize.MEGA));
                return BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Debug.log(Debug.VERBOSE, TAG, "Unable to find the artist " + artistName, context);
        }
        return null;
    }
}
