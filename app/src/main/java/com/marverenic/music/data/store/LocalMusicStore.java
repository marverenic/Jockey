package com.marverenic.music.data.store;

import android.content.Context;
import android.provider.MediaStore;

import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Song;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class LocalMusicStore implements MusicStore {

    private Context mContext;
    private PreferencesStore mPreferencesStore;

    private BehaviorSubject<List<Song>> mSongs;
    private BehaviorSubject<List<Album>> mAlbums;
    private BehaviorSubject<List<Artist>> mArtists;
    private BehaviorSubject<List<Genre>> mGenres;

    public LocalMusicStore(Context context, PreferencesStore preferencesStore) {
        mContext = context;
        mPreferencesStore = preferencesStore;
    }

    @Override
    public Observable<Boolean> refresh() {
        return MediaStoreUtil.promptPermission(mContext).map(
                granted -> {
                    if (granted) {
                        if (mSongs != null) {
                            mSongs.onNext(getAllSongs());
                        }
                        if (mArtists != null) {
                            mArtists.onNext(getAllArtists());
                        }
                        if (mAlbums != null) {
                            mAlbums.onNext(getAllAlbums());
                        }
                        if (mGenres != null) {
                            mGenres.onNext(getAllGenres());
                        }
                    }
                    return granted;
                });
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            mSongs = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext).subscribe(granted -> {
                if (granted) {
                    mSongs.onNext(getAllSongs());
                } else {
                    mSongs.onNext(Collections.emptyList());
                }
            });
        }
        return mSongs;
    }

    private List<Song> getAllSongs() {
        String selection;

        String includeSelection = getDirectoryInclusionSelection();
        String excludeSelection = getDirectoryExclusionSelection();

        if (includeSelection != null && excludeSelection != null) {
            selection = "(" + includeSelection + ") AND (" + excludeSelection + ")";
        } else if (includeSelection != null) {
            selection = includeSelection;
        } else if (excludeSelection != null) {
            selection = excludeSelection;
        } else {
            selection = null;
        }

        return MediaStoreUtil.getSongs(mContext, selection, null);
    }

    private String getDirectoryInclusionSelection() {
        if (mPreferencesStore.getIncludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferencesStore.getIncludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" LIKE \'%").append(directory).append("%\'");

            builder.append(" OR ");
        }

        builder.setLength(builder.length() - 4);
        return builder.toString();
    }

    private String getDirectoryExclusionSelection() {
        if (mPreferencesStore.getExcludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferencesStore.getExcludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" NOT LIKE \'%").append(directory).append("%\'");

            builder.append(" AND ");
        }

        builder.setLength(builder.length() - 5);
        return builder.toString();
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            mAlbums = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext).subscribe(granted -> {
                if (granted) {
                    mAlbums.onNext(getAllAlbums());
                } else {
                    mAlbums.onNext(Collections.emptyList());
                }
            });
        }
        return mAlbums;
    }

    private List<Album> getAllAlbums() {
        return MediaStoreUtil.getAlbums(mContext, null, null);
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            mArtists = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext).subscribe(granted -> {
                if (granted) {
                    mArtists.onNext(getAllArtists());
                } else {
                    mArtists.onNext(Collections.emptyList());
                }
            });
        }
        return mArtists;
    }

    private List<Artist> getAllArtists() {
        return MediaStoreUtil.getArtists(mContext, null, null);
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        if (mGenres == null) {
            mGenres = BehaviorSubject.create();

            MediaStoreUtil.getPermission(mContext).subscribe(granted -> {
                if (granted) {
                    mGenres.onNext(getAllGenres());
                } else {
                    mGenres.onNext(Collections.emptyList());
                }
            });
        }
        return mGenres;
    }

    private List<Genre> getAllGenres() {
        return MediaStoreUtil.getGenres(mContext, null, null);
    }

    @Override
    public Observable<List<Song>> getSongs(Artist artist) {
        return Observable.just(MediaStoreUtil.getArtistSongs(mContext, artist));
    }

    @Override
    public Observable<List<Song>> getSongs(Album album) {
        return Observable.just(MediaStoreUtil.getAlbumSongs(mContext, album));
    }

    @Override
    public Observable<List<Song>> getSongs(Genre genre) {
        return Observable.just(MediaStoreUtil.getGenreSongs(mContext, genre));
    }

    @Override
    public Observable<List<Album>> getAlbums(Artist artist) {
        return Observable.just(MediaStoreUtil.getArtistAlbums(mContext, artist));
    }

    @Override
    public Observable<Artist> findArtistById(long artistId) {
        return Observable.just(MediaStoreUtil.findArtistById(mContext, artistId));
    }

    @Override
    public Observable<Album> findAlbumById(long albumId) {
        return Observable.just(MediaStoreUtil.findAlbumById(mContext, albumId));
    }

    @Override
    public Observable<Artist> findArtistByName(String artistName) {
        return Observable.just(MediaStoreUtil.findArtistByName(mContext, artistName));
    }

    @Override
    public Observable<List<Song>> searchForSongs(String query) {
        return Observable.just(MediaStoreUtil.searchForSongs(mContext, query));
    }

    @Override
    public Observable<List<Artist>> searchForArtists(String query) {
        return Observable.just(MediaStoreUtil.searchForArtists(mContext, query));
    }

    @Override
    public Observable<List<Album>> searchForAlbums(String query) {
        return Observable.just(MediaStoreUtil.searchForAlbums(mContext, query));
    }

    @Override
    public Observable<List<Genre>> searchForGenres(String query) {
        return Observable.just(MediaStoreUtil.searchForGenres(mContext, query));
    }
}
