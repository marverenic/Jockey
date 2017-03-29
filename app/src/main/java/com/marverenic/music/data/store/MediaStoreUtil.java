package com.marverenic.music.data.store;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;
import com.marverenic.music.utils.Util;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

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

    private static BehaviorSubject<Boolean> sPermissionObservable;

    private static Map<Uri, BehaviorSubject<Boolean>> sContentObservers;
    private static Map<Uri, Integer> sSelfPendingUpdates;

    static {
        sContentObservers = new ArrayMap<>();
        sSelfPendingUpdates = new ArrayMap<>();
    }

    /**
     * This class is never instantiated
     */
    private MediaStoreUtil() {
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Util.hasPermissions(context,
                READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE);
    }

    public static Observable<Boolean> waitForPermission() {
        if (sPermissionObservable == null) {
            sPermissionObservable = BehaviorSubject.create();
        }

        return sPermissionObservable.filter(hasPermission -> hasPermission).take(1);
    }

    public static Observable<Boolean> getPermission(Context context) {
        if (sPermissionObservable != null && sPermissionObservable.hasValue()) {
            return sPermissionObservable.asObservable();
        } else {
            return promptPermission(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static Observable<Boolean> promptPermission(Context context) {
        if (sPermissionObservable == null) {
            sPermissionObservable = BehaviorSubject.create();
        }

        if (hasPermission(context)) {
            sPermissionObservable.onNext(true);
            return sPermissionObservable;
        }

        RxPermissions.getInstance(context).request(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
                .subscribe(sPermissionObservable::onNext, throwable -> {
                    Timber.i(throwable, "Failed to get storage permission");
                });

        return sPermissionObservable.asObservable();
    }

    public static Observable<Boolean> getContentObserver(Context context, Uri uri) {
        BehaviorSubject<Boolean> observer;

        if (!sContentObservers.containsKey(uri)) {
            observer = BehaviorSubject.create();
            sContentObservers.put(uri, observer);
            sSelfPendingUpdates.put(uri, 0);

            context.getContentResolver().registerContentObserver(uri, true,
                    new ContentObserver(null) {
                        @Override
                        public void onChange(boolean selfChange) {
                            observer.onNext(selfChange);
                        }
                    });
        } else {
            observer = sContentObservers.get(uri);
        }

        return observer.filter(selfChange -> {
            int pendingUpdates = sSelfPendingUpdates.get(uri);

            if (pendingUpdates > 0) {
                pendingUpdates--;
                sSelfPendingUpdates.put(uri, pendingUpdates);
                return false;
            } else {
                return true;
            }
        });
    }

    private static void ignoreSingleContentUpdate() {
        for (Map.Entry<Uri, Integer> count : sSelfPendingUpdates.entrySet()) {
            count.setValue(count.getValue() + 1);
        }
    }

    public static List<Song> getSongs(Context context, Uri uri, @Nullable String selection,
                                      @Nullable String[] selectionArgs) {
        String musicSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        if (selection != null) {
            musicSelection += " AND " + selection;
        }

        Cursor cur = context.getContentResolver().query(
                uri, SONG_PROJECTION, musicSelection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        Collections.sort(songs);
        cur.close();

        return songs;
    }

    public static List<Song> getSongs(Context context, @Nullable String selection,
                                      @Nullable String[] selectionArgs) {
        return getSongs(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                selection, selectionArgs);
    }

    public static List<Album> getAlbums(Context context, @Nullable String selection,
                                        @Nullable String[] selectionArgs) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION, selection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Album> albums = Album.buildAlbumList(cur, context.getResources());
        Collections.sort(albums);
        cur.close();

        return albums;
    }

    public static List<Artist> getArtists(Context context, @Nullable String selection,
                                          @Nullable String[] selectionArgs) {

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                ARTIST_PROJECTION, selection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Artist> artists = Artist.buildArtistList(cur, context.getResources());
        Collections.sort(artists);
        cur.close();

        return artists;
    }

    public static List<Genre> getGenres(Context context, @Nullable String selection,
                                        @Nullable String[] selectionArgs) {

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                GENRE_PROJECTION, selection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Genre> genres = Genre.buildGenreList(context, cur);
        Collections.sort(genres);
        cur.close();

        return genres;
    }

    public static List<Playlist> getAllPlaylists(Context context) {
        List<Playlist> playlists = getPlaylists(context, null, null);

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

        Collections.sort(playlists);
        return playlists;
    }

    public static List<Playlist> getPlaylists(Context context, @Nullable String selection,
                                              @Nullable String[] selectionArgs) {

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                PLAYLIST_PROJECTION, selection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Playlist> playlists = Playlist.buildPlaylistList(cur);
        Collections.sort(playlists);
        cur.close();

        return playlists;
    }

    public static List<AutoPlaylist> getAutoPlaylists(Context context) {
        List<AutoPlaylist> autoPlaylists = new ArrayList<>();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AutoPlaylistRule.class, new AutoPlaylistRule.RuleTypeAdapter())
                .create();

        try {
            File externalFiles = new File(context.getExternalFilesDir(null) + "/");

            if (externalFiles.exists() || externalFiles.mkdirs()) {
                String[] files = externalFiles.list();
                for (String file : files) {
                    if (file.endsWith(AUTO_PLAYLIST_EXTENSION)) {
                        String filePath = externalFiles + File.separator + file;
                        autoPlaylists.add(readAutoPlaylist(gson, filePath));
                    }
                }
            }
        } catch (IOException e) {
            Timber.e(e, "Failed to read AutoPlaylist");
        }

        Collections.sort(autoPlaylists);
        return autoPlaylists;
    }

    private static AutoPlaylist readAutoPlaylist(Gson gson, String path) throws IOException {
        FileReader reader = new FileReader(path);

        try {
            return gson.fromJson(reader, AutoPlaylist.class);
        } finally {
            reader.close();
        }
    }

    public static List<Album> getArtistAlbums(Context context, Artist artist) {
        return getArtistAlbums(context, artist.getArtistId());
    }

    public static List<Album> getArtistAlbums(Context context, long artistId) {
        String selection = MediaStore.Audio.AudioColumns.ARTIST_ID + " = ?";
        String[] selectionArgs = {Long.toString(artistId)};

        return getAlbums(context, selection, selectionArgs);
    }

    public static List<Song> getPlaylistSongs(Context context, Playlist playlist) {
        return getPlaylistSongs(context, playlist.getPlaylistId());
    }

    public static List<Song> getPlaylistSongs(Context context, long playlistId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                PLAYLIST_ENTRY_PROJECTION,
                null, null, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        cur.close();

        return songs;
    }

    public static List<Song> getGenreSongs(Context context, Genre genre, @Nullable String selection,
                                           @Nullable String[] selectionArgs) {
        return getGenreSongs(context, genre.getGenreId(), selection, selectionArgs);
    }

    public static List<Song> getGenreSongs(Context context, long genreId,
                                           @Nullable String selection,
                                           @Nullable String[] selectionArgs) {
        return getSongs(context, MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                selection, selectionArgs);
    }

    public static Artist findArtistByName(Context context, String artistName) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                ARTIST_PROJECTION,
                "UPPER(" + MediaStore.Audio.Artists.ARTIST + ") = ?",
                new String[]{artistName.toUpperCase()}, null);

        if (cur == null) {
            return null;
        }

        Artist found = (cur.moveToFirst()) ? new Artist(context, cur) : null;
        cur.close();

        return found;
    }

    public static Artist findArtistById(Context context, long artistId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                ARTIST_PROJECTION,
                MediaStore.Audio.Artists._ID + " = ?",
                new String[]{Long.toString(artistId)}, null);

        if (cur == null) {
            return null;
        }

        Artist found = (cur.moveToFirst()) ? new Artist(context, cur) : null;
        cur.close();

        return found;
    }

    public static Album findAlbumById(Context context, long albumId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION,
                MediaStore.Audio.Albums._ID + " = ?",
                new String[]{Long.toString(albumId)}, null);

        if (cur == null) {
            return null;
        }

        Album found = (cur.moveToFirst()) ? new Album(context, cur) : null;
        cur.close();

        return found;
    }

    public static Playlist findPlaylistByName(Context context, String playlistName) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                PLAYLIST_PROJECTION,
                "UPPER(" + MediaStore.Audio.Playlists.NAME + ") = ?",
                new String[]{playlistName.toUpperCase()}, null);

        if (cur == null) {
            return null;
        }

        Playlist found = (cur.moveToFirst()) ? new Playlist(cur) : null;
        cur.close();

        return found;
    }

    public static Playlist createPlaylist(Context context, String playlistName,
                                          @Nullable List<Song> songs) {

        ignoreSingleContentUpdate();
        String name = playlistName.trim();

        // Add the playlist to the MediaStore
        ContentValues mInserts = new ContentValues();
        mInserts.put(MediaStore.Audio.Playlists.NAME, name);
        mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        Uri newPlaylistUri = context.getContentResolver()
                .insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);

        if (newPlaylistUri == null) {
            throw new RuntimeException("Content resolver insert returned null");
        }

        // Get the id of the new playlist
        Cursor cursor = context.getContentResolver().query(
                newPlaylistUri,
                PLAYLIST_PROJECTION,
                null, null, null);

        if (cursor == null) {
            throw new RuntimeException("Content resolver query returned null");
        }

        cursor.moveToFirst();
        Playlist playlist = new Playlist(cursor);
        cursor.close();

        // If we have a list of songs, associate it with the playlist
        if (songs != null) {
            ContentValues[] values = new ContentValues[songs.size()];

            for (int i = 0; i < songs.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);
                values[i].put(
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        songs.get(i).getSongId());
            }

            Uri uri = MediaStore.Audio.Playlists.Members
                    .getContentUri("external", playlist.getPlaylistId());
            ContentResolver resolver = context.getContentResolver();

            resolver.bulkInsert(uri, values);
            resolver.notifyChange(Uri.parse("content://media"), null);
        }

        return playlist;
    }

    public static void deletePlaylist(Context context, Playlist playlist) {
        ignoreSingleContentUpdate();

        // Remove the playlist from the MediaStore
        context.getContentResolver().delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{playlist.getPlaylistId() + ""});
    }

    public static void editPlaylist(Context context, Playlist playlist,
                                    @Nullable List<Song> songs) {
        ignoreSingleContentUpdate();

        // Clear the playlist...
        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);

        if (songs != null) {
            // ... Then add all of the songs to it
            ContentValues[] values = new ContentValues[songs.size()];
            for (int i = 0; i < songs.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i + 1);
                values[i].put(
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        songs.get(i).getSongId());
            }
            resolver.bulkInsert(uri, values);
            resolver.notifyChange(Uri.parse("content://media"), null);
        }
    }

    private static int getPlaylistSize(Context context, long playlistId) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID},
                null, null, null);

        if (cur == null) {
            throw new RuntimeException("Couldn\'t open Cursor");
        }

        int count = cur.getCount();
        cur.close();

        return count;
    }

    public static void appendToPlaylist(Context context, Playlist playlist, Song song) {
        ignoreSingleContentUpdate();

        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,
                getPlaylistSize(context, playlist.getPlaylistId()));
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.getSongId());

        resolver.insert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    public static void appendToPlaylist(Context context, Playlist playlist, List<Song> songs) {
        ignoreSingleContentUpdate();

        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();

        int startingCount = getPlaylistSize(context, playlist.getPlaylistId());

        ContentValues[] values = new ContentValues[songs.size()];
        for (int i = 0; i < songs.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, startingCount + i);
            values[i].put(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    songs.get(i).getSongId());
        }

        resolver.bulkInsert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    /**
     * Get a list of songs to play for a certain input file. If a song is passed as the file, then
     * the list will include other songs in the same directory. If a playlist is passed as the file,
     * then the playlist will be opened as a regular playlist.
     *
     * @param context A {@link Context} used to resolve media paths
     * @param file The {@link File} which the list will be built around
     * @param type The MIME type of the file being opened
     * @return A list of songs that are in the same directory as the file
     */
    public static List<Song> buildSongListFromFile(Context context, File file, String type) {
        if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(type)) {
            // If a playlist was opened, try to find and play its entry from the MediaStore
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Playlists.DATA + "=?",
                    new String[] {file.getPath()},
                    MediaStore.Audio.Playlists.NAME + " ASC");

            if (cur == null) {
                return null;
            }

            List<Song> songs = getPlaylistSongs(context, new Playlist(cur));
            cur.close();
            return songs;
        } else {
            // If the file isn't a playlist, use a content resolver to find the song and play it
            // Find all songs in the directory
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATA + " like ?",
                    new String[] {"%" + file.getParent() + "/%"},
                    MediaStore.Audio.Media.DATA + " ASC");

            if (cur == null) {
                return null;
            }

            List<Song> songs = Song.buildSongList(cur, context.getResources());
            cur.close();
            return songs;
        }
    }

    /**
     * Build an {@link ArrayList} of {@link Song}s from a list of id's. Doesn't require the
     * library to be loaded
     * @param songIDs The list of song ids to convert to {@link Song}s
     * @param context The {@link Context} used to open a {@link Cursor}
     * @return An {@link ArrayList} of {@link Song}s with ids matching those of the
     *         songIDs parameter
     */
    public static List<Song> buildSongListFromIds(long[] songIDs, Context context) {
        List<Song> contents = new ArrayList<>();
        // Split this request into batches of size SQL_MAX_VARS
        for (int i = 0; i < songIDs.length / SQL_MAX_VARS; i++) {
            contents.addAll(buildSongListFromIds(songIDs, context,
                    i * SQL_MAX_VARS, (i + 1) * SQL_MAX_VARS));
        }

        // Load the remaining songs (the last section that's not divisible by SQL_MAX_VARS)
        contents.addAll(buildSongListFromIds(songIDs, context,
                SQL_MAX_VARS * (songIDs.length / SQL_MAX_VARS), songIDs.length));

        // Sort the contents of the list so that it matches the order of the array
        List<Song> songs = new ArrayList<>();
        for (long i : songIDs) {
            for (Song s : contents) {
                if (s.getSongId() == i) {
                    songs.add(s);
                    break;
                }
            }
        }

        return songs;
    }

    /**
     * Implementation of {@link MediaStoreUtil#buildSongListFromIds(long[], Context)}. This method
     * takes upper and lower bounds into account when looking at the song ids so that it can be
     * partitioned into sections of size {@link #SQL_MAX_VARS} without the overhead of making array
     * copies.
     * @param songIDs The song ids build the list from
     * @param context A Context to open a {@link Cursor} to query the {@link MediaStore}
     * @param lowerBound The first index in the array to get IDs from
     * @param upperBound The last index in the array to get IDs from
     * @return An unsorted list of {@link Song Songs} with the same IDs as the ids that were passed
     * into {@code songIDs}
     */
    private static List<Song> buildSongListFromIds(long[] songIDs, Context context, int lowerBound,
                                                   int upperBound) {
        List<Song> contents = new ArrayList<>();
        if (songIDs.length == 0) {
            return contents;
        }

        String query = MediaStore.Audio.Media._ID + " IN(?";
        String[] ids = new String[upperBound - lowerBound];
        ids[0] = Long.toString(songIDs[lowerBound]);

        for (int i = 1; i < ids.length; i++) {
            query += ",?";
            ids[i] = Long.toString(songIDs[i + lowerBound]);
        }
        query += ")";

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                query, ids,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cur == null) {
            return contents;
        }

        contents = Song.buildSongList(cur, context.getResources());
        cur.close();

        return contents;
    }
}
