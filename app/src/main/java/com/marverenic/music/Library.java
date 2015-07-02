package com.marverenic.music;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class Library {

    private static final ArrayList<Playlist> playlistLib = new ArrayList<>();
    private static final ArrayList<Song> songLib = new ArrayList<>();
    private static final ArrayList<Artist> artistLib = new ArrayList<>();
    private static final ArrayList<Album> albumLib = new ArrayList<>();
    private static final ArrayList<Genre> genreLib = new ArrayList<>();

    private static final String[] songProjection = new String[]{
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
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
            MediaStore.Audio.Playlists.Members.ALBUM_ID,
            MediaStore.Audio.Playlists.Members.ARTIST_ID
    };

    //
    //          LIBRARY LISTENERS
    //

    // Since it's important to know when the Library has entries added or removed so we can update
    // the UI accordingly, associate listeners to receive callbacks for such events. These listeners
    // will get called only when entries are added or removed -- not changed. This lets us do a lot
    // of things on the UI like adding and removing playlists without having to create the associated
    // Snackbars, AlertDialogs, etc. and is slightly cleaner than passing a callback as a parameter
    // to methods that cause such changes since we don't have to instantiate a single-use Object.

    private static final ArrayList<PlaylistChangeListener> PLAYLIST_LISTENERS = new ArrayList<>();

    public interface PlaylistChangeListener {
        void onPlaylistRemoved(Playlist removed);
        void onPlaylistAdded(Playlist added);
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
     * <b>When using this method MAKE SURE TO CALL {@link Library#removePlaylistListener(PlaylistChangeListener)}
     * WHEN THE ACTIVITY PAUSES -- OTHERWISE YOU WILL CAUSE A LEAK.</b>
     *
     * @param listener A {@link PlaylistChangeListener} to act as a callback
     *                 when the library is changed in any way
     */
    public static void addPlaylistListener(PlaylistChangeListener listener){
        if (!PLAYLIST_LISTENERS.contains(listener)) PLAYLIST_LISTENERS.add(listener);
    }

    /**
     * Remove a {@link PlaylistChangeListener} previously added to listen
     * for library updates.
     * @param listener A {@link PlaylistChangeListener} currently registered
     *                 to recieve a callback when the library gets modified. If it's not already
     *                 registered, then nothing will happen.
     */
    public static void removePlaylistListener(PlaylistChangeListener listener){
        if (PLAYLIST_LISTENERS.contains(listener)) PLAYLIST_LISTENERS.remove(listener);
    }

    /**
     * Private method for notifying registered {@link PlaylistChangeListener}s
     * that the library has lost an entry. (Changing entries doesn't matter)
     */
    private static void notifyPlaylistRemoved(Playlist removed){
        for (PlaylistChangeListener l : PLAYLIST_LISTENERS) l.onPlaylistRemoved(removed);
    }

    /**
     * Private method for notifying registered {@link PlaylistChangeListener}s
     * that the library has gained an entry. (Changing entries doesn't matter)
     */
    private static void notifyPlaylistAdded(Playlist added){
        for (PlaylistChangeListener l : PLAYLIST_LISTENERS) l.onPlaylistAdded(added);
    }

    //
    //          LIBRARY BUILDING METHODS
    //

    /**
     * Scan the MediaStore and update the libraries held in memory
     * @param context {@link Context} to use to open {@link Cursor}s
     */
    public static void scanAll (final Context context){
        resetAll();
        setPlaylistLib(scanPlaylists(context));
        setSongLib(scanSongs(context));
        setArtistLib(scanArtists(context));
        setAlbumLib(scanAlbums(context));
        setGenreLib(scanGenres(context));
        sort();
    }

    /**
     * Scans the MediaStore for songs
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Song}s in the MediaStore
     */
    public static ArrayList<Song> scanSongs (Context context){
        ArrayList<Song> songs = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songProjection,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Song s = new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media._ID)),
                    (cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                            ? context.getString(R.string.no_artist)
                            : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)));

            s.trackNumber = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.TRACK));
            songs.add(s);
        }
        cur.close();

        return songs;
    }

    /**
     * Scans the MediaStore for artists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Artist}s in the MediaStore
     */
    public static ArrayList<Artist> scanArtists (Context context){
        ArrayList<Artist> artists = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                artistProjection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            if (!cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).equals(MediaStore.UNKNOWN_STRING)) {
                artists.add(new Artist(
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Artists._ID)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST))));
            }
            else{
                artists.add(new Artist(
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Artists._ID)),
                        context.getString(R.string.no_artist)));
            }
        }
        cur.close();

        return artists;
    }

    /**
     * Scans the MediaStore for albums
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Album}s in the MediaStore
     */
    public static ArrayList<Album> scanAlbums (Context context){
        ArrayList<Album> albums = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumProjection,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC");
        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            albums.add(new Album(
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Albums._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                    (cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                            ? context.getString(R.string.no_artist)
                            : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))));
        }
        cur.close();

        return albums;
    }

    /**
     * Scans the MediaStore for playlists
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Playlist}s in the MediaStore
     */
    public static ArrayList<Playlist> scanPlaylists (Context context){
        ArrayList<Playlist> playlists = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                playlistProjection,
                null,
                null,
                MediaStore.Audio.Playlists.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            playlists.add(new Playlist(
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.NAME))));
        }
        cur.close();
        return playlists;
    }

    /**
     * Scans the MediaStore for genres
     * @param context {@link Context} to use to open a {@link Cursor}
     * @return An {@link ArrayList} with the {@link Genre}s in the MediaStore
     */
    public static ArrayList<Genre> scanGenres (Context context){
        ArrayList<Genre> genres = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                genreProjection,
                null,
                null,
                MediaStore.Audio.Genres.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            int thisGenreId = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Genres._ID));

            if (cur.getString(cur.getColumnIndex(MediaStore.Audio.Genres.NAME)).equalsIgnoreCase("Unknown")){
                genres.add(new Genre(-1, "Unknown"));
            }
            else {
                genres.add(new Genre(
                        thisGenreId,
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Genres.NAME))));

                // Associate all songs in this genre by setting the genreID field of each song in the genre

                Cursor genreCur = context.getContentResolver().query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", thisGenreId),
                        new String[]{MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.IS_MUSIC + " != 0 ", null, null);
                genreCur.moveToFirst();

                final int ID_INDEX = genreCur.getColumnIndex(MediaStore.Audio.Media._ID);
                for (int j = 0; j < genreCur.getCount(); j++) {
                    genreCur.moveToPosition(j);
                    final Song s = findSongById(genreCur.getInt(ID_INDEX));
                    if (s != null) s.genreId = thisGenreId;
                }
                genreCur.close();
            }
        }
        cur.close();

        return genres;
    }

    //
    //          LIBRARY STORAGE METHODS
    //

    /**
     * Remove all library entries from memory
     */
    public static void resetAll(){
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
    public static void setPlaylistLib(ArrayList<Playlist> newLib){
        playlistLib.clear();
        playlistLib.addAll(newLib);
    }

    /**
     * Replace the song library in memory with another one
     * @param newLib The new song library
     */
    public static void setSongLib(ArrayList<Song> newLib){
        songLib.clear();
        songLib.addAll(newLib);
    }

    /**
     * Replace the album library in memory with another one
     * @param newLib The new album library
     */
    public static void setAlbumLib(ArrayList<Album> newLib){
        albumLib.clear();
        albumLib.addAll(newLib);
    }

    /**
     * Replace the artist library in memory with another one
     * @param newLib The new artist library
     */
    public static void setArtistLib(ArrayList<Artist> newLib){
        artistLib.clear();
        artistLib.addAll(newLib);
    }

    /**
     * Replace the genre library in memory with another one
     * @param newLib The new genre library
     */
    public static void setGenreLib(ArrayList<Genre> newLib){
        genreLib.clear();
        genreLib.addAll(newLib);
    }

    /**
     * Sorts the libraries in memory using the default {@link Library} sort methods
     */
    public static void sort (){
        sortSongList(songLib);
        sortAlbumList(albumLib);
        sortArtistList(artistLib);
        sortPlaylistList(playlistLib);
        sortGenreList(genreLib);
    }

    /**
     * @return true if the library is populated with any entries
     */
    public static boolean isEmpty (){
        return songLib.isEmpty() && albumLib.isEmpty() && artistLib.isEmpty() && playlistLib.isEmpty() && genreLib.isEmpty();
    }

    /**
     * @return An {@link ArrayList} of {@link Playlist}s in the MediaStore
     */
    public static ArrayList<Playlist> getPlaylists(){
        return playlistLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Song}s in the MediaStore
     */
    public static ArrayList<Song> getSongs(){
        return songLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Album}s in the MediaStore
     */
    public static ArrayList<Album> getAlbums(){
        return albumLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Artist}s in the MediaStore
     */
    public static ArrayList<Artist> getArtists(){
        return artistLib;
    }

    /**
     * @return An {@link ArrayList} of {@link Genre}s in the MediaStore
     */
    public static ArrayList<Genre> getGenres(){
        return genreLib;
    }

    //
    //          LIBRARY SORT METHODS
    //

    /**
     * Sorts an {@link ArrayList} of {@link Song}s by name, ignoring leading "the "'s and "a "'s
     * @param songs the {@link ArrayList} to be sorted
     */
    public static void sortSongList(final ArrayList<Song> songs){
        Comparator<Song> songComparator = new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String o1c = o1.songName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.songName.toLowerCase(Locale.ENGLISH);
                if (o1c.startsWith("the ")) {
                    o1c = o1c.substring(4);
                } else if (o1c.startsWith("a ")) {
                    o1c = o1c.substring(2);
                }
                if (o2c.startsWith("the ")) {
                    o2c = o2c.substring(4);
                } else if (o2c.startsWith("a ")) {
                    o2c = o2c.substring(2);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(songs, songComparator);
    }

    /**
     * Sorts an {@link ArrayList} of {@link Album}s by name, ignoring leading "the "'s and "a "'s
     * @param albums the {@link ArrayList} to be sorted
     */
    public static void sortAlbumList (final ArrayList<Album> albums){
        Comparator<Album> albumComparator = new Comparator<Album>() {
            @Override
            public int compare(Album o1, Album o2) {
                String o1c = o1.albumName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.albumName.toLowerCase(Locale.ENGLISH);
                if (o1c.startsWith("the ")) {
                    o1c = o1c.substring(4);
                } else if (o1c.startsWith("a ")) {
                    o1c = o1c.substring(2);
                }
                if (o2c.startsWith("the ")) {
                    o2c = o2c.substring(4);
                } else if (o2c.startsWith("a ")) {
                    o2c = o2c.substring(2);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(albums, albumComparator);
    }

    /**
     * Sorts an {@link ArrayList} of {@link Artist}s by name, ignoring leading "the "'s and "a "'s
     * @param artists the {@link ArrayList} to be sorted
     */
    public static void sortArtistList (final ArrayList<Artist> artists){
        Comparator<Artist> artistComparator = new Comparator<Artist>() {
            @Override
            public int compare(Artist o1, Artist o2) {
                String o1c = o1.artistName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.artistName.toLowerCase(Locale.ENGLISH);
                if (o1c.startsWith("the ")) {
                    o1c = o1c.substring(4);
                } else if (o1c.startsWith("a ")) {
                    o1c = o1c.substring(2);
                }
                if (o2c.startsWith("the ")) {
                    o2c = o2c.substring(4);
                } else if (o2c.startsWith("a ")) {
                    o2c = o2c.substring(2);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(artists, artistComparator);
    }

    /**
     * Sorts an {@link ArrayList} of {@link Playlist}s by name
     * @param playlists {@link ArrayList} to be sorted
     */
    public static void sortPlaylistList (final ArrayList<Playlist> playlists){
        Comparator<Playlist> playlistComparator = new Comparator<Playlist>() {
            @Override
            public int compare(Playlist o1, Playlist o2) {
                return o1.playlistName.compareToIgnoreCase(o2.playlistName);
            }
        };
        Collections.sort(playlists, playlistComparator);
    }

    /**
     * Sorts an {@link ArrayList} of {@link Genre}s by name
     * @param genres the {@link ArrayList} to be sorted
     */
    public static void sortGenreList (final ArrayList<Genre> genres){
        Comparator<Genre> genreComparator = new Comparator<Genre>() {
            @Override
            public int compare(Genre o1, Genre o2) {
                return o1.genreName.compareToIgnoreCase(o2.genreName);
            }
        };
        Collections.sort(genres, genreComparator);
    }

    //
    //          LIBRARY SEARCH METHODS
    //

    /**
     * Finds a {@link Song} in the library based on its Id
     * @param songId the MediaStore Id of the {@link Song}
     * @return A {@link Song} with a matching Id
     */
    public static Song findSongById (int songId){
        for (Song s : songLib){
            if (s.songId == songId){
                return s;
            }
        }
        return null;
    }

    /**
     * Build an {@link ArrayList} of {@link Song}s from a list of id's. Doesn't require the library to be loaded
     * @param songIDs The list of song ids to convert to {@link Song}s
     * @param context The {@link Context} used to open a {@link Cursor}
     * @return An {@link ArrayList} of {@link Song}s with ids matching those of the songIDs parameter
     */
    public static ArrayList<Song> buildSongListFromIds (int[] songIDs, Context context){
        ArrayList<Song> contents = new ArrayList<>();
        if (songIDs.length == 0) return contents;

        String query = MediaStore.Audio.Media._ID + " IN(?";
        String[] ids = new String[songIDs.length];
        ids[0] = Integer.toString(songIDs[0]);

        for (int i = 1; i < songIDs.length; i++){
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

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Song s = new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media._ID)),
                    (cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                            ? context.getString(R.string.no_artist)
                            : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)));

            s.trackNumber = cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.TRACK));
            contents.add(s);
        }
        cur.close();

        // Sort the contents of the list so that it matches the order of the int array
        ArrayList<Song> songs = new ArrayList<>();
        Song dummy = new Song(null, 0, null, null, 0, null, 0, 0);
        for (int i : songIDs){
            dummy.songId = i;
            // Because Songs are equal if their ids are equal, we can use a dummy song with the ID
            // we want to find it in the list
            songs.add(contents.get(contents.indexOf(dummy)));
        }

        return songs;
    }

    /**
     * Finds a {@link Artist} in the library based on its Id
     * @param artistId the MediaStore Id of the {@link Artist}
     * @return A {@link Artist} with a matching Id
     */
    public static Artist findArtistById (int artistId){
        for (Artist a : artistLib){
            if (a.artistId == artistId){
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
    public static Album findAlbumById (int albumId){
        // Returns the first Artist object in the library with a matching id
        for (Album a : albumLib){
            if (a.albumId == albumId){
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
    public static Genre findGenreById (int genreId){
        // Returns the first Genre object in the library with a matching id
        for (Genre g : genreLib){
            if (g.genreId == genreId){
                return g;
            }
        }
        return null;
    }

    public static Artist findArtistByName (String artistName){
        final String trimmedQuery = artistName.trim();
        for (Artist a : artistLib){
            if (a.artistName.equalsIgnoreCase(trimmedQuery))
                return a;
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
    public static ArrayList<Song> getPlaylistEntries (Context context, Playlist playlist){
        ArrayList<Song> songEntries = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId),
                playlistEntryProjection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            songEntries.add(new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_ID)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST_ID))));
        }
        cur.close();

        return songEntries;
    }

    /**
     * Get a list of songs on a certain album
     * @param album The {@link Album} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s contained in the album
     */
    public static ArrayList<Song> getAlbumEntries (Album album){
        ArrayList<Song> songEntries = new ArrayList<>();

        for (Song s : songLib){
            if (s.albumId == album.albumId){
                songEntries.add(s);
            }
        }

        Collections.sort(songEntries, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return ((Integer) o1.trackNumber).compareTo(o2.trackNumber);
            }
        });

        return songEntries;
    }

    /**
     * Get a list of songs by a certain artist
     * @param artist The {@link Artist} to get the entries of
     * @return An {@link ArrayList} of {@link Song}s by the artist
     */
    public static ArrayList<Song> getArtistSongEntries (Artist artist){
        ArrayList<Song> songEntries = new ArrayList<>();

        for(Song s : songLib){
            if (s.artistId == artist.artistId){
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
    public static ArrayList<Album> getArtistAlbumEntries (Artist artist){
        ArrayList<Album> albumEntries = new ArrayList<>();

        for (Album a : albumLib){
            if (a.artistId == artist.artistId){
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
    public static ArrayList<Song> getGenreEntries (Genre genre){
        ArrayList<Song> songEntries = new ArrayList<>();

        for(Song s : songLib){
            if (s.genreId == genre.genreId){
                songEntries.add(s);
            }
        }

        return songEntries;
    }

    //
    //          PLAYLIST WRITING METHODS
    //

    /**
     * Add a new playlist to the MediaStore
     * @param view A {@link View} to put a {@link Snackbar} in. Will also be used to get a {@link Context}.
     * @param playlistName The name of the new playlist
     * @param songList An {@link ArrayList} of {@link Song}s to populate the new playlist
     * @return The Playlist that was added to the library
     */
    public static Playlist createPlaylist(final View view, final String playlistName, @Nullable final ArrayList<Song> songList){
        final Context context = view.getContext();
        String trimmedName = playlistName.trim();

        setPlaylistLib(scanPlaylists(context));

        String error = verifyPlaylistName(context, trimmedName);
        if (error != null){
            Snackbar
                    .make(
                            view,
                            error,
                            Snackbar.LENGTH_SHORT)
                    .show();
            return null;
        }

        // Add the playlist to the MediaStore
        final Playlist created = makePlaylist(context, trimmedName, songList);

        Snackbar
                .make(
                        view,
                        String.format(context.getResources().getString(R.string.message_created_playlist), playlistName),
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
     * Test a playlist name to make sure it is valid when making a new playlist. Invalid playlist names
     * are empty or already exist in the MediaStore
     * @param context A {@link Context} used to get localized Strings
     * @param playlistName The playlist name that needs to be validated
     * @return null if there is no error, or a {@link String} describing the error that can be
     *         presented to the user
     */
    public static String verifyPlaylistName (final Context context, final String playlistName){
        if (playlistName.length() == 0) return null;

        String trimmedName = playlistName.trim();
        if (trimmedName.length() == 0){
            return context.getResources().getString(R.string.error_hint_empty_playlist);
        }

        for (Playlist p : playlistLib){
            if (p.playlistName.equalsIgnoreCase(trimmedName)){
                return context.getResources().getString(R.string.error_hint_duplicate_playlist);
            }
        }
        return null;
    }

    /**
     * Removes a playlist from the MediaStore
     * @param view A {@link View} to show a {@link Snackbar} and to get a {@link Context} used to edit the MediaStore
     * @param playlist A {@link Playlist} which will be removed from the MediaStore
     */
    public static void removePlaylist(final View view, final Playlist playlist){
        final Context context = view.getContext();
        final ArrayList<Song> entries = getPlaylistEntries(context, playlist);

        deletePlaylist(context, playlist);

        Snackbar
                .make(
                        view,
                        String.format(context.getString(R.string.message_removed_playlist), playlist),
                        Snackbar.LENGTH_LONG)
                .setAction(
                        context.getString(R.string.action_undo),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                makePlaylist(context, playlist.playlistName, entries);
                            }
                        })
                .show();
    }

    /**
     * Replace the entries of a playlist in the MediaStore with a new {@link ArrayList} of {@link Song}s
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param newSongList An {@link ArrayList} of {@link Song}s to overwrite the list contained in the MediaStore
     */
    public static void editPlaylist(final Context context, final Playlist playlist, final ArrayList<Song> newSongList){
        // Clear the playlist...
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId);
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);

        // Then add all of the songs to it
        ContentValues[] values = new ContentValues[newSongList.size()];
        for (int i = 0; i < newSongList.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i + 1);
            values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, newSongList.get(i).songId);
        }
        resolver.bulkInsert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    /**
     * Append a song to the end of a playlist. Alerts the user about duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param song The {@link Song} to be added to the playlist in the MediaStore
     */
    public static void addPlaylistEntry(final Context context, final Playlist playlist, final Song song){
        // Public method to add a song to a playlist
        // Checks the playlist for duplicate entries
        if (getPlaylistEntries(context, playlist).contains(song)){
            new AlertDialog.Builder(context)
                    .setTitle("Add duplicate song")
                    .setMessage("Playlist \"" + playlist +"\" already contains song \"" + song + "\". Do you want to add a duplicate entry?")
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addSongToEndOfPlaylist(context, playlist, song);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        else{
            addSongToEndOfPlaylist(context, playlist, song);
        }
    }

    /**
     * Append a list of songs to the end of a playlist. Alerts the user about duplicates
     * @param view A {@link View} to put a {@link android.support.design.widget.Snackbar} in. Will
     *             also be used to get a {@link Context}.
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param songs The {@link ArrayList} of {@link Song}s to be added to the playlist in the MediaStore
     */
    public static void addPlaylistEntries(final View view, final Playlist playlist, final ArrayList<Song> songs){
        // Public method to add songs to a playlist
        // Checks the playlist for duplicate entries

        final Context context = view.getContext();

        int duplicateCount = 0;
        final ArrayList<Song> currentEntries = getPlaylistEntries(context, playlist);
        final ArrayList<Song> newEntries = new ArrayList<>();

        for (Song s : songs){
            if (currentEntries.contains(s))duplicateCount++;
            else newEntries.add(s);
        }

        if (duplicateCount > 0){
            //TODO String Resources
            AlertDialog.Builder alert = new AlertDialog.Builder(context).setTitle("Add duplicate entries?");

            if (duplicateCount == songs.size()) {
                alert
                        .setMessage("This playlist already contains all of these songs. Adding them again will result in duplicates.")
                        .setPositiveButton("Add duplicates", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
                                Snackbar.make(
                                        view,
                                        "Added " + songs.size() + " song(s) to the end of playlist \"" + playlist.playlistName + "\"",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Undo", new View.OnClickListener() {
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
                        .setNeutralButton("Cancel", null);
            }
            else{
                alert
                        .setMessage(context.getResources().getQuantityString(R.plurals.playlistConfirmSomeDuplicates, duplicateCount, duplicateCount))
                        .setPositiveButton("Add new", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, newEntries);
                                Snackbar.make(
                                        view,
                                        "Added " + newEntries.size() + " song(s) to the end of playlist \"" + playlist.playlistName + "\"",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Undo", new View.OnClickListener() {
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
                        .setNegativeButton("Add all", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
                                Snackbar.make(
                                        view,
                                        "Added " + songs.size() + " song(s) to the end of playlist \"" + playlist.playlistName + "\"",
                                        Snackbar.LENGTH_LONG)
                                        .setAction("Undo", new View.OnClickListener() {
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
                        .setNeutralButton("Cancel", null);
            }

            alert.show();
        }
        else{
            addSongsToEndOfPlaylist(context, playlist, songs);
            Snackbar.make(
                    view,
                    "Added " + newEntries.size() + " song(s) to the end of playlist \"" + playlist.playlistName + "\"",
                    Snackbar.LENGTH_LONG)
                    .setAction("Undo", new View.OnClickListener() {
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
     * Add a new playlist to the MediaStore. Outside of this class, use
     * {@link Library#createPlaylist(View, String, ArrayList)} instead
     * <b>This method DOES NOT validate inputs or display a confirmation message to the user</b>.
     * @param context A {@link Context} used to edit the MediaStore
     * @param playlistName The name of the new playlist
     * @param songList An {@link ArrayList} of {@link Song}s to populate the new playlist
     * @return The Playlist that was added to the library
     */
    private static Playlist makePlaylist(final Context context, final String playlistName, @Nullable final ArrayList<Song> songList){
        String trimmedName = playlistName.trim();

        // Add the playlist to the MediaStore
        ContentValues mInserts = new ContentValues();
        mInserts.put(MediaStore.Audio.Playlists.NAME, trimmedName);
        mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        Uri newPlaylistUri = context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);

        // Update the playlist library & resort it
        setPlaylistLib(scanPlaylists(context));
        sortPlaylistList(playlistLib);

        // Get the id of the new playlist
        Cursor cursor = context.getContentResolver().query(
                newPlaylistUri,
                new String[] {MediaStore.Audio.Playlists._ID},
                null, null, null);

        cursor.moveToFirst();
        final Playlist playlist = new Playlist(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)), playlistName);
        cursor.close();

        // If we have a list of songs, associate it with the playlist
        if(songList != null) {
            ContentValues[] values = new ContentValues[songList.size()];

            for (int i = 0; i < songList.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);
                values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songList.get(i).songId);
            }

            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId);
            ContentResolver resolver = context.getContentResolver();

            resolver.bulkInsert(uri, values);
            resolver.notifyChange(Uri.parse("content://media"), null);
        }

        notifyPlaylistAdded(playlist);
        return playlist;
    }

    /**
     * Removes a playlist from the MediaStore
     * @param context A {@link Context} to update the MediaStore
     * @param playlist A {@link Playlist} whose matching playlist will be removed from the MediaStore
     */
    public static void deletePlaylist(final Context context, final Playlist playlist){
        // Remove the playlist from the MediaStore
        context.getContentResolver().delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{playlist.playlistId + ""});

        // Update the playlist library & resort it
        playlistLib.clear();
        setPlaylistLib(scanPlaylists(context));
        sortPlaylistList(playlistLib);
        notifyPlaylistRemoved(playlist);
    }

    /**
     * Append a song to the end of a playlist. Doesn't check for duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param song The {@link Song} to be added to the playlist in the MediaStore
     */
    private static void addSongToEndOfPlaylist (final Context context, final Playlist playlist, final Song song){
        // Private method to add a song to a playlist
        // This method does the actual operation to the MediaStore
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId),
                null, null, null,
                MediaStore.Audio.Playlists.Members.TRACK + " ASC");

        long count = 0;
        if (cur.moveToLast()) count = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
        cur.close();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count + 1);
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song.songId);

        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId);
        ContentResolver resolver = context.getContentResolver();
        resolver.insert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    /**
     * Append a list of songs to the end of a playlist. Doesn't check for duplicates
     * @param context A {@link Context} to open a {@link Cursor}
     * @param playlist The {@link Playlist} to edit in the MediaStore
     * @param songs The {@link ArrayList} of {@link Song}s to be added to the playlist in the MediaStore
     */
    private static void addSongsToEndOfPlaylist(final Context context, final Playlist playlist, final ArrayList<Song> songs){
        // Private method to add a song to a playlist
        // This method does the actual operation to the MediaStore
        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId),
                null, null, null,
                MediaStore.Audio.Playlists.Members.TRACK + " ASC");

        long count = 0;
        if (cur.moveToLast()) count = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TRACK));
        cur.close();

        ContentValues[] values = new ContentValues[songs.size()];
        for (int i = 0; i < songs.size(); i++) {
            values[i] = new ContentValues();
            values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, count + 1);
            values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(i).songId);
        }

        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId);
        ContentResolver resolver = context.getContentResolver();
        resolver.bulkInsert(uri, values);
        resolver.notifyChange(Uri.parse("content://media"), null);
    }

    //
    //          Media file open method
    //

    /**
     * Get a list of songs to play for a certain input file. If a song is passed as the file, then
     * the list will include other songs in the same directory. If a playlist is passed as the file,
     * then the playlist will be opened as a regular playlist.
     *
     * @param context A {@link Context} to use when building the list
     * @param file The {@link File} which the list will be built around
     * @param type The MIME type of the file being opened
     * @param queue An {@link ArrayList} which will be populated with the {@link Song}s
     * @return The position that this list should be started from
     * @throws IOException
     */
    public static int getSongListFromFile(Context context, File file, String type, final ArrayList<Song> queue) throws IOException{
        // A somewhat convoluted method for getting a list of songs from a path

        // Songs are put into the queue array list
        // The integer returned is the position in this queue that corresponds to the requested song

        if (isEmpty()){
            // We depend on the library being scanned, so make sure it's scanned before we go any further
            scanAll(context);
        }

        // PLAYLISTS
        if (type.equals("audio/x-mpegurl") || type.equals("audio/x-scpls") || type.equals("application/vnd.ms-wpl")){
            // If a playlist was opened, try to find and play its entry from the MediaStore
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Playlists.DATA + "=?",
                    new String[] {file.getPath()},
                    MediaStore.Audio.Playlists.NAME + " ASC");

            // If the media store contains this playlist, play it like a regular playlist
            if (cur.getCount() > 0){
                cur.moveToFirst();
                queue.addAll(getPlaylistEntries(context, new Playlist(
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

                if (lines.size() > 0){
                    // Do stuff
                }

            }*/
            cur.close();
            // Return 0 to start at the beginning of the playlist
            return 0;
        }
        // ALL OTHER TYPES OF MEDIA
        else {
            // If the file isn't a playlist, use a content resolver to find the song and play it
            // Find all songs in the directory
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATA + " like ?",
                    new String[] {"%" + file.getParent() + "/%"},
                    MediaStore.Audio.Media.DATA + " ASC");

            // Create song objects to match those in the music library
            for (int i = 0; i < cur.getCount(); i++) {
                cur.moveToPosition(i);
                queue.add(new Song(
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media._ID)),
                        (cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                                ? "Unknown Artist"
                                : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID))));
            }
            cur.close();

            // Find the position of the song that should be played
            for(int i = 0; i < queue.size(); i++){
                if (queue.get(i).location.equals(file.getPath())) return i;
            }
        }

        return 0;
    }
}