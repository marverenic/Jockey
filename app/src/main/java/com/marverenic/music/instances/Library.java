package com.marverenic.music.instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class Library {
    private static ArrayList<Song> songLib = new ArrayList<>();
    private static ArrayList<Album> albumLib = new ArrayList<>();
    private static ArrayList<Artist> artistLib = new ArrayList<>();
    private static ArrayList<Playlist> playlistLib = new ArrayList<>();
    private static ArrayList<Genre> genreLib = new ArrayList<>();

    //
    //      Add methods
    //
    // Add instances to the library array

    public static void add(Song s) {
        songLib.add(s);
    }

    public static void add(Album a) {
        albumLib.add(a);
    }

    public static void add(Artist a) {
        artistLib.add(a);
    }

    public static void add(Playlist p) {
        playlistLib.add(p);
    }

    public static void add(Genre g) {
        genreLib.add(g);
    }

    //
    //      Sorting methods
    //
    // Pass an array of instance objects and a sorted one will be returned
    // In general they're sorted alphabetically by name
    // For songs and albums, "a" and "the" are ignored when sorting

    public static ArrayList<Song> sortSongList(final ArrayList<Song> songs){
        ArrayList<Song> sortedSongs = new ArrayList<>(songs);
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
                if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                    return o2c.compareTo(o1c);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(sortedSongs, songComparator);
        return sortedSongs;
    }

    public static ArrayList<Album> sortAlbumList (final ArrayList<Album> albums){
        ArrayList<Album> sortedAlbums = new ArrayList<>(albums);
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
                if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                    return o2c.compareTo(o1c);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(sortedAlbums, albumComparator);
        return sortedAlbums;
    }

    public static ArrayList<Artist> sortArtistList (final ArrayList<Artist> artists){
        ArrayList<Artist> sortedArtists = new ArrayList<>(artists);
        Comparator<Artist> artistComparator = new Comparator<Artist>() {
            @Override
            public int compare(Artist o1, Artist o2) {
                String o1c = o1.artistName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.artistName.toLowerCase(Locale.ENGLISH);
                if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                    return o2c.compareTo(o1c);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(sortedArtists, artistComparator);
        return sortedArtists;
    }

    public static ArrayList<Playlist> sortPlaylistList (final ArrayList<Playlist> playlists){
        ArrayList<Playlist> sortedPlaylists = new ArrayList<>(playlists);
        Comparator<Playlist> playlistComparator = new Comparator<Playlist>() {
            @Override
            public int compare(Playlist o1, Playlist o2) {
                String o1c = o1.playlistName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.playlistName.toLowerCase(Locale.ENGLISH);
                if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                    return o2c.compareTo(o1c);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(sortedPlaylists, playlistComparator);
        return sortedPlaylists;
    }

    public static ArrayList<Genre> sortGenreList (final ArrayList<Genre> genres){
        ArrayList<Genre> sortedArtists = new ArrayList<>(genres);
        Comparator<Genre> genreComparator = new Comparator<Genre>() {
            @Override
            public int compare(Genre o1, Genre o2) {
                String o1c = o1.genreName.toLowerCase(Locale.ENGLISH);
                String o2c = o2.genreName.toLowerCase(Locale.ENGLISH);
                if (!o1c.matches("[a-z]") && o2c.matches("[a-z]")) {
                    return o2c.compareTo(o1c);
                }
                return o1c.compareTo(o2c);
            }
        };
        Collections.sort(sortedArtists, genreComparator);
        return sortedArtists;
    }

    public static void sort (){
        songLib = sortSongList(songLib);
        albumLib = sortAlbumList(albumLib);
        artistLib = sortArtistList(artistLib);
        playlistLib = sortPlaylistList(playlistLib);
        genreLib = sortGenreList(genreLib);
    }

    //
    //      Get methods for libraries
    //
    public static ArrayList<Song> getSongs() {
        return songLib;
    }

    public static ArrayList<Album> getAlbums() {
        return albumLib;
    }

    public static ArrayList<Artist> getArtists() {
        return artistLib;
    }

    public static ArrayList<Playlist> getPlaylists() {
        return playlistLib;
    }

    public static ArrayList<Genre> getGenres() {
        return genreLib;
    }

    //
    //      Set methods for libraries
    //
    public static void setSongLib(ArrayList<Song> songLib) {
        Library.songLib = songLib;
    }

    public static void setAlbumLib(ArrayList<Album> albumLib) {
        Library.albumLib = albumLib;
    }

    public static void setArtistLib(ArrayList<Artist> artistLib) {
        Library.artistLib = artistLib;
    }

    public static void setPlaylistLib(ArrayList<Playlist> playlistLib) {
        Library.playlistLib = playlistLib;
    }

    public static void setGenreLib(ArrayList<Genre> genreLib) {
        Library.genreLib = genreLib;
    }

    // Clears all the libraries
    public static void resetAll() {
        songLib = new ArrayList<>();
        albumLib = new ArrayList<>();
        artistLib = new ArrayList<>();
        playlistLib = new ArrayList<>();
        genreLib = new ArrayList<>();
    }

    // Returns true if the libraries have no entries or aren't initialized
    public static boolean isEmpty() {
        return (songLib == null || albumLib == null || artistLib == null || playlistLib == null || genreLib == null)
               || (songLib.size() == 0 && albumLib.size() == 0 && artistLib.size() == 0 && playlistLib.size() == 0 && genreLib.size() == 0);
    }
}
