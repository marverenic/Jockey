package com.marverenic.music.instances;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.marverenic.music.Player;
import com.marverenic.music.R;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Themes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class Library {

    public static final String PLAY_COUNT_FILENAME = ".playcount";
    public static final String PLAY_COUNT_FILE_COMMENT =
            "This file contains play count information for Jockey and should not be edited";
    public static final int PERMISSION_REQUEST_ID = 0x01;

    private static final String AUTO_PLAYLIST_EXTENSION = ".jpl";

    private static final List<Playlist> playlistLib = new ArrayList<>();
    private static final List<Song> songLib = new ArrayList<>();
    private static final List<Artist> artistLib = new ArrayList<>();
    private static final List<Album> albumLib = new ArrayList<>();
    private static final List<Genre> genreLib = new ArrayList<>();

    // TODO use a better data structure
    private static final Map<Long, Integer> playCounts = new HashMap<>();
    private static final Map<Long, Integer> skipCounts = new HashMap<>();
    private static final Map<Long, Integer> playDates = new HashMap<>();

    private static final String[] songProjection = new String[]{
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

    private static final String[] artistProjection = new String[]{
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
    };

    private static final String[] albumProjection = new String[]{
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR,
            MediaStore.Audio.Albums.ALBUM_ART
    };

    private static final String[] playlistProjection = new String[]{
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
    };

    private static final String[] genreProjection = new String[]{
            MediaStore.Audio.Genres._ID,
            MediaStore.Audio.Genres.NAME
    };

    private static final String[] playlistEntryProjection = new String[]{
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
    private Library() {

    }

    //
    //          LIBRARY LISTENERS
    //

    // Since it's important to know when the Library has entries added or removed so we can update
    // the UI accordingly, associate listeners to receive callbacks for such events. These listeners
    // will get called only when entries are added or removed -- not changed. This lets us do a lot
    // of things on the UI like adding and removing playlists without having to create the
    // associated Snackbars, AlertDialogs, etc. and is slightly cleaner than passing a callback as a
    // parameter to methods that cause such changes since we don't have to instantiate
    // a single-use Object.

    private static final ArrayList<PlaylistChangeListener> PLAYLIST_LISTENERS = new ArrayList<>();
    private static final ArrayList<LibraryRefreshListener> REFRESH_LISTENERS = new ArrayList<>();

    public interface PlaylistChangeListener {
        void onPlaylistRemoved(Playlist removed, int index);
        void onPlaylistAdded(Playlist added, int index);
    }

    public interface LibraryRefreshListener {
        void onLibraryRefreshed();
    }

    /**
     * In certain cases like in {@link com.marverenic.music.fragments.PlaylistFragment}, editing the
     * library can break a lot of things if not done carefully. (i.e. if
     * {@link android.support.v7.widget.RecyclerView.Adapter#notifyDataSetChanged()} (or similar)
     * isn't called after adding a playlist, then the UI process can throw an exception and crash)
     *
     * If this is the case, call this method to set a callback whenever the library gets updated
     * (Not all library update calls will be relevant to the context, but better safe than sorry).
     *
     * <b>When using this method MAKE SURE TO CALL
     * {@link Library#removePlaylistListener(PlaylistChangeListener)} WHEN THE ACTIVITY PAUSES --
     * OTHERWISE YOU WILL CAUSE A LEAK.</b>
     *
     * @param listener A {@link PlaylistChangeListener} to act as a callback
     *                 when the library is changed in any way
     */
    public static void addPlaylistListener(PlaylistChangeListener listener) {
        if (!PLAYLIST_LISTENERS.contains(listener)) {
            PLAYLIST_LISTENERS.add(listener);
        }
    }

    public static void addRefreshListener(LibraryRefreshListener listener) {
        if (!REFRESH_LISTENERS.contains(listener)) {
            REFRESH_LISTENERS.add(listener);
        }
    }

    /**
     * Remove a {@link PlaylistChangeListener} previously added to listen
     * for library updates.
     * @param listener A {@link PlaylistChangeListener} currently registered
     *                 to recieve a callback when the library gets modified. If it's not already
     *                 registered, then nothing will happen.
     */
    public static void removePlaylistListener(PlaylistChangeListener listener) {
        if (PLAYLIST_LISTENERS.contains(listener)) {
            PLAYLIST_LISTENERS.remove(listener);
        }
    }

    public static void removeRefreshListener(LibraryRefreshListener listener) {
        REFRESH_LISTENERS.remove(listener);
    }

    /**
     * Private method for notifying registered {@link PlaylistChangeListener}s
     * that the library has lost an entry. (Changing entries doesn't matter)
     */
    private static void notifyPlaylistRemoved(Playlist removed, int index) {
        for (PlaylistChangeListener l : PLAYLIST_LISTENERS) {
            l.onPlaylistRemoved(removed, index);
        }
    }

    /**
     * Private method for notifying registered {@link PlaylistChangeListener}s
     * that the library has gained an entry. (Changing entries doesn't matter)
     */
    private static void notifyPlaylistAdded(Playlist added) {
        int index = playlistLib.indexOf(added);
        for (PlaylistChangeListener l : PLAYLIST_LISTENERS) {
            l.onPlaylistAdded(added, index);
        }
    }

    private static void notifyLibraryRefreshed() {
        for (LibraryRefreshListener l : REFRESH_LISTENERS) {
            l.onLibraryRefreshed();
        }
    }

    //
    //          PERMISSION METHODS
    //

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasRWPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED
                        && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestRWPermission(Activity activity) {
        activity.requestPermissions(
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_ID);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean previouslyRequestedRWPermission(Activity activity) {
        return
                activity.shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                || activity.shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    //
    //          LIBRARY BUILDING METHODS
    //

    /**
     * Scan the MediaStore and update the libraries held in memory
     * @param activity {@link Activity} to use to open {@link Cursor}s and get external storage
     *                                 permission if necessary
     */
    public static void scanAll(final Activity activity) {
        if (hasRWPermission(activity)) {
            resetAll();
            setPlaylistLib(scanPlaylists(activity));
            setSongLib(scanSongs(activity));
            setArtistLib(scanArtists(activity));
            setAlbumLib(scanAlbums(activity));
            setGenreLib(scanGenres(activity));
            sort();
            notifyLibraryRefreshed();

            // If the user permits it, log info about the size of their library
            if (Prefs.allowAnalytics(activity)) {
                int autoPlaylistCount = 0;
                for (Playlist p : playlistLib) {
                    if (p instanceof AutoPlaylist) {
                        autoPlaylistCount++;
                    }
                }

                Answers.getInstance().logCustom(
                        new CustomEvent("Loaded library")
                                .putCustomAttribute("Playlist count", playlistLib.size())
                                .putCustomAttribute("Auto Playlist count", autoPlaylistCount)
                                .putCustomAttribute("Song count", songLib.size())
                                .putCustomAttribute("Artist count", artistLib.size())
                                .putCustomAttribute("Album count", albumLib.size())
                                .putCustomAttribute("Genre count", genreLib.size()));
            }
        } else if (!previouslyRequestedRWPermission(activity)) {
            requestRWPermission(activity);
        }
    }

    /**
     * Scans the MediaStore for songs
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Song}s in the MediaStore
     */
    public static List<Song> scanSongs(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songProjection,
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

    /**
     * Scans the MediaStore for artists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Artist}s in the MediaStore
     */
    public static List<Artist> scanArtists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                artistProjection,
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

    /**
     * Scans the MediaStore for albums
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Album}s in the MediaStore
     */
    public static List<Album> scanAlbums(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumProjection,
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

    /**
     * Scans the MediaStore for playlists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Playlist}s in the MediaStore
     */
    public static List<Playlist> scanPlaylists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                playlistProjection,
                null,
                null,
                MediaStore.Audio.Playlists.NAME + " ASC");

        if (cur == null) {
            return new ArrayList<>();
        }

        List<Playlist> playlists = Playlist.buildPlaylistList(cur);
        cur.close();

        for (Playlist p : scanAutoPlaylists(context)) {
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

    /**
     * Scans storage for Auto Playlist configurations
     * @param context {@link Context} to use to read files on the device
     * @return An {@link ArrayList} with the loaded {@link AutoPlaylist}s
     */
    public static ArrayList<AutoPlaylist> scanAutoPlaylists(Context context) {
        ArrayList<AutoPlaylist> autoPlaylists = new ArrayList<>();
        final Gson gson = new Gson();

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

    /**
     * Scans the MediaStore for genres
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Genre}s in the MediaStore
     */
    public static List<Genre> scanGenres(Context context) {
        List<Genre> genres = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                genreProjection,
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

    /**
     * Checks Strings from ContentResolvers and replaces the default unknown value of
     * {@link MediaStore#UNKNOWN_STRING} with another String if needed
     * @param value The value returned from the ContentResolver
     * @param convertValue The value to replace unknown Strings with
     * @return A String with localized unknown values if needed, otherwise the original value
     */
    protected static String parseUnknown(String value, String convertValue) {
        if (value == null || value.equals(MediaStore.UNKNOWN_STRING)) {
            return convertValue;
        } else {
            return value;
        }
    }

    //
    //          LIBRARY STORAGE METHODS
    //

    /**
     * Remove all library entries from memory
     */
    public static void resetAll() {
        playlistLib.clear();
        songLib.clear();
        artistLib.clear();
        albumLib.clear();
        genreLib.clear();
    }

    /**
     * Replace the playlist library in memory with another one
     * @param newLib The new playlist library
     */
    public static void setPlaylistLib(List<Playlist> newLib) {
        playlistLib.clear();
        playlistLib.addAll(newLib);
    }

    /**
     * Replace the song library in memory with another one
     * @param newLib The new song library
     */
    public static void setSongLib(List<Song> newLib) {
        songLib.clear();
        songLib.addAll(newLib);
    }

    /**
     * Replace the album library in memory with another one
     * @param newLib The new album library
     */
    public static void setAlbumLib(List<Album> newLib) {
        albumLib.clear();
        albumLib.addAll(newLib);
    }

    /**
     * Replace the artist library in memory with another one
     * @param newLib The new artist library
     */
    public static void setArtistLib(List<Artist> newLib) {
        artistLib.clear();
        artistLib.addAll(newLib);
    }

    /**
     * Replace the genre library in memory with another one
     * @param newLib The new genre library
     */
    public static void setGenreLib(List<Genre> newLib) {
        genreLib.clear();
        genreLib.addAll(newLib);
    }

    /**
     * Sorts the libraries in memory using the default {@link Library} sort methods
     */
    public static void sort() {
        Collections.sort(songLib);
        Collections.sort(albumLib);
        Collections.sort(artistLib);
        Collections.sort(playlistLib);
        Collections.sort(genreLib);
    }

    /**
     * @return true if the library is populated with any entries
     */
    public static boolean isEmpty() {
        return songLib.isEmpty() && albumLib.isEmpty() && artistLib.isEmpty()
                && playlistLib.isEmpty() && genreLib.isEmpty();
    }

    /**
     * @return An {@link ArrayList} of {@link Playlist}s in the MediaStore
     */
    public static List<Playlist> getPlaylists() {
        return playlistLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Song}s in the MediaStore
     */
    public static List<Song> getSongs() {
        return songLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Album}s in the MediaStore
     */
    public static List<Album> getAlbums() {
        return albumLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Artist}s in the MediaStore
     */
    public static List<Artist> getArtists() {
        return artistLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Genre}s in the MediaStore
     */
    public static List<Genre> getGenres() {
        return genreLib;
    }

    //
    //          LIBRARY SEARCH METHODS
    //

    /**
     * Finds a {@link Song} in the library based on its Id
     * @param songId the MediaStore Id of the {@link Song}
     * @return A {@link Song} with a matching Id
     */
    public static Song findSongById(int songId) {
        for (Song s : songLib) {
            if (s.getSongId() == songId) {
                return s;
            }
        }
        return null;
    }

    /**
     * Build an {@link ArrayList} of {@link Song}s from a list of id's. Doesn't require the
     * library to be loaded
     * @param songIDs The list of song ids to convert to {@link Song}s
     * @param context The {@link Context} used to open a {@link Cursor}
     * @return An {@link ArrayList} of {@link Song}s with ids matching those of the
     *         songIDs parameter
     */
    public static List<Song> buildSongListFromIds(int[] songIDs, Context context) {
        List<Song> contents = new ArrayList<>();
        if (songIDs.length == 0) {
            return contents;
        }

        String query = MediaStore.Audio.Media._ID + " IN(?";
        String[] ids = new String[songIDs.length];
        ids[0] = Integer.toString(songIDs[0]);

        for (int i = 1; i < songIDs.length; i++) {
            query += ",?";
            ids[i] = Integer.toString(songIDs[i]);
        }
        query += ")";

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songProjection,
                query,
                ids,
                MediaStore.Audio.Media.TITLE + " ASC");

        if (cur == null) {
            return contents;
        }

        contents = Song.buildSongList(cur, context.getResources());
        cur.close();

        // TODO Is this necessary?
        // Sort the contents of the list so that it matches the order of the int array
        /*List<Song> songs = new ArrayList<>();
        Song dummy = new Song(null, 0, null, null, 0, null, 0, 0, 0, 0);
        for (int i : songIDs) {
            dummy.songId = i;
            // Because Songs are equal if their ids are equal, we can use a dummy song with the ID
            // we want to find it in the list
            songs.add(contents.get(contents.indexOf(dummy)));
        }*/

        return contents;
    }

    /**
     * Finds a {@link Artist} in the library based on its Id
     * @param artistId the MediaStore Id of the {@link Artist}
     * @return A {@link Artist} with a matching Id
     */
    public static Artist findArtistById(long artistId) {
        for (Artist a : artistLib) {
            if (a.getArtistId() == artistId) {
                return a;
            }
        }
        return null;
    }

    /**
     * Finds a {@link Album} in a library based on its Id
     * @param albumId the MediaStore Id of the {@link Album}
     * @return A {@link Album} with a matching Id
     */
    public static Album findAlbumById(long albumId) {
        // Returns the first Artist object in the library with a matching id
        for (Album a : albumLib) {
            if (a.getAlbumId() == albumId) {
                return a;
            }
        }
        return null;
    }

    /**
     * Finds a {@link Genre} in a library based on its Id
     * @param genreId the MediaStore Id of the {@link Genre}
     * @return A {@link Genre} with a matching Id
     */
    public static Genre findGenreById(long genreId) {
        // Returns the first Genre object in the library with a matching id
        for (Genre g : genreLib) {
            if (g.getGenreId() == genreId) {
                return g;
            }
        }
        return null;
    }

    public static Artist findArtistByName(String artistName) {
        final String trimmedQuery = artistName.trim();
        for (Artist a : artistLib) {
            if (a.getArtistName().equalsIgnoreCase(trimmedQuery)) {
                return a;
            }
        }
        return null;
    }

    //
    //          CONTENTS QUERY METHODS
    //

    /**
     * Get a list of songs in a certain playlist
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s contained in the playlist
     */
    public static List<Song> getPlaylistEntries(Context context, Playlist playlist) {
        if (playlist instanceof AutoPlaylist) {
            List<Song> entries = ((AutoPlaylist) playlist).generatePlaylist(context);
            editPlaylist(context, playlist, entries);
            return entries;
        }

        List<Song> songEntries = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members
                        .getContentUri("external", playlist.getPlaylistId()),
                playlistEntryProjection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);

        if (cur == null) {
            return songEntries;
        }

        songEntries = Song.buildSongList(cur, context.getResources());
        cur.close();

        return songEntries;
    }

    /**
     * Get a list of songs on a certain album
     * @param album The {@link Album} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s contained in the album
     */
    public static ArrayList<Song> getAlbumEntries(Album album) {
        ArrayList<Song> songEntries = new ArrayList<>();

        for (Song s : songLib) {
            if (s.getAlbumId() == album.getAlbumId()) {
                songEntries.add(s);
            }
        }

        Collections.sort(songEntries, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return ((Integer) o1.getTrackNumber()).compareTo(o2.getTrackNumber());
            }
        });

        return songEntries;
    }

    /**
     * Get a list of songs by a certain artist
     * @param artist The {@link Artist} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s by the artist
     */
    public static ArrayList<Song> getArtistSongEntries(Artist artist) {
        ArrayList<Song> songEntries = new ArrayList<>();

        for (Song s : songLib) {
            if (s.getArtistId() == artist.getArtistId()) {
                songEntries.add(s);
            }
        }

        return songEntries;
    }

    /**
     * Get a list of albums by a certain artist
     * @param artist The {@link Artist} to get the entries of
     * @return An {@link ArrayList} of {@link Album}s by the artist
     */
    public static ArrayList<Album> getArtistAlbumEntries(Artist artist) {
        ArrayList<Album> albumEntries = new ArrayList<>();

        for (Album a : albumLib) {
            if (a.getArtistId() == artist.getArtistId()) {
                albumEntries.add(a);
            }
        }

        return albumEntries;
    }

    /**
     * Get a list of songs in a certain genre
     * @param genre The {@link Genre} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s contained in the genre
     */
    public static ArrayList<Song> getGenreEntries(Genre genre) {
        ArrayList<Song> songEntries = new ArrayList<>();

        for (Song s : songLib) {
            if (s.getGenreId() == genre.getGenreId()) {
                songEntries.add(s);
            }
        }

        return songEntries;
    }

    //
    //          PLAY COUNT READING & ACCESSING METHODS
    //

    /**
     * Reload the play counts as modified by {@link Player#logPlayCount(long, boolean)}
     * @param context Used to open a {@link Properties} from disk
     */
    public static void loadPlayCounts(Context context) {
        playCounts.clear();
        skipCounts.clear();
        playDates.clear();
        try {
            Properties countProperties = openPlayCountFile(context);
            Enumeration iterator = countProperties.propertyNames();

            while (iterator.hasMoreElements()) {
                String key = (String) iterator.nextElement();
                String value = countProperties.getProperty(key, "0,0");

                final String[] originalValues = value.split(",");

                int playCount = Integer.parseInt(originalValues[0]);
                int skipCount = Integer.parseInt(originalValues[1]);
                int playDate = 0;

                if (originalValues.length > 2) {
                    playDate = Integer.parseInt(originalValues[2]);
                }

                playCounts.put(Long.parseLong(key), playCount);
                skipCounts.put(Long.parseLong(key), skipCount);
                playDates.put(Long.parseLong(key), playDate);
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    /**
     * Returns a readable {@link Properties} to be used with {@link Library#loadPlayCounts(Context)}
     * @param context Used to read files from external storage
     * @return A {@link Properties} object that has been initialized with values saved by
     *         {@link Player#logPlayCount(long, boolean)} and {@link Player#savePlayCountFile()}
     * @throws IOException
     */
    private static Properties openPlayCountFile(Context context) throws IOException {
        File file = new File(context.getExternalFilesDir(null) + "/" + Library.PLAY_COUNT_FILENAME);

        if (file.exists() || file.createNewFile()) {
            InputStream is = new FileInputStream(file);
            Properties playCountHashtable;

            playCountHashtable = new Properties();
            playCountHashtable.load(is);

            is.close();
            return playCountHashtable;
        } else {
            return new Properties();
        }
    }

    /**
     * Returns the number of skips a song has. Note that you may need to call
     * {@link Library#loadPlayCounts(Context)} in case the data has gone stale
     * @param songId The {@link Song#songId} as written in the MediaStore
     * @return The number of times a song has been skipped
     */
    protected static int getSkipCount(long songId) {
        return skipCounts.get(songId);
    }

    /**
     * Returns the number of plays a song has. Note that you may need to call
     * {@link Library#loadPlayCounts(Context)} in case the data has gone stale
     * @param songId The {@link Song#songId} as written in the MediaStore
     * @return The number of times a song has been plays
     */
    protected static int getPlayCount(long songId) {
        return playCounts.get(songId);
    }

    /**
     * * Returns the last time a song was played with Jockey. Note that you may need to call
     * {@link Library#loadPlayCounts(Context)} in case the data has gone stale
     * @param songId The {@link Song#songId} as written in the MediaStore
     * @return The last time a song was played given in seconds as a UTC timestamp
     *         (since midnight of January 1, 1970 UTC)
     */
    protected static int getPlayDate(long songId) {
        return playDates.get(songId);
    }

    //
    //          PLAYLIST WRITING METHODS
    //

    /**
     * Add a new playlist to the MediaStore
     * @param view A {@link View} to put a {@link Snackbar} in. Will also be used to get a
     *             {@link Context}.
     * @param playlistName The name of the new playlist
     * @param songList An {@link ArrayList} of {@link Song}s to populate the new playlist
     * @return The Playlist that was added to the library
     */
    public static Playlist createPlaylist(final View view, final String playlistName,
                                          @Nullable final List<Song> songList) {
        final Context context = view.getContext();
        String trimmedName = playlistName.trim();

        setPlaylistLib(scanPlaylists(context));

        String error = verifyPlaylistName(context, trimmedName);
        if (error != null) {
            Snackbar
                    .make(
                            view,
                            error,
                            Snackbar.LENGTH_SHORT)
                    .show();
            return null;
        }

        // Add the playlist to the MediaStore
        final Playlist created = addPlaylist(context, trimmedName, songList);

        Snackbar
                .make(
                        view,
                        String.format(context.getResources().getString(
                                R.string.message_created_playlist), playlistName),
                        Snackbar.LENGTH_LONG)
                .setAction(
                        R.string.action_undo,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                deletePlaylist(context, created);
                            }
                        })
                .show();

        return created;
    }

    /**
     * Test a playlist name to make sure it is valid when making a new playlist.
     * Invalid playlist names are instance_empty or already exist in the MediaStore
     * @param context A {@link Context} used to get localized Strings
     * @param playlistName The playlist name that needs to be validated
     * @return null if there is no error, or a {@link String} describing the error that can be
     *         presented to the user
     */
    public static String verifyPlaylistName(final Context context, final String playlistName) {
        String trimmedName = playlistName.trim();
        if (trimmedName.length() == 0) {
            return context.getResources().getString(R.string.error_hint_empty_playlist);
        }

        for (Playlist p : playlistLib) {
            if (p.getPlaylistName().equalsIgnoreCase(trimmedName)) {
                return context.getResources().getString(R.string.error_hint_duplicate_playlist);
            }
        }
        return null;
    }

    /**
     * Removes a playlist from the MediaStore
     * @param view A {@link View} to show a {@link Snackbar} and to get a {@link Context} used
     *             to edit the MediaStore
     * @param playlist A {@link Playlist} which will be removed from the MediaStore
     */
    public static void removePlaylist(final View view, final Playlist playlist) {
        final Context context = view.getContext();
        final List<Song> entries = getPlaylistEntries(context, playlist);

        deletePlaylist(context, playlist);

        Snackbar
                .make(
                        view,
                        String.format(context.getString(
                                R.string.message_removed_playlist), playlist),
                        Snackbar.LENGTH_LONG)
                .setAction(
                        context.getString(R.string.action_undo),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (playlist instanceof AutoPlaylist) {
                                    createAutoPlaylist(context, (AutoPlaylist) playlist);
                                } else {
                                    addPlaylist(context, playlist.getPlaylistName(), entries);
                                }
                            }
                        })
                .show();
    }

    /**
     * Replace the entries of a playlist in the MediaStore with a new {@link ArrayList} of
     * {@link Song}s
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param newSongList An {@link ArrayList} of {@link Song}s to overwrite the list contained
     *                    in the MediaStore
     */
    public static void editPlaylist(final Context context, final Playlist playlist,
                                    final List<Song> newSongList) {
        // Clear the playlist...
        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);

        // Then add all of the songs to it
        ContentValues[] values = new ContentValues[newSongList.size()];
        for (int i = 0; i < newSongList.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i + 1);
            values[i].put(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    newSongList.get(i).getSongId());
        }
        resolver.bulkInsert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    /**
     * Rename a playlist in the MediaStore
     * @param context A {@link Context} to open a {@link ContentResolver}
     * @param playlistID The id of the {@link Playlist} to be renamed
     * @param name The new name of the playlist
     */
    public static void renamePlaylist(final Context context, final long playlistID,
                                      final String name) {
        if (verifyPlaylistName(context, name) == null) {
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Audio.Playlists.NAME, name);

            ContentResolver resolver = context.getContentResolver();
            resolver.update(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    values,
                    MediaStore.Audio.Playlists._ID + "=?",
                    new String[]{Long.toString(playlistID)});
        }
    }

    /**
     * Append a song to the end of a playlist. Alerts the user about duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param song The {@link Song} to be added to the playlist in the MediaStore
     */
    public static void addPlaylistEntry(final Context context, final Playlist playlist,
                                        final Song song) {
        // Public method to add a song to a playlist
        // Checks the playlist for duplicate entries
        if (getPlaylistEntries(context, playlist).contains(song)) {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getQuantityString(
                            R.plurals.alert_confirm_duplicates, 1))
                    .setMessage(context.getString(
                            R.string.playlist_confirm_duplicate, playlist, song))
                    .setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addSongToEndOfPlaylist(context, playlist, song);
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();

            Themes.themeAlertDialog(dialog);
        } else {
            addSongToEndOfPlaylist(context, playlist, song);
        }
    }

    /**
     * Append a list of songs to the end of a playlist. Alerts the user about duplicates
     * @param view A {@link View} to put a {@link android.support.design.widget.Snackbar} in. Will
     *             also be used to get a {@link Context}.
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param songs The {@link ArrayList} of {@link Song}s to be added to the playlist
     *              in the MediaStore
     */
    public static void addPlaylistEntries(final View view, final Playlist playlist,
                                          final List<Song> songs) {
        // Public method to add songs to a playlist
        // Checks the playlist for duplicate entries

        final Context context = view.getContext();

        int duplicateCount = 0;
        final List<Song> currentEntries = getPlaylistEntries(context, playlist);
        final List<Song> newEntries = new ArrayList<>();

        for (Song s : songs) {
            if (currentEntries.contains(s)) {
                duplicateCount++;
            } else {
                newEntries.add(s);
            }
        }

        if (duplicateCount > 0) {
            AlertDialog.Builder alert = new AlertDialog.Builder(context).setTitle(
                    context.getResources().getQuantityString(
                            R.plurals.alert_confirm_duplicates, duplicateCount));

            if (duplicateCount == songs.size()) {
                alert
                        .setMessage(context.getString(R.string.playlist_confirm_all_duplicates,
                                duplicateCount))
                        .setPositiveButton(context.getResources().getQuantityText(
                                R.plurals.playlist_positive_add_duplicates, duplicateCount),
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
                                Snackbar.make(
                                        view,
                                        context.getString(
                                                R.string.confirm_add_songs,
                                                songs.size(),
                                                playlist.getPlaylistName()),
                                        Snackbar.LENGTH_LONG)
                                        .setAction(context.getString(R.string.action_undo),
                                                new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Library.editPlaylist(
                                                        context,
                                                        playlist,
                                                        currentEntries);
                                            }
                                        }).show();
                            }
                        })
                        .setNeutralButton(context.getString(R.string.action_cancel), null);
            } else {
                alert
                        .setMessage(context.getResources().getQuantityString(
                                R.plurals.playlist_confirm_some_duplicates,
                                duplicateCount, duplicateCount))
                        .setPositiveButton(R.string.action_add_new,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, newEntries);
                                Snackbar.make(
                                        view,
                                        context.getString(
                                                R.string.confirm_add_songs,
                                                newEntries.size(),
                                                playlist.getPlaylistName()),
                                        Snackbar.LENGTH_LONG)
                                        .setAction(R.string.action_undo,
                                                new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Library.editPlaylist(
                                                        context,
                                                        playlist,
                                                        currentEntries);
                                            }
                                        }).show();
                            }
                        })
                        .setNegativeButton(R.string.action_add_all,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
                                Snackbar.make(
                                        view,
                                        context.getString(
                                                R.string.confirm_add_songs,
                                                songs.size(),
                                                playlist.getPlaylistName()),
                                        Snackbar.LENGTH_LONG)
                                        .setAction(R.string.action_undo,
                                                new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Library.editPlaylist(
                                                        context,
                                                        playlist,
                                                        currentEntries);
                                            }
                                        }).show();
                            }
                        })
                        .setNeutralButton(R.string.action_cancel, null);
            }

            Themes.themeAlertDialog(alert.show());
        } else {
            addSongsToEndOfPlaylist(context, playlist, songs);
            Snackbar.make(
                    view,
                    context.getString(
                            R.string.confirm_add_songs,
                            newEntries.size(),
                            playlist.getPlaylistName()),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Library.editPlaylist(
                                    context,
                                    playlist,
                                    currentEntries);
                        }
                    }).show();
        }
    }

    //
    //          MEDIA_STORE EDIT METHODS
    //
    // These methods only perform actions to the MediaStore. They do not validate inputs, and they
    // do not display confirmation messages to the user.
    //

    /**
     * Add a new playlist to the MediaStore and to the application's current library instance. Use
     * this when making regular playlists.
     * Outside of this class, use {@link Library#createPlaylist(View, String, List)} instead
     * <b>This method DOES NOT validate inputs or display a confirmation message to the user</b>.
     * @param context A {@link Context} used to edit the MediaStore
     * @param playlistName The name of the new playlist
     * @param songList An {@link ArrayList} of {@link Song}s to populate the new playlist
     * @return The Playlist that was added to the library
     */
    private static Playlist addPlaylist(final Context context, final String playlistName,
                                        @Nullable final List<Song> songList) {
        final Playlist added = makePlaylist(context, playlistName, songList);
        playlistLib.add(added);
        Collections.sort(playlistLib);
        notifyPlaylistAdded(added);
        return added;
    }

    /**
     * Internal logic for adding a playlist to the MediaStore only.
     * @param context A {@link Context} used to edit the MediaStore
     * @param playlistName The name of the new playlist
     * @param songList An {@link ArrayList} of {@link Song}s to populate the new playlist
     * @return The Playlist that was added to the library
     * @see Library#addPlaylist(Context, String, List) for playlist creation
     * @see Library#createAutoPlaylist(Context, AutoPlaylist) for AutoPlaylist creation
     */
    private static Playlist makePlaylist(final Context context, final String playlistName,
                                         @Nullable final List<Song> songList) {
        String trimmedName = playlistName.trim();

        // Add the playlist to the MediaStore
        ContentValues mInserts = new ContentValues();
        mInserts.put(MediaStore.Audio.Playlists.NAME, trimmedName);
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
                new String[] {MediaStore.Audio.Playlists._ID},
                null, null, null);

        if (cursor == null) {
            throw new RuntimeException("Content resolver query returned null");
        }

        cursor.moveToFirst();
        Playlist playlist = new Playlist(
                cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)), playlistName);
        cursor.close();

        // If we have a list of songs, associate it with the playlist
        if (songList != null) {
            ContentValues[] values = new ContentValues[songList.size()];

            for (int i = 0; i < songList.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);
                values[i].put(
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        songList.get(i).getSongId());
            }

            Uri uri = MediaStore.Audio.Playlists.Members
                    .getContentUri("external", playlist.getPlaylistId());
            ContentResolver resolver = context.getContentResolver();

            resolver.bulkInsert(uri, values);
            resolver.notifyChange(Uri.parse("content://media"), null);
        }

        return playlist;
    }

    /**
     * Removes a playlist from the MediaStore
     * @param context A {@link Context} to update the MediaStore
     * @param playlist A {@link Playlist} whose matching playlist will be removed
     *                 from the MediaStore
     */
    public static void deletePlaylist(final Context context, final Playlist playlist) {
        int index = playlistLib.indexOf(playlist);

        // Remove the playlist from the MediaStore
        context.getContentResolver().delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{playlist.getPlaylistId() + ""});

        // If the playlist is an AutoPlaylist, make sure to delete its configuration
        if (playlist instanceof AutoPlaylist) {
            //noinspection ResultOfMethodCallIgnored
            new File(context.getExternalFilesDir(null)
                    + "/" + playlist.getPlaylistName() + AUTO_PLAYLIST_EXTENSION).delete();
        }

        // Update the playlist library & resort it
        playlistLib.clear();
        setPlaylistLib(scanPlaylists(context));
        Collections.sort(playlistLib);
        notifyPlaylistRemoved(playlist, index);
    }

    /**
     * Append a song to the end of a playlist. Doesn't check for duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param song The {@link Song} to be added to the playlist in the MediaStore
     */
    private static void addSongToEndOfPlaylist(final Context context, final Playlist playlist,
                                               final Song song) {
        // Private method to add a song to a playlist
        // This method does the actual operation to the MediaStore
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members
                        .getContentUri("external", playlist.getPlaylistId()),
                null, null, null,
                MediaStore.Audio.Playlists.Members.TRACK + " ASC");

        if (cur == null) {
            throw new RuntimeException("Content resolver query returned null");
        }

        long count = 0;
        if (cur.moveToLast()) {
            count = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
        }
        cur.close();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count + 1);
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.getSongId());

        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();
        resolver.insert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    /**
     * Append a list of songs to the end of a playlist. Doesn't check for duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param songs The {@link ArrayList} of {@link Song}s to be added to the playlist
     *              in the MediaStore
     */
    private static void addSongsToEndOfPlaylist(final Context context, final Playlist playlist,
                                                final List<Song> songs) {
        // Private method to add a song to a playlist
        // This method does the actual operation to the MediaStore
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members
                        .getContentUri("external", playlist.getPlaylistId()),
                null, null, null,
                MediaStore.Audio.Playlists.Members.TRACK + " ASC");

        if (cur == null) {
            throw new RuntimeException("Content resolver query returned null");
        }

        long count = 0;
        if (cur.moveToLast()) {
            count = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
        }
        cur.close();

        ContentValues[] values = new ContentValues[songs.size()];
        for (int i = 0; i < songs.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count + 1);
            values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(i).getSongId());
        }

        Uri uri = MediaStore.Audio.Playlists.Members
                .getContentUri("external", playlist.getPlaylistId());
        ContentResolver resolver = context.getContentResolver();
        resolver.bulkInsert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    //
    //          AUTO PLAYLIST EDIT METHODS
    //

    /**
     * Add an {@link AutoPlaylist} to the library.
     * @param playlist the AutoPlaylist to be added to the library. The configuration of this
     *                 playlist will be saved so that it can be loaded when the library is next
     *                 rescanned, and a "stale" copy with current entries will be written in the
     *                 MediaStore so that other applications may access this playlist
     */
    public static void createAutoPlaylist(Context context, AutoPlaylist playlist) {
        try {
            // Add the playlist to the MediaStore
            Playlist p = makePlaylist(
                    context,
                    playlist.getPlaylistName(),
                    playlist.generatePlaylist(context));

            // Assign the auto playlist's ID to match the one in the MediaStore
            playlist.playlistId = p.getPlaylistId();

            // Save the playlist configuration with GSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter playlistWriter = new FileWriter(context.getExternalFilesDir(null)
                    + "/" + playlist.getPlaylistName() + AUTO_PLAYLIST_EXTENSION);
            playlistWriter.write(gson.toJson(playlist, AutoPlaylist.class));
            playlistWriter.close();

            // Add the playlist to the library and resort the playlist library
            playlistLib.add(playlist);
            Collections.sort(playlistLib);
            notifyPlaylistAdded(playlist);
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    public static void editAutoPlaylist(Context context, AutoPlaylist playlist) {
        try {
            // Save the playlist configuration with GSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter playlistWriter = new FileWriter(context.getExternalFilesDir(null) + "/"
                    + playlist.getPlaylistName() + AUTO_PLAYLIST_EXTENSION);
            playlistWriter.write(gson.toJson(playlist, AutoPlaylist.class));
            playlistWriter.close();

            // Edit the contents of this playlist in the MediaStore
            editPlaylist(context, playlist, playlist.generatePlaylist(context));

            // Remove the old index of this playlist, but keep the Object for reference.
            // Since playlists are compared by Id's, this will remove the old index
            AutoPlaylist oldReference =
                    (AutoPlaylist) playlistLib.remove(playlistLib.indexOf(playlist));

            // If the user renamed the playlist, update it now
            if (!oldReference.getPlaylistName().equals(playlist.getPlaylistName())) {
                renamePlaylist(context, playlist.getPlaylistId(), playlist.getPlaylistName());
                // Delete the old config file so that it doesn't reappear on restart
                //noinspection ResultOfMethodCallIgnored
                new File(context.getExternalFilesDir(null) + "/"
                        + oldReference.getPlaylistName() + AUTO_PLAYLIST_EXTENSION).delete();
            }

            // Add the playlist again. This makes sure that if the values have been cloned before
            // being changed that their values will be updated without having to rescan the
            // entire library
            playlistLib.add(playlist);

            Collections.sort(playlistLib);
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    //
    //          Media file open method
    //

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
        // A somewhat convoluted method for getting a list of songs from a path

        // Songs are put into the queue array list
        // The integer returned is the position in this queue that corresponds to the requested song

        if (isEmpty()) {
            // We depend on the library being scanned, so make sure it's scanned
            // before we go any further
            scanAll(activity);
        }

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
                queue.addAll(getPlaylistEntries(activity, new Playlist(
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.NAME)))));
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
}
