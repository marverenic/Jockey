package com.marverenic.music.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.util.Log;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;

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

    @Deprecated
    public static void initImageCache (Context context) {
        // Currently unused
    }

    public static void initLastFm(Context context) {
        Caller.getInstance().setCache(new FileSystemCache(new File(context.getExternalCacheDir() + "/lastfm/")));
        Caller.getInstance().setUserAgent("Jockey/" + BuildConfig.VERSION_NAME);

        lastFmInitialized = true;
    }

    // Returns the album art thumbnail from MediaStore
    public static Bitmap fetchAlbumArtLocal(long albumId) {
        Album album = LibraryScanner.findAlbumById(albumId);
        if (album.artUri != null) {
            return BitmapFactory.decodeFile(album.artUri);
        }
        return null;
    }

    // Loads the full resolution art into memory
    public static void fetchFullResolutionArt(final Song song, final Context context, final fullResolutionArtCallback callback){
        if (callback == null){
            Log.w(TAG, "No callback was provided. Aborting fetchFullResolutionArt");
            return;
        }

        new AsyncTask<Void, Void, Void>(){
            Bitmap art;

            @Override
            protected Void doInBackground(Void... voids) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                art = null;
                try {
                    retriever.setDataSource(song.location);
                    byte[] stream = retriever.getEmbeddedPicture();
                    if (stream != null) art = BitmapFactory.decodeByteArray(stream, 0, stream.length);
                }
                catch (Exception e){
                    e.printStackTrace();
                    return null;
                }

                if (art != null) {
                    // Resize the art so that it is no taller than the device's screen
                    // and preserve its original aspect ratio
                    final int displayHeight = context.getResources().getDisplayMetrics().heightPixels;
                    if (art.getHeight() > displayHeight) {
                        art = Bitmap.createScaledBitmap(art, (int) (displayHeight * ((float) art.getWidth() / (float) art.getHeight())), displayHeight, true);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v){
                if (art != null) {
                    // Then do whatever the method wanted to do with this bitmap
                    callback.onArtFetched(art);
                }
            }
        }.execute();
    }

    // Because fetching full resolution art can be slow, do it
    public interface fullResolutionArtCallback{
        public void onArtFetched(Bitmap art);
    }

    public static void buildAlbumPalette (Bitmap bitmap, int defaultPrimary, int defaultTitleText,
                                          int defaultBodyText, Album album){

        Palette palette = Palette.generate(bitmap);

        int primary = defaultPrimary;
        int titleText = defaultTitleText;
        int bodyText = defaultBodyText;

        if (palette.getVibrantSwatch() != null && palette.getVibrantColor(-1) != -1) {
            primary = palette.getVibrantColor(0);
            titleText = palette.getVibrantSwatch().getTitleTextColor();
            bodyText = palette.getVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightVibrantSwatch() != null && palette.getLightVibrantColor(-1) != -1) {
            primary = palette.getLightVibrantColor(0);
            titleText = palette.getLightVibrantSwatch().getTitleTextColor();
            bodyText = palette.getLightVibrantSwatch().getBodyTextColor();
        } else if (palette.getDarkVibrantSwatch() != null && palette.getDarkVibrantColor(-1) != -1) {
            primary = palette.getDarkVibrantColor(0);
            titleText = palette.getDarkVibrantSwatch().getTitleTextColor();
            bodyText = palette.getDarkVibrantSwatch().getBodyTextColor();
        } else if (palette.getLightMutedSwatch() != null && palette.getLightMutedColor(-1) != -1) {
            primary = palette.getLightMutedColor(0);
            titleText = palette.getLightMutedSwatch().getTitleTextColor();
            bodyText = palette.getLightMutedSwatch().getBodyTextColor();
        } else if (palette.getDarkMutedSwatch() != null && palette.getDarkMutedColor(-1) != -1) {
            primary = palette.getDarkMutedColor(0);
            titleText = palette.getDarkMutedSwatch().getTitleTextColor();
            bodyText = palette.getDarkMutedSwatch().getBodyTextColor();
        }

        album.artPrimaryPalette = primary;
        album.artPrimaryTextPalette = titleText;
        album.artDetailTextPalette = bodyText;
    }

    public static ArtistBio fetchArtistBio(Context context, String artistName) {
        if (!lastFmInitialized) initLastFm(context);

        Artist artist = Artist.getInfo(artistName, API_KEY);
        if (artist != null) {
            try {
                initImageCache(context);

                String art = artist.getImageURL(ImageSize.MEGA);
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
            Debug.log(Debug.LogLevel.INFO, TAG, "Unable to find the artist " + artistName, context);
        }
        return null;
    }

    public static class ArtistBio {
        public String artURL;
        public String summary;
        public String[] tags;

        public ArtistBio(String artURL, String summary, String[] tags) {
            this.artURL = artURL;
            this.summary = summary;
            this.tags = tags;
        }
    }
}
