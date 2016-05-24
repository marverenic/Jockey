package com.marverenic.music.instances;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.util.LongSparseArray;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.marverenic.music.utils.Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Data holder class used by {@link Library}
 */
public final class LibraryInstance {

    private List<Song> mSongs;
    private List<Album> mAlbums;
    private List<Artist> mArtists;
    private List<Genre> mGenres;
    private List<Playlist> mPlaylists;

    private LongSparseArray<Song> mSongLookupTable;

    private LongSparseArray<List<Song>> mAlbumContents;
    private LongSparseArray<List<Song>> mArtistSongContents;
    private LongSparseArray<List<Album>> mArtistAlbumContents;

    /**
     * Instantiates a new Library instance with no contents. Note that
     * {@link Collections#EMPTY_LIST} is used as one of the default backing lists, so any calls to
     * the applicable getters of this object will return immutable lists.
     */
    protected LibraryInstance() {
        mSongs = Collections.emptyList();
        mAlbums = Collections.emptyList();
        mArtists = Collections.emptyList();
        mGenres = Collections.emptyList();
        mPlaylists = Collections.emptyList();

        mSongLookupTable = new LongSparseArray<>();

        mAlbumContents = new LongSparseArray<>();
        mArtistSongContents = new LongSparseArray<>();
        mArtistAlbumContents = new LongSparseArray<>();
    }

    /**
     * Populates this LibraryInstance with data from the {@link MediaStore}
     * @param context A Context used to open {@link Cursor Cursors} and obtain other information
     *                required to populate the library
     */
    public void scanAll(Context context) {
        mSongs = scanSongs(context);
        createSongLookupTable();

        mArtists = scanArtists(context);
        mAlbums = scanAlbums(context);
        mGenres = scanGenres(context);
        mPlaylists = scanPlaylists(context);

        scanArtistSongContents();
        scanArtistAlbumContents();
        scanAlbumContents();

        trimEmptyArtists();
        trimEmptyAlbums();
    }

    /**
     * Forces the playlist contents of this instance to be reloaded from disk
     * @param context A Context to use to open a {@link Cursor} and read files from the disk
     */
    protected void refreshPlaylists(Context context) {
        mPlaylists = scanPlaylists(context);
    }

    public static List<Song> scanSongs(Context context) {
        String selection = Library.getDirectorySelection(context);
        if (selection == null) {
            selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        } else {
            selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + selection;
        }

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Library.SONG_PROJECTION,
                selection,
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
     * Updates the contents of {@link #mSongLookupTable} so that songs can be found by their id in
     * O(1) time later
     */
    private void createSongLookupTable() {
        for (Song s : getSongs()) {
            mSongLookupTable.put(s.getSongId(), s);
        }
    }

    /**
     * Scans the MediaStore for artists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Artist}s in the MediaStore
     */
    public static List<Artist> scanArtists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                Library.ARTIST_PROJECTION,
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
     * Associates songs with their corresponding artist by building a map from
     * {@link Artist Artists} -> {@link Song Songs}. This data is stored in
     * {@link #mArtistSongContents}
     */
    private void scanArtistSongContents() {
        for (Song s : getSongs()) {
            List<Song> artistContents = mArtistSongContents.get(s.getArtistId());

            if (artistContents == null) {
                artistContents = new ArrayList<>();
                mArtistSongContents.put(s.getArtistId(), artistContents);
            }

            Util.insertInOrder(artistContents, s);
        }
    }

    /**
     * Associates albums with their corresponding album by building a map from
     * {@link Artist Artists} -> {@link Album Albums}. This data is stored in
     * {@link #mArtistAlbumContents}
     */
    private void scanArtistAlbumContents() {
        for (Album a : getAlbums()) {
            List<Album> artistContents = mArtistAlbumContents.get(a.getArtistId());

            if (artistContents == null) {
                artistContents = new ArrayList<>();
                mArtistAlbumContents.put(a.getArtistId(), artistContents);
            }

            Util.insertInOrder(artistContents, a);
        }
    }

    /**
     * Scans the MediaStore for albums
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Album}s in the MediaStore
     */
    public static List<Album> scanAlbums(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                Library.ALBUM_PROJECTION,
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
     * Associates songs with their corresponding album by building a map from
     * {@link Album Albums} -> {@link Song Songs}. This data is stored in {@link #mAlbumContents}
     */
    private void scanAlbumContents() {
        for (Song s : getSongs()) {
            List<Song> albumContents = mAlbumContents.get(s.getAlbumId());

            if (albumContents == null) {
                albumContents = new ArrayList<>();
                mAlbumContents.put(s.getAlbumId(), albumContents);
            }

            Util.insertInOrder(albumContents, Song.TRACK_COMPRATOR, s);
        }
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
                Library.GENRE_PROJECTION,
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
     * Scans the MediaStore for playlists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Playlist}s in the MediaStore
     */
    private static List<Playlist> scanPlaylists(Context context) {
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                Library.PLAYLIST_PROJECTION,
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
                        + "/" + p.getPlaylistName() + Library.AUTO_PLAYLIST_EXTENSION)
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
    private static List<AutoPlaylist> scanAutoPlaylists(Context context) {
        List<AutoPlaylist> autoPlaylists = new ArrayList<>();
        final Gson gson = new Gson();

        try {
            File externalFiles = new File(context.getExternalFilesDir(null) + "/");

            if (externalFiles.exists() || externalFiles.mkdirs()) {
                String[] files = externalFiles.list();
                for (String s : files) {
                    if (s.endsWith(Library.AUTO_PLAYLIST_EXTENSION)) {
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
     * Removes artists that have no songs from this library instance
     */
    private void trimEmptyArtists() {
        for (Iterator<Artist> iter = getArtists().iterator(); iter.hasNext(); ) {
            Artist a = iter.next();

            List<Song> songContents = getArtistSongEntries(a);
            if (songContents == null || songContents.isEmpty()) {
                iter.remove();
            }
        }
    }

    /**
     * Removes albums that have no songs from this library instance
     */
    private void trimEmptyAlbums() {
        for (Iterator<Album> iter = getAlbums().iterator(); iter.hasNext(); ) {
            Album a = iter.next();

            List<Song> songContents = getAlbumEntries(a);
            if (songContents == null || songContents.isEmpty()) {
                iter.remove();
            }
        }
    }

    /**
     * Removes all data from this instance
     */
    public void clear() {
        mSongs.clear();
        mAlbums.clear();
        mArtists.clear();
        mGenres.clear();
        mPlaylists.clear();

        mSongLookupTable.clear();

        mAlbumContents.clear();
        mArtistSongContents.clear();
        mArtistAlbumContents.clear();
    }

    /**
     * Sorts the data contained in each library (song, album, artist, etc.) held by this instance
     */
    public void sort() {
        Collections.sort(mSongs);
        Collections.sort(mAlbums);
        Collections.sort(mArtists);
        Collections.sort(mGenres);
        Collections.sort(mPlaylists);
    }

    /**
     * Checks whether this instance contains any elements
     * @return {@code true} if there are no songs, albums, artists, genres, and playlists,
     *         {@code true} otherwise
     */
    public boolean isEmpty() {
        return mSongs.isEmpty() && mAlbums.isEmpty() && mArtists.isEmpty()
                && mGenres.isEmpty() && mPlaylists.isEmpty();
    }

    /**
     * Gets the list of songs contained by this instance
     * @return A list of songs from the {@link MediaStore} currently held by this object
     */
    public List<Song> getSongs() {
        return mSongs;
    }

    /**
     * Gets the list of albums contained by this instance
     * @return A list of albums from the {@link MediaStore} currently held by this object
     */
    public List<Album> getAlbums() {
        return mAlbums;
    }

    /**
     * Gets the list of artists contained by this instance
     * @return A list of albums from the {@link MediaStore} currently held by this object
     */
    public List<Artist> getArtists() {
        return mArtists;
    }

    /**
     * Gets the list of genres contained by this instance
     * @return A list of genres from the {@link MediaStore} currently held by this object
     */
    public List<Genre> getGenres() {
        return mGenres;
    }

    /**
     * Gets the list of playlists contained by this instance
     * @return A list of playlists from the {@link MediaStore} currently held by this object
     */
    public List<Playlist> getPlayists() {
        return mPlaylists;
    }

    /**
     * Gets a {@link Song} based on its {@link Song#getSongId() songId} field. This method completes
     * in O(1) time
     * @param id The ID to search for
     * @return A {@link Song} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Song findSongById(long id) {
        return mSongLookupTable.get(id);
    }

    /**
     * Gets an {@link Artist} based on its {@link Artist#getArtistId() artistId} field. This method
     * completes in O(n) time
     * @param id The ID to search for
     * @return A {@link Artist} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Artist findArtistById(long id) {
        for (Artist a : getArtists()) {
            if (a.getArtistId() == id) {
                return a;
            }
        }
        return null;
    }

    /**
     * Gets an {@link Album} based on its {@link Album#getAlbumId() albumId} field. This method
     * completes in O(n) time
     * @param id The ID to search for
     * @return A {@link Album} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Album findAlbumById(long id) {
        for (Album a : getAlbums()) {
            if (a.getAlbumId() == id) {
                return a;
            }
        }
        return null;
    }

    /**
     * Gets a {@link Playlist} based on its {@link Playlist#getPlaylistId() playlistId} field. This
     * method completes in O(n) time
     * @param id The ID to search for
     * @return A {@link Playlist} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Playlist findPlaylistById(long id) {
        for (Playlist p : getPlayists()) {
            if (p.getPlaylistId() == id) {
                return p;
            }
        }
        return null;
    }

    /**
     * Gets a {@link Genre} based on its {@link Genre#getGenreId() genreId} field. This method
     * completes in O(n) time
     * @param id The ID to search for
     * @return A {@link Genre} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Genre findGenreById(long id) {
        for (Genre g : getGenres()) {
            if (g.getGenreId() == id) {
                return g;
            }
        }
        return null;
    }

    /**
     * Gets an {@link Artist} based on its {@link Artist#getArtistName() artistName} field. This
     * method uses an O(n) iteration, but takes longer to perform a String comparison
     * @param name The name to search for
     * @return A {@link Artist} from the {@link MediaStore} with a matching ID, or {@code null} if
     *         no matching element was found
     */
    public Artist findArtistByName(String name) {
        final String trimmedQuery = name.trim();
        for (Artist a : getArtists()) {
            if (a.getArtistName().equalsIgnoreCase(trimmedQuery)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Gets the songs in an album as written in this instance. This method completes in O(1) time.
     * @param album The album to lookup the contents of
     * @return The entries in the album, sorted by track number
     * @see #getAlbumEntries(long) To perform this lookup using an album ID instead
     */
    public List<Song> getAlbumEntries(Album album) {
        return getAlbumEntries(album.getAlbumId());
    }

    /**
     * Gets the songs in an album as written in this instance. This method completes in O(1) time.
     * @param id The {@link Album#getAlbumId() albumID} of the album to lookup the contents of
     * @return The entries in the album, sorted by track number
     * @see #getAlbumEntries(Album) To perform this lookup using an album Object instead
     */
    public List<Song> getAlbumEntries(long id) {
        return mAlbumContents.get(id);
    }

    /**
     * Gets the songs by an artist as written in this instance. This method completes in O(1) time.
     * @param artist The artist to lookup the contents of
     * @return All songs in this instance that are by the given artist, sorted by name
     * @see #getArtistSongEntries(long) To perform this lookup using an artist ID instead
     */
    public List<Song> getArtistSongEntries(Artist artist) {
        return getArtistSongEntries(artist.getArtistId());
    }

    /**
     * Gets the songs by an artist as written in this instance. This method completes in O(1) time.
     * @param id The {@link Artist#getArtistId() artistID} to lookup the contents of
     * @return All songs in this instance that are by the given artist, sorted by name
     * @see #getArtistSongEntries(long) To perform this lookup using an artist Object instead
     */
    public List<Song> getArtistSongEntries(long id) {
        return mArtistSongContents.get(id);
    }

    /**
     * Gets the albums by an artist as written in this instance. This method completes in O(1) time.
     * @param artist The artist to lookup the contents of
     * @return All albums in this instance that are by the given artist, sorted by name
     * @see #getArtistSongEntries(long) To perform this lookup using an artist ID instead
     */
    public List<Album> getArtistAlbumEntries(Artist artist) {
        return getArtistAlbumEntries(artist.getArtistId());
    }

    /**
     * Gets the albums by an artist as written in this instance. This method completes in O(1) time.
     * @param id The {@link Artist#getArtistId() artistID} to lookup the contents of
     * @return All songs in this instance that are by the given artist, sorted by name
     * @see #getArtistAlbumEntries(Artist) To perform this lookup using an artist Object instead
     */
    public List<Album> getArtistAlbumEntries(long id) {
        return mArtistAlbumContents.get(id);
    }

    /**
     * Gets the songs that are associated with a particular genre in this instance. This method
     * completes in O(n) time.
     * @param genre The genre to lookup the contents of
     * @return All songs in this instance that are in the given genre, sorted by name
     * @see #getGenreEntries(long) To perform this lookup using a genre ID instead
     */
    public List<Song> getGenreEntries(Genre genre) {
        return getGenreEntries(genre.getGenreId());
    }

    /**
     * Gets the songs that are associated with a particular genre in this instance. This method
     * completes in O(n) time.
     * @param id The {@link Genre#getGenreId() genreID} to lookup the contents of
     * @return All songs in this instance that are in the given genre, sorted by name
     * @see #getGenreEntries(Genre) To perform this lookup using a genre Object instead
     */
    public List<Song> getGenreEntries(long id) {
        List<Song> contents = new ArrayList<>();
        for (Song s : getSongs()) {
            if (s.getGenreId() == id) {
                Util.insertInOrder(contents, s);
            }
        }
        return contents;
    }

}
