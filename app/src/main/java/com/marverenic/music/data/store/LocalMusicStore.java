package com.marverenic.music.data.store;

import android.content.Context;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public class LocalMusicStore implements MusicStore {

    private Context mContext;

    public LocalMusicStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<List<Song>> getSongs() {
        return null;
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        return null;
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        return null;
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        return null;
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
        return null;
    }

    @Override
    public Observable<List<Song>> getSongs(Artist artist) {
        return null;
    }

    @Override
    public Observable<List<Song>> getSongs(Album album) {
        return null;
    }

    @Override
    public Observable<List<Song>> getSongs(Genre genre) {
        return null;
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        return null;
    }

    @Override
    public Observable<List<Album>> getAlbums(Artist artist) {
        return null;
    }

    @Override
    public void makePlaylist(String name) {

    }

    @Override
    public void makeAutoPlaylist(AutoPlaylist playlist) {

    }

    @Override
    public void removePlaylist(Playlist playlist) {

    }

    @Override
    public void editPlaylist(Playlist playlist, List<Song> newSongs) {

    }

    @Override
    public void addToPlaylist(Playlist playlist, Song song) {

    }

    @Override
    public void addToPlaylist(Playlist playlist, List<Song> songs) {

    }
}
