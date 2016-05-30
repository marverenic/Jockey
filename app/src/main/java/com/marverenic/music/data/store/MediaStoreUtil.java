package com.marverenic.music.data.store;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.List;

public final class MediaStoreUtil {

    // This value is hardcoded into Android's sqlite implementation. If a query exceeds this many
    // variables, an SQLiteException will be thrown, so when doing long queries be sure to check
    // against this value. This value is defined as SQLITE_MAX_VARIABLE_NUMBER in
    // https://raw.githubusercontent.com/android/platform_external_sqlite/master/dist/sqlite3.c
    private static final int SQL_MAX_VARS = 999;

    private static final String[] SONG_PROJECTION = new String[]{
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TRACK
    };

    private static final String[] ARTIST_PROJECTION = new String[]{
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
    };

    private static final String[] ALBUM_PROJECTION = new String[]{
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR,
            MediaStore.Audio.Albums.ALBUM_ART
    };

    private static final String[] PLAYLIST_PROJECTION = new String[]{
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    private static final String[] GENRE_PROJECTION = new String[]{
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    private static final String[] PLAYLIST_ENTRY_PROJECTION = new String[]{
            MediaStore.Audio.Playlists.Members.TITLE,
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members.ARTIST,
            MediaStore.Audio.Playlists.Members.ALBUM,
            MediaStore.Audio.Playlists.Members.DURATION,
            MediaStore.Audio.Playlists.Members.DATA,
            MediaStore.Audio.Playlists.Members.YEAR,
            MediaStore.Audio.Playlists.Members.DATE_ADDED,
            MediaStore.Audio.Playlists.Members.ALBUM_ID,
            MediaStore.Audio.Playlists.Members.ARTIST_ID,
            MediaStore.Audio.Playlists.Members.TRACK
    };

    /**
     * This class is never instantiated
     */
    private MediaStoreUtil() {
    }

    public static List<Song> getAllSongs(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cur == null) {
            return new ArrayList<>();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        cur.close();

        return songs;
    }

    public static List<Album> getAllAlbums(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC");

        if (cur == null) {
            return new ArrayList<>();
        }

        List<Album> albums = Album.buildAlbumList(cur, context.getResources());
        cur.close();

        return albums;
    }

}
