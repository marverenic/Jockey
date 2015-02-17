package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;

import com.marverenic.music.BuildConfig;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.cache.FileSystemCache;

public class Fetch {

    private static final String TAG = "Fetch";
    // API key for Last.fm. Please use your own.
    private static final String API_KEY = "a9fc65293034b84b83d20c6e2ecda4b5";
    private static boolean lastFmInitialized = false;

    public static void initLastFm() {
        Caller.getInstance().setCache(new FileSystemCache(new File(Environment.getExternalStorageDirectory() + "/.lastfm")));
        Caller.getInstance().setUserAgent("Jockey/" + BuildConfig.VERSION_NAME);

        lastFmInitialized = true;
    }

    public static Bitmap fetchAlbumArtLocal(Context context, String albumId) {
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

    public static ArtistBio fetchArtistBio(Context context, String artistName) {
        if (!lastFmInitialized) initLastFm();

        Artist artist = Artist.getInfo(artistName, API_KEY);
        if (artist != null) {
            try {
                Bitmap art = ImageLoader.getInstance().loadImageSync(artist.getImageURL(ImageSize.MEGA));
                String summary = Html.fromHtml(artist.getWikiSummary()).toString();
                // This probably violates something in the Last.fm API license
                if (summary.length() > 0)
                    summary = summary.substring(0, summary.length() - " Read more about  on Last.fm.".length() - artist.getName().length() - 1);
                String[] tags;
                if (artist.getTags().size() > 0) {
                    tags = artist.getTags().toArray(new String[artist.getTags().size()]);
                } else {
                    tags = new String[]{""};
                }
                return new ArtistBio(art, summary, tags);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Debug.log(Debug.VERBOSE, TAG, "Unable to find the artist " + artistName, context);
        }
        return null;
    }

    public static class ArtistBio {
        public Bitmap art;
        public String summary;
        public String[] tags;

        public ArtistBio(Bitmap art, String summary, String[] tags) {
            this.art = art;
            this.summary = summary;
            this.tags = tags;
        }
    }
}
