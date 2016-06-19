package com.marverenic.music.data.store;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public interface MusicStore {

    Observable<Boolean> refresh();

    Observable<List<Song>> getSongs();

    Observable<List<Album>> getAlbums();

    Observable<List<Artist>> getArtists();

    Observable<List<Genre>> getGenres();

    Observable<List<Song>> getSongs(Artist artist);

    Observable<List<Song>> getSongs(Album album);

    Observable<List<Song>> getSongs(Genre genre);

    Observable<List<Album>> getAlbums(Artist artist);

    Observable<Artist> findArtistById(long artistId);

    Observable<Album> findAlbumById(long albumId);

    Observable<Artist> findArtistByName(String artistName);

    Observable<List<Song>> searchForSongs(String query);

    Observable<List<Artist>> searchForArtists(String query);

    Observable<List<Album>> searchForAlbums(String query);

    Observable<List<Genre>> searchForGenres(String query);

}
