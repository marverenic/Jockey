package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;

import java.io.File;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.cache.FileSystemCache;

public class Fetch {

    private static final String TAG = "Fetch";
    // API key for Last.fm. Please use your own.
    private static final String API_KEY = "a9fc65293034b84b83d20c6e2ecda4b5";
    private static boolean lastFmInitialized = false;

    public static void initLastFm(Context context) {
        Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir() + "/lastfm/")));
        Caller.getInstance().setUserAgent("Jockey/" + BuildConfig.VERSION_NAME);

        lastFmInitialized = true;
    }

    // Returns the album art thumbnail from the MediaStore cache
    // Uses the Library loaded in RAM to retrieve the art URI
    public static Bitmap fetchAlbumArt(int albumId, @Nullable Context context) {
        if (!Library.isEmpty()) {
            Album album = Library.findAlbumById(albumId);
            if (album != null && album.artUri != null) {
                return BitmapFactory.decodeFile(album.artUri);
            }
        }
        else if (context == null){
            throw new IllegalArgumentException("Can't resolve album without Context");
        }
        else{
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART
                    },
                    MediaStore.Audio.Albums._ID + " = ?",
                    new String[]{Integer.toString(albumId)},
                    MediaStore.Audio.Albums.ALBUM + " ASC");

            cur.moveToFirst();
            String artURI = cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            cur.close();
            return BitmapFactory.decodeFile(artURI);
        }
        return null;
    }

    public static Bitmap fetchFullArt(Song song){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(song.location);
            byte[] stream = retriever.getEmbeddedPicture();
            if (stream != null)
                return BitmapFactory.decodeByteArray(stream, 0, stream.length);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Artist fetchArtistBio(Context context, String artistName) {
        if (artistName.equalsIgnoreCase(context.getString(R.string.no_artist))
                || artistName.equalsIgnoreCase(context.getString(R.string.various_artists))) return null;
        if (!lastFmInitialized) initLastFm(context);

        // Only get the bio if a valid network is present
        if(Prefs.allowNetwork(context)) {
            return  Artist.getInfo(artistName, API_KEY);
        }
        return null;
    }

}
