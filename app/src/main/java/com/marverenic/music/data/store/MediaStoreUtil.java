package com.marverenic.music.data.store;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

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
        if (sAlreadyRequestedPermission) {
            return Observable.just(hasPermission(context));
        } else {
            return promptPermission(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static Observable<Boolean> promptPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Observable.just(true);
        }

        sAlreadyRequestedPermission = true;
        return RxPermissions.getInstance(context).request(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static List<Song> getSongs(Context context, @Nullable String selection,
                                      @Nullable String[] selectionArgs) {

        String musicSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        if (selection != null) {
            musicSelection += " AND " + selection;
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION, musicSelection, selectionArgs, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> songs = Song.buildSongList(cur, context.getResources());
        Collections.sort(songs);
        cur.close();

        return songs;
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

        Collections.sort(autoPlaylists);
        return autoPlaylists;
    }

    public static List<Song> getAlbumSongs(Context context, Album album) {
        return getAlbumSongs(context, album.getAlbumId());
    }

    public static List<Song> getAlbumSongs(Context context, long albumId) {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0  AND "
                + MediaStore.Audio.Media.ALBUM_ID + " = ";

        String[] selectionArgs = {Long.toString(albumId)};

        return getSongs(context, selection, selectionArgs);
    }

    public static List<Song> getArtistSongs(Context context, Artist artist) {
        return getArtistSongs(context, artist.getArtistId());
    }

    public static List<Song> getArtistSongs(Context context, long artistId) {
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = ?";
        String[] selectionArgs = {Long.toString(artistId)};

        return getSongs(context, selection, selectionArgs);
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
        Collections.sort(songs);
        cur.close();

        return songs;
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

    public static List<Song> searchForSongs(Context context, String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SONG_PROJECTION,
                "UPPER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?",
                new String[]{"%" + query.toUpperCase() + "%"}, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Song> found = Song.buildSongList(cur, context.getResources());
        Collections.sort(found);

        cur.close();

        return found;
    }

    public static List<Artist> searchForArtists(Context context, String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                ARTIST_PROJECTION,
                "UPPER(" + MediaStore.Audio.Artists.ARTIST + ") LIKE ?",
                new String[]{"%" + query.toUpperCase() + "%"}, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Artist> found = Artist.buildArtistList(cur, context.getResources());
        Collections.sort(found);

        cur.close();

        return found;
    }

    public static List<Album> searchForAlbums(Context context, String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                ALBUM_PROJECTION,
                "UPPER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ?",
                new String[]{"%" + query.toUpperCase() + "%"}, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Album> found = Album.buildAlbumList(cur, context.getResources());
        Collections.sort(found);

        cur.close();

        return found;
    }

    public static List<Genre> searchForGenres(Context context, String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                GENRE_PROJECTION,
                "UPPER(" + MediaStore.Audio.Genres.NAME + ") LIKE ?",
                new String[]{"%" + query.toUpperCase() + "%"}, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Genre> found = Genre.buildGenreList(context, cur);
        Collections.sort(found);

        cur.close();

        return found;
    }

    public static List<Playlist> searchForPlaylists(Context context, String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                PLAYLIST_PROJECTION,
                "UPPER(" + MediaStore.Audio.Playlists.NAME + ") LIKE ?",
                new String[]{"%" + query.toUpperCase() + "%"}, null);

        if (cur == null) {
            return Collections.emptyList();
        }

        List<Playlist> found = Playlist.buildPlaylistList(cur);
        Collections.sort(found);

        cur.close();

        return found;
    }

    public static Playlist createPlaylist(Context context, String playlistName,
                                          @Nullable List<Song> songs) {

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
        // Remove the playlist from the MediaStore
        context.getContentResolver().delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{playlist.getPlaylistId() + ""});
    }

    public static void editPlaylist(Context context, Playlist playlist,
                                    @Nullable List<Song> songs) {
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
     * @param activity An {@link Activity} to use when building the list
     * @param file The {@link File} which the list will be built around
     * @param type The MIME type of the file being opened
     * @param queue An {@link ArrayList} which will be populated with the {@link Song}s
     * @return The position that this list should be started from
     * @throws IOException
     */
    public static int getSongListFromFile(Activity activity, File file, String type,
                                          final List<Song> queue) throws IOException {
        // PLAYLISTS
        if (type.equals("audio/x-mpegurl") || type.equals("audio/x-scpls")
                || type.equals("application/vnd.ms-wpl")) {
            // If a playlist was opened, try to find and play its entry from the MediaStore
            Cursor cur = activity.getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Playlists.DATA + "=?",
                    new String[] {file.getPath()},
                    MediaStore.Audio.Playlists.NAME + " ASC");

            if (cur == null) {
                throw new RuntimeException("Content resolver query returned null");
            }

            // If the media store contains this playlist, play it like a regular playlist
            if (cur.getCount() > 0) {
                cur.moveToFirst();
                queue.addAll(getPlaylistSongs(activity, new Playlist(cur)));
            }
            //TODO Attempt to manually read common playlist writing schemes
            /*else{
                // If the MediaStore doesn't contain this playlist, attempt to read it manually
                Scanner sc = new Scanner(file);
                ArrayList<String> lines = new ArrayList<>();
                while (sc.hasNextLine()) {
                    lines.add(sc.nextLine());
                }

                if (lines.size() > 0) {
                    // Do stuff
                }

            }*/
            cur.close();
            // Return 0 to start at the beginning of the playlist
            return 0;
        } else { // ALL OTHER TYPES OF MEDIA
            // If the file isn't a playlist, use a content resolver to find the song and play it
            // Find all songs in the directory
            Cursor cur = activity.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATA + " like ?",
                    new String[] {"%" + file.getParent() + "/%"},
                    MediaStore.Audio.Media.DATA + " ASC");

            if (cur == null) {
                throw new RuntimeException("Content resolver query returned null");
            }

            // Create song objects to match those in the music library
            queue.addAll(Song.buildSongList(cur, activity.getResources()));
            cur.close();

            // Find the position of the song that should be played
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getLocation().equals(file.getPath())) return i;
            }
        }

        return 0;
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
