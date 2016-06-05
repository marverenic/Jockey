package com.marverenic.music.data.store;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public interface MusicStore {

    Observable<List<Song>> getSongs();

    Observable<List<Album>> getAlbums();

    Observable<List<Artist>> getArtists();

    Observable<List<Genre>> getGenres();

    Observable<List<Playlist>> getPlaylists();

    Observable<List<Song>> getSongs(Artist artist);

    Observable<List<Song>> getSongs(Album album);

    Observable<List<Song>> getSongs(Genre genre);

    Observable<List<Song>> getSongs(Playlist playlist);

    Observable<List<Album>> getAlbums(Artist artist);

    void makePlaylist(String name);

    void makeAutoPlaylist(AutoPlaylist playlist);

    void removePlaylist(Playlist playlist);

    void editPlaylist(Playlist playlist, List<Song> newSongs);

    void addToPlaylist(Playlist playlist, Song song);

    void addToPlaylist(Playlist playlist, List<Song> songs);

}
