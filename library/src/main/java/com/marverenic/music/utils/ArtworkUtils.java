package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.marverenic.music.library.R;
import com.marverenic.music.model.Song;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import timber.log.Timber;

public class ArtworkUtils {

    private static final List<String> COVER_IMAGE_NAMES = Arrays.asList(
            "cover.jpg",
            "folder.jpg"
    );

    @Nullable
    private static byte[] fetchEmbeddedArtwork(Context context, Uri songLocation) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, songLocation);
            return retriever.getEmbeddedPicture();
        } catch (RuntimeException e) {
            Timber.e(e, "Failed to load full song artwork");
        } catch (OutOfMemoryError e) {
            Timber.e(e, "Unable to allocate space on the heap for full song artwork");
        } finally {
            retriever.release();
        }

        return null;
    }

    @Nullable
    private static File resolveArtwork(Context context, Uri songLocation) {
        String path = UriUtils.getPathFromUri(context, songLocation);
        if (path == null) {
            return null;
        }

        File directory = new File(path).getParentFile();
        for (String cover : COVER_IMAGE_NAMES) {
            File image = new File(directory, cover);
            if (image.canRead()) {
                return image;
            }
        }

        return null;
    }

    @Nullable
    private static String resolveMediaStoreArtwork(Context context, Uri songLocation) {
        Cursor songCursor = null;
        Cursor albumCursor = null;
        try {
            String[] songProjection = new String[] { MediaStore.Audio.AudioColumns.ALBUM_ID };

            songCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songProjection, MediaStore.Audio.AudioColumns.DATA + " = ?",
                    new String[] { songLocation.getPath() }, null);

            if (songCursor == null || !songCursor.moveToFirst()) {
                return null;
            }

            long albumId = songCursor.getLong(0);

            String[] albumProjection = new String[] { MediaStore.Audio.AlbumColumns.ALBUM_ART };
            albumCursor = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumProjection,
                    MediaStore.Audio.AudioColumns._ID + " = " + albumId, null, null);

            if (albumCursor == null || !albumCursor.moveToFirst()) {
                return null;
            }

            return albumCursor.getString(0);
        } finally {
            if (songCursor != null) {
                songCursor.close();
            }
            if (albumCursor != null) {
                albumCursor.close();
            }
        }
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Song song) {
        if (song == null) {
            return fetchArtwork(context, (Uri) null);
        } else {
            return fetchArtwork(context, song.getLocation());
        }
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Uri songLocation) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int size = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        return fetchArtwork(context, songLocation, size, true);
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Uri songLocation,
                                                  int size, boolean highQuality) {
        return Observable.fromCallable(() -> {
            if (songLocation == null) {
                return Glide.with(context).load(R.drawable.art_default_xl);
            }

            byte[] embedded = fetchEmbeddedArtwork(context, songLocation);
            if (embedded != null){
                return Glide.with(context).load(embedded);
            }

            File folderImage = resolveArtwork(context, songLocation);
            if (folderImage != null) {
                return Glide.with(context).load(folderImage);
            }

            String mediaStoreThumbnail = resolveMediaStoreArtwork(context, songLocation);
            if (mediaStoreThumbnail != null) {
                return Glide.with(context).load(mediaStoreThumbnail);
            }

            return Glide.with(context).load(R.drawable.art_default_xl);
        }).map(request -> {
            try {
                return request.asBitmap()
                        .format((highQuality)
                                ? DecodeFormat.PREFER_ARGB_8888
                                : DecodeFormat.PREFER_RGB_565)
                        .atMost()
                        .into(size, size)
                        .get();
            } catch (InterruptedException| ExecutionException e) {
                Timber.e(e, "Failed to load artwork");
                return BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.art_default_xl);
            }
        });
    }

}
