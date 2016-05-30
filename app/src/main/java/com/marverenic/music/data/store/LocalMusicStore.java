package com.marverenic.music.data.store;

import android.content.Context;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.Collections;
import java.util.List;

import rx.Observable;

public class LocalMusicStore implements MusicStore {

    private Context mContext;
    private boolean mAlreadyRequestedPermission;

    private List<Song> mSongs;
    private List<Album> mAlbums;
    private List<Artist> mArtists;
    private List<Genre> mGenres;

    public LocalMusicStore(Context context) {
        mContext = context;
        mAlreadyRequestedPermission = false;
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            return MediaStoreUtil.hasPermission(mContext).map(granted -> {
                if (granted) {
                    mSongs = MediaStoreUtil.getAllSongs(mContext);
                } else {
                    mSongs = Collections.emptyList();
                }
                return mSongs;
            });
        }
        return Observable.just(mSongs);
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            return MediaStoreUtil.hasPermission(mContext).map(granted -> {
                if (granted) {
                    mAlbums = MediaStoreUtil.getAllAlbums(mContext);
                } else {
                    mAlbums = Collections.emptyList();
                }
                return mAlbums;
            });
        }
        return Observable.just(mAlbums);
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            return MediaStoreUtil.hasPermission(mContext).map(granted -> {
                if (granted) {
                    mArtists = MediaStoreUtil.getAllArtists(mContext);
                } else {
                    mArtists = Collections.emptyList();
                }
                return mArtists;
            });
        }
        return Observable.just(mArtists);
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        if (mGenres == null) {
            return MediaStoreUtil.hasPermission(mContext).map(granted -> {
                if (granted) {
                    mGenres = MediaStoreUtil.getAllGenres(mContext);
                } else {
                    mGenres = Collections.emptyList();
                }
                return mGenres;
            });
        }
        return Observable.just(mGenres);
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
