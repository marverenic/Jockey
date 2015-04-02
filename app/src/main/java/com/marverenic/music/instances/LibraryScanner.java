package com.marverenic.music.instances;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.marverenic.music.R;
import com.marverenic.music.utils.Themes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class LibraryScanner {

    private static final String FILENAME_PLAYLISTS = "library-playlists.json";
    private static final String FILENAME_SONGS = "library-songs.json";
    private static final String FILENAME_ARTISTS = "library-artists.json";
    private static final String FILENAME_ALBUMS = "library-albums.json";
    private static final String FILENAME_GENRES = "library-genres.json";

    private static boolean loaded = false;

    //
    //          LIBRARY BUILDING METHODS
    //

    // Refresh the entire library
    public static void scanAll (final Context context, final boolean attemptReload, final boolean mergeWithMediaStore){

        Library.resetAll();
        if (!attemptReload || !readLibrary(context, mergeWithMediaStore)) {
            Library.setPlaylistLib(scanPlaylists(context));
            Library.setSongLib(scanSongs(context));
            Library.setArtistLib(scanArtists(context));
            Library.setAlbumLib(scanAlbums(context));
            Library.setGenreLib(scanGenres(context));
        }
        Library.sort();

        loaded = true;

        writeLibrary(context);
    }

    public static void refresh(final Context context, final boolean attemptReload, final boolean mergeWithMediaStore,
                               @Nullable final onScanCompleteListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeLibrary(context);
                Library.resetAll();

                if (!attemptReload || !readLibrary(context, mergeWithMediaStore)) {
                    Library.setPlaylistLib(scanPlaylists(context));
                    Library.setSongLib(scanSongs(context));
                    Library.setArtistLib(scanArtists(context));
                    Library.setAlbumLib(scanAlbums(context));
                    Library.setGenreLib(scanGenres(context));
                }
                Library.sort();

                loaded = true;

                if (listener != null){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onScanComplete();
                        }
                    });
                }

                writeLibrary(context);
            }
        }).start();
    }

    public static boolean isLoaded (){
        return loaded;
    }

    public static interface onScanCompleteListener {
        public void onScanComplete();
    }

    // Scan the MediaStore for songs
    public static ArrayList<Song> scanSongs (Context context){
        ArrayList<Song> songs = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Song s = new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media._ID)),
                    (cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                            ? "Unknown Artist"
                            : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)));

            s.trackNumber = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.TRACK));
            songs.add(s);
        }
        cur.close();

        return songs;
    }

    // Scan the MediaStore for artists
    public static ArrayList<Artist> scanArtists (Context context){
        ArrayList<Artist> artists = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            if (!cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST)).equals(MediaStore.UNKNOWN_STRING)) {
                artists.add(new Artist(
                        cur.getLong(cur.getColumnIndex(MediaStore.Audio.Artists._ID)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST))));
            }
            else{
                artists.add(new Artist(
                        cur.getLong(cur.getColumnIndex(MediaStore.Audio.Artists._ID)),
                        "Unknown Artist"));
            }
        }
        cur.close();

        return artists;
    }

    // Scan the MediaStore for albums
    public static ArrayList<Album> scanAlbums (Context context){
        ArrayList<Album> albums = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC");
        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            albums.add(new Album(
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Albums._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
                    (cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                            ? "Unknown Artist"
                            : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.LAST_YEAR)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))));
        }
        cur.close();

        return albums;
    }

    // Scan the MediaStore for playlists
    public static ArrayList<Playlist> scanPlaylists (Context context){
        ArrayList<Playlist> playlists = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                null, null, null,
                MediaStore.Audio.Playlists.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            playlists.add(new Playlist(
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.NAME))));
        }
        cur.close();
        return playlists;
    }

    // Scan the MediaStore for Genres
    public static ArrayList<Genre> scanGenres (Context context){
        ArrayList<Genre> genres = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                null, null, null,
                MediaStore.Audio.Genres.NAME + " ASC");

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            long thisGenreId = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Genres._ID));

            if (cur.getString(cur.getColumnIndex(MediaStore.Audio.Genres.NAME)).equalsIgnoreCase("Unknown")){
                genres.add(new Genre(-1, "Unknown"));
            }
            else {
                genres.add(new Genre(
                        thisGenreId,
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Genres.NAME))));

                Cursor genreCur = context.getContentResolver().query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", thisGenreId),
                        new String[]{MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.IS_MUSIC + " != 0 ", null, null);
                genreCur.moveToFirst();

                for (int j = 0; j < genreCur.getCount(); j++) {
                    genreCur.moveToPosition(j);
                    findSongById(genreCur.getLong(genreCur.getColumnIndex(MediaStore.Audio.Media._ID))).genreId = thisGenreId;
                }
                genreCur.close();
            }
        }
        cur.close();

        return genres;
    }

    //
    //          LIBRARY SEARCH METHODS
    //

    public static Song findSongById (long songId){
        // Returns the first Artist object in the library with a matching id
        for (Song s : Library.getSongs()){
            if (s.songId == songId){
                return s;
            }
        }
        return null;
    }

    public static Artist findArtistById (long artistId){
        // Returns the first Artist object in the library with a matching id
        for (Artist a : Library.getArtists()){
            if (a.artistId == artistId){
                return a;
            }
        }
        return null;
    }

    public static Album findAlbumById (long albumId){
        // Returns the first Artist object in the library with a matching id
        for (Album a : Library.getAlbums()){
            if (a.albumId == albumId){
                return a;
            }
        }
        return null;
    }

    public static Genre findGenreById (long genreId){
        // Returns the first Genre object in the library with a matching id
        for (Genre g : Library.getGenres()){
            if (g.genreId == genreId){
                return g;
            }
        }
        return null;
    }

    //
    //          CONTENTS QUERY METHODS
    //

    // Return a list of song entries for a playlist
    public static ArrayList<Song> getPlaylistEntries (Context context, Playlist playlist){
        ArrayList<Song> songEntries = new ArrayList<>();

        Cursor cur = context.getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId),
                new String[]{
                        MediaStore.Audio.Playlists.Members.TITLE,
                        MediaStore.Audio.Playlists.Members.AUDIO_ID,
                        MediaStore.Audio.Playlists.Members.ARTIST,
                        MediaStore.Audio.Playlists.Members.ALBUM,
                        MediaStore.Audio.Playlists.Members.DURATION,
                        MediaStore.Audio.Playlists.Members.DATA,
                        MediaStore.Audio.Playlists.Members.ALBUM_ID,
                        MediaStore.Audio.Playlists.Members.ARTIST_ID},
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            songEntries.add(new Song(
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM)),
                    cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.DURATION)),
                    cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.DATA)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ALBUM_ID)),
                    cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.ARTIST_ID))));
        }
        cur.close();

        return songEntries;
    }

    // Return a list of song entries for an album
    public static ArrayList<Song> getAlbumEntries (Album album){
        ArrayList<Song> songEntries = new ArrayList<>();

        for (Song s : Library.getSongs()){
            if (s.albumId == album.albumId){
                songEntries.add(s);
            }
        }

        Collections.sort(songEntries, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return ((Long) o1.trackNumber).compareTo(o2.trackNumber);
            }
        });

        return songEntries;
    }

    // Return a list of song entries for an artist
    public static ArrayList<Song> getArtistSongEntries (Artist artist){
        ArrayList<Song> songEntries = new ArrayList<>();

        for(Song s : Library.getSongs()){
            if (s.artistId == artist.artistId){
                songEntries.add(s);
            }
        }

        return songEntries;
    }

    // Return a list of album entries for an artist
    public static ArrayList<Album> getArtistAlbumEntries (Artist artist){
        ArrayList<Album> albumEntries = new ArrayList<>();

        for (Album a : Library.getAlbums()){
            if (a.artistId == artist.artistId){
                albumEntries.add(a);
            }
        }

        return albumEntries;
    }

    // Return a list of song entries for a genre
    public static ArrayList<Song> getGenreEntries (Genre genre){
        ArrayList<Song> songEntries = new ArrayList<>();

        for(Song s : Library.getSongs()){
            if (s.genreId == genre.genreId){
                songEntries.add(s);
            }
        }

        return songEntries;
    }

    //
    //          PLAYLIST WRITING METHODS
    //

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

    public static void addPlaylistEntry(final Context context, final Playlist playlist, final Song song){
        // Public method to add a song to a playlist
        // Checks the playlist for duplicate entries
        if (getPlaylistEntries(context, playlist).contains(song)){
            new AlertDialog.Builder(context, Themes.getAlertTheme(context))
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

        Toast.makeText(
                context,
                String.format(context.getResources().getString(R.string.message_added_song), song, playlist),
                Toast.LENGTH_SHORT)
                .show();

    }

    public static void addPlaylistEntries(final Context context, final Playlist playlist, final ArrayList<Song> songs){
        // Public method to add songs to a playlist
        // Checks the playlist for duplicate entries

        int duplicateCount = 0;
        ArrayList<Song> currentEntries = getPlaylistEntries(context, playlist);
        final ArrayList<Song> newEntries = new ArrayList<>();

        for (Song s : songs){
            if (currentEntries.contains(s))duplicateCount++;
            else newEntries.add(s);
        }

        if (duplicateCount > 0){
            AlertDialog.Builder alert = new AlertDialog.Builder(context, Themes.getAlertTheme(context))
                    .setTitle("Add duplicate entries?");

            if (duplicateCount == songs.size()) {
                alert
                        .setMessage("This playlist already contains all of these songs. Adding them again will result in duplicates.")
                        .setPositiveButton("Add duplicates", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
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
                            }
                        })
                        .setNegativeButton("Add all", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                addSongsToEndOfPlaylist(context, playlist, songs);
                            }
                        })
                        .setNeutralButton("Cancel", null);
            }

            alert.show();
        }
        else{
            addSongsToEndOfPlaylist(context, playlist, songs);
        }
    }

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

        Toast.makeText(
                context,
                String.format(context.getResources().getQuantityString(R.plurals.message_added_songs, songs.size()), songs.size(), playlist),
                Toast.LENGTH_SHORT)
                .show();
    }

    public static void createPlaylist(final Context context, final String playlistName, final ArrayList<Song> songList){

        String trimmedName = playlistName.trim();

        if (trimmedName.length() == 0){
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.message_create_playlist_error_no_name),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        for (Playlist p : Library.getPlaylists()){
            if (p.playlistName.equalsIgnoreCase(trimmedName)){
                Toast.makeText(
                        context,
                        String.format(context.getResources().getString(R.string.message_create_playlist_error_exists), trimmedName),
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        }

        // Add the playlist to the MediaStore
        ContentValues mInserts = new ContentValues();
        mInserts.put(MediaStore.Audio.Playlists.NAME, trimmedName);
        mInserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        mInserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        Uri newPlaylistUri = context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts);

        // Update the playlist library & resort it
        Library.getPlaylists().clear();
        Library.setPlaylistLib(scanPlaylists(context));
        Library.sortPlaylistList(Library.getPlaylists());

        // Get the id of the new playlist
        Cursor cursor = context.getContentResolver().query(
                newPlaylistUri,
                new String[] {MediaStore.Audio.Playlists._ID},
                null, null, null);

        cursor.moveToFirst();
        Playlist playlist = new Playlist(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)), playlistName);
        cursor.close();

        // If we have a list of songs, associate it with the playlist
        if(songList != null) {
            ContentValues[] values = new ContentValues[songList.size()];

            for (int i = 0; i < songList.size(); i++) {
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, i);
                values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songList.get(i).songId);
            }

            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist.playlistId);
            ContentResolver resolver = context.getContentResolver();

            resolver.bulkInsert(uri, values);
            resolver.notifyChange(Uri.parse("content://media"), null);
        }

        Toast.makeText(
                context,
                String.format(context.getResources().getString(R.string.message_created_playlist), playlistName),
                Toast.LENGTH_SHORT)
                .show();
    }

    public static void removePlaylist(final Context context, final Playlist playlist){
        // Remove the playlist from the MediaStore
        context.getContentResolver().delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[] {playlist.playlistId + ""});

        // Update the playlist library & resort it
        Library.getPlaylists().clear();
        Library.setPlaylistLib(scanPlaylists(context));
        Library.sortPlaylistList(Library.getPlaylists());

        Toast.makeText(
                context,
                String.format(context.getResources().getString(R.string.message_removed_playlist), playlist),
                Toast.LENGTH_SHORT)
                .show();
    }

    //
    //          Media file open method
    //

    public static ArrayList<Song> getSongListFromFile(Context context, String path, String type) throws IOException{
        // A somewhat convoluted method for getting a list of songs from a path

        ArrayList<Song> queue = new ArrayList<>();

        if (Library.isEmpty()){
            scanAll(context, true, true);
        }

        // PLAYLISTS
        if (type.equals("audio/x-mpegurl") || type.equals("audio/x-scpls") || type.equals("application/vnd.ms-wpl")){
            // If a playlist was opened, try to find and play its entry from the MediaStore
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Playlists.DATA + "=?",
                    new String[] {path},
                    MediaStore.Audio.Playlists.NAME + " ASC");

            // If the media store contains this playlist, play it like a regular playlist
            if (cur.getCount() > 0){
                cur.moveToFirst();
                queue = getPlaylistEntries(context, new Playlist(
                                cur.getLong(cur.getColumnIndex(MediaStore.Audio.Playlists._ID)),
                                cur.getString(cur.getColumnIndex(MediaStore.Audio.Playlists.NAME))));
            }
            else{
                // If the MediaStore doesn't contain this playlist, attempt to read it manually
                Scanner sc = new Scanner(new File(path));
                ArrayList<String> lines = new ArrayList<>();
                while (sc.hasNextLine()) {
                    lines.add(sc.nextLine());
                }

                if (lines.size() > 0){
                    //TODO Attempt to read common playlist writing schemes
                }

            }
            cur.close();
        }
        // ALL OTHER TYPES OF MEDIA
        else {
            // If the file isn't a playlist, use a content resolver to find the song and play it
            Cursor cur = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Audio.Media.DATA + "=?",
                    new String[] {path},
                    MediaStore.Audio.Media.TITLE + " ASC");

            for (int i = 0; i < cur.getCount(); i++) {
                cur.moveToPosition(i);
                queue.add(new Song(
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                        cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media._ID)),
                        (cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals(MediaStore.UNKNOWN_STRING))
                                ? "Unknown Artist"
                                : cur.getString(cur.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                        cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                        cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID))));
            }
            cur.close();
        }

        if (queue.size() == 0) return null;
        else return queue;
    }

    //
    //          LIBRARY SAVING & READING METHODS
    //

    public static void saveLibrary (final Context context){
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeLibrary(context);
            }
        }).start();
    }

    private static void writeLibrary (Context context){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            // Save playlists
            FileWriter playlistWriter = new FileWriter(new File(context.getExternalFilesDir(null), FILENAME_PLAYLISTS));
            playlistWriter.write(gson.toJson(Library.getPlaylists(), ArrayList.class));
            playlistWriter.close();

            // Save songs
            FileWriter songWriter = new FileWriter(new File(context.getExternalFilesDir(null), FILENAME_SONGS));
            songWriter.write(gson.toJson(Library.getSongs(), ArrayList.class));
            songWriter.close();

            // Save artists
            FileWriter artistWriter = new FileWriter(new File(context.getExternalFilesDir(null), FILENAME_ARTISTS));
            artistWriter.write(gson.toJson(Library.getArtists(), ArrayList.class));
            artistWriter.close();

            // Save albums
            FileWriter albumWriter = new FileWriter(new File(context.getExternalFilesDir(null), FILENAME_ALBUMS));
            albumWriter.write(gson.toJson(Library.getAlbums(), ArrayList.class));
            albumWriter.close();

            // Save genres
            FileWriter genreWriter = new FileWriter(new File(context.getExternalFilesDir(null), FILENAME_GENRES));
            genreWriter.write(gson.toJson(Library.getGenres(), ArrayList.class));
            genreWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Try to reload the library if it was saved previously
    // Returns false if it couldn't be reloaded
    private static boolean readLibrary (Context context, boolean mergeWithMediaStore) {
        try {
            File playlistJSON = new File(context.getExternalFilesDir(null), FILENAME_PLAYLISTS);
            File songJSON = new File(context.getExternalFilesDir(null), FILENAME_SONGS);
            File artistJSON = new File(context.getExternalFilesDir(null), FILENAME_ARTISTS);
            File albumJSON = new File(context.getExternalFilesDir(null), FILENAME_ALBUMS);
            File genreJSON = new File(context.getExternalFilesDir(null), FILENAME_GENRES);

            if (!playlistJSON.exists() || !songJSON.exists() || !artistJSON.exists()
                    || !albumJSON.exists() || !genreJSON.exists()) {
                return false;
            }

            FileInputStream playlistIn = new FileInputStream(playlistJSON);
            String playlistGSON = convertStreamToString(playlistIn);

            FileInputStream songIn = new FileInputStream(songJSON);
            String songGSON = convertStreamToString(songIn);

            FileInputStream artistIn = new FileInputStream(artistJSON);
            String artistGSON = convertStreamToString(artistIn);

            FileInputStream albumIn = new FileInputStream(albumJSON);
            String albumGSON = convertStreamToString(albumIn);

            FileInputStream genreIn = new FileInputStream(genreJSON);
            String genreGSON = convertStreamToString(genreIn);

            Gson gson = new Gson();

            ArrayList<Playlist> playlistLibrary = gson.fromJson(playlistGSON, new TypeToken<List<Playlist>>() {}.getType());
            ArrayList<Song> songLibrary = gson.fromJson(songGSON, new TypeToken<List<Song>>() {}.getType());
            ArrayList<Artist> artistLibrary = gson.fromJson(artistGSON, new TypeToken<List<Artist>>() {}.getType());
            ArrayList<Album> albumLibrary = gson.fromJson(albumGSON, new TypeToken<List<Album>>() {}.getType());
            ArrayList<Genre> genreLibrary = gson.fromJson(genreGSON, new TypeToken<List<Genre>>() {}.getType());

            if (playlistLibrary != null && songLibrary != null && artistLibrary != null && albumLibrary != null && genreLibrary != null) {
                if (mergeWithMediaStore) {
                    // Scan the MediaStore and find differences between the device's library and the saved library
                    // If an entry in the MediaStore isn't in the saved library, add it
                    // If an entry in the saved library isn't in the MediaStore, remove it

                    ArrayList<Playlist> mediaStorePlaylists = scanPlaylists(context);
                    for (Playlist p : playlistLibrary){
                        if (!mediaStorePlaylists.contains(p)) playlistLibrary.remove(p);
                    }
                    for (Playlist p : mediaStorePlaylists){
                        if (!playlistLibrary.contains(p)) playlistLibrary.add(p);
                    }
                    Library.setPlaylistLib(playlistLibrary);

                    ArrayList<Song> mediaStoreSongs = scanSongs(context);
                    for (Song s : songLibrary){
                        if (!mediaStoreSongs.contains(s)) songLibrary.remove(s);
                    }
                    for (Song s : mediaStoreSongs){
                        if (!songLibrary.contains(s)) songLibrary.add(s);
                    }
                    Library.setSongLib(songLibrary);

                    ArrayList<Artist> mediaStoreArtists = scanArtists(context);
                    for (Artist a : artistLibrary){
                        if (!mediaStoreArtists.contains(a)) artistLibrary.remove(a);
                    }
                    for (Artist a : mediaStoreArtists){
                        if (!artistLibrary.contains(a)) artistLibrary.add(a);
                    }
                    Library.setArtistLib(artistLibrary);

                    ArrayList<Album> mediaStoreAlbums = scanAlbums(context);
                    for (Album a : albumLibrary){
                        if (!mediaStoreAlbums.contains(a)) albumLibrary.remove(a);
                    }
                    for (Album a : mediaStoreAlbums){
                        if (!albumLibrary.contains(a)) albumLibrary.add(a);
                    }
                    Library.setAlbumLib(albumLibrary);

                    ArrayList<Genre> mediaStoreGenres = scanGenres(context);
                    for (Genre g : genreLibrary){
                        if (!mediaStoreGenres.contains(g)) genreLibrary.remove(g);
                    }
                    for (Genre g : mediaStoreGenres){
                        if (!genreLibrary.contains(g)) genreLibrary.add(g);
                    }
                    Library.setGenreLib(genreLibrary);
                }
                else {
                    Library.setPlaylistLib(playlistLibrary);
                    Library.setSongLib(songLibrary);
                    Library.setArtistLib(artistLibrary);
                    Library.setAlbumLib(albumLibrary);
                    Library.setGenreLib(genreLibrary);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}