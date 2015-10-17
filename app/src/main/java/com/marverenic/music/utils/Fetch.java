package com.marverenic.music.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.marverenic.music.Library;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;

public class Fetch {

    private static final String TAG = "Fetch";

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

}
