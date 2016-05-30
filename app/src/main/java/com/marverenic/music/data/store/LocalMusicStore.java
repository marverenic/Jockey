package com.marverenic.music.data.store;

import android.content.Context;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public class LocalMusicStore implements MusicStore {

    private Context mContext;
    private List<Song> mSongs;
    private List<Album> mAlbums;
    private List<Artist> mArtists;

    public LocalMusicStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            mSongs = MediaStoreUtil.getAllSongs(mContext);
        }
        return Observable.just(mSongs);
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            mAlbums = MediaStoreUtil.getAllAlbums(mContext);
        }
        return Observable.just(mAlbums);
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            mArtists = MediaStoreUtil.getAllArtists(mContext);
        }
        return Observable.just(mArtists);
    }

    @Override
    public Observable<List<Genre>> getGenres() {
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
}
