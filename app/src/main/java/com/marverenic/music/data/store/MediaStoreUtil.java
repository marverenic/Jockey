package com.marverenic.music.data.store;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Util;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;

public final class MediaStoreUtil {

    private static final String AUTO_PLAYLIST_EXTENSION = ".jpl";

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

    private static boolean sAlreadyRequestedPermission = false;

    /**
     * This class is never instantiated
     */
    private MediaStoreUtil() {
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Util.hasPermissions(context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static Observable<Boolean> getPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Observable.just(true);
        } else if (sAlreadyRequestedPermission) {
            return Observable.just(hasPermission(context));
        } else {
            return RxPermissions.getInstance(context).request(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
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

    public static List<Artist> getAllArtists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                ARTIST_PROJECTION,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC");

        if (cur == null) {
            return new ArrayList<>();
        }

        List<Artist> artists = Artist.buildArtistList(cur, context.getResources());
        cur.close();

        return artists;
    }

    public static List<Genre> getAllGenres(Context context) {
        List<Genre> genres = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                GENRE_PROJECTION,
                null,
                null,
                MediaStore.Audio.Genres.NAME + " ASC");

        if (cur == null) {
            return genres;
        }

        genres = Genre.buildGenreList(context, cur);
        cur.close();

        return genres;
    }

    public static List<Playlist> getAllPlaylists(Context context) {
        List<Playlist> playlists = getMediaStorePlaylists(context);

        for (Playlist p : getAutoPlaylists(context)) {
            if (playlists.remove(p)) {
                playlists.add(p);
            } else {
                // If AutoPlaylists have been deleted outside of Jockey, delete its configuration
                //noinspection ResultOfMethodCallIgnored
                new File(context.getExternalFilesDir(null)
                        + "/" + p.getPlaylistName() + AUTO_PLAYLIST_EXTENSION)
                        .delete();
            }
        }

        return playlists;
    }

    public static List<Playlist> getMediaStorePlaylists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                PLAYLIST_PROJECTION,
                null,
                null,
                MediaStore.Audio.Playlists.NAME + " ASC");

        if (cur == null) {
            return new ArrayList<>();
        }

        List<Playlist> playlists = Playlist.buildPlaylistList(cur);
        cur.close();

        return playlists;
    }

    public static List<AutoPlaylist> getAutoPlaylists(Context context) {
        List<AutoPlaylist> autoPlaylists = new ArrayList<>();
        Gson gson = new Gson();

        try {
            File externalFiles = new File(context.getExternalFilesDir(null) + "/");

            if (externalFiles.exists() || externalFiles.mkdirs()) {
                String[] files = externalFiles.list();
                for (String s : files) {
                    if (s.endsWith(AUTO_PLAYLIST_EXTENSION)) {
                        autoPlaylists.add(gson.fromJson(
                                new FileReader(externalFiles + "/" + s),
                                AutoPlaylist.class));
                    }
                }
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
        }

        return autoPlaylists;
    }

    public static List<Song> getAlbumSongs(Context context, Album album) {
        return getAlbumSongs(context, album.getAlbumId());
    }

    public static List<Song> getAlbumSongs(Context context, long albumId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC + " != 0 "
                        + " AND " + MediaStore.Audio.AlbumColumns.ALBUM_ID + " = " + albumId,
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        cur.close();

        return songs;
    }

    public static List<Song> getArtistSongs(Context context, Artist artist) {
        return getArtistSongs(context, artist.getArtistId());
    }

    public static List<Song> getArtistSongs(Context context, long artistId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC  + " != 0 "
                        + " AND " + MediaStore.Audio.AudioColumns.ARTIST_ID + " = " + artistId,
                null, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        Collections.sort(songs);
        cur.close();

        return songs;
    }

    public static List<Album> getArtistAlbums(Context context, Artist artist) {
        return getArtistAlbums(context, artist.getArtistId());
    }

    public static List<Album> getArtistAlbums(Context context, long artistId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION,
                MediaStore.Audio.AudioColumns.ARTIST_ID + " = " + artistId,
                null, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Album> albums = Album.buildAlbumList(cur, context.getResources());
        Collections.sort(albums);
        cur.close();

        return albums;
    }

    public static List<Song> getGenreSongs(Context context, Genre genre) {
        return getGenreSongs(context, genre.getGenreId());
    }

    public static List<Song> getGenreSongs(Context context, long genreId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                SONG_PROJECTION,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        cur.close();

        return songs;
    }

}
