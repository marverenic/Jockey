package com.marverenic.music.data.store;

import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;

import java.io.File;
import java.util.List;

import rx.Observable;

public interface MusicStore {

    void loadAll();

    Observable<Boolean> refresh();

    Observable<Boolean> isLoading();

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

    Observable<List<Song>> getSongsFromFiles(List<File> files);

}
