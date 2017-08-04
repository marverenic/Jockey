package com.marverenic.music.data.store;

import android.content.Context;
import android.provider.MediaStore;

import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class LocalMusicStore implements MusicStore {

    private Context mContext;
    private PreferenceStore mPreferenceStore;

    private BehaviorSubject<Boolean> mSongLoadingState;
    private BehaviorSubject<Boolean> mArtistLoadingState;
    private BehaviorSubject<Boolean> mAlbumLoadingState;
    private BehaviorSubject<Boolean> mGenreLoadingState;

    private BehaviorSubject<List<Song>> mSongs;
    private BehaviorSubject<List<Album>> mAlbums;
    private BehaviorSubject<List<Artist>> mArtists;
    private BehaviorSubject<List<Genre>> mGenres;

    public LocalMusicStore(Context context, PreferenceStore preferenceStore) {
        mContext = context;
        mPreferenceStore = preferenceStore;

        mSongLoadingState = BehaviorSubject.create(false);
        mAlbumLoadingState = BehaviorSubject.create(false);
        mArtistLoadingState = BehaviorSubject.create(false);
        mGenreLoadingState = BehaviorSubject.create(false);

        MediaStoreUtil.waitForPermission()
                .subscribe(permission -> bindRefreshListener(), throwable -> {
                    Timber.e(throwable, "Failed to bind refresh listener");
                });
    }

    private void bindRefreshListener() {
        MediaStoreUtil.getContentObserver(mContext, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .subscribe(selfChange -> refreshSongs(), throwable -> {
                    Timber.e(throwable, "Failed to automatically refresh songs");
                });

        MediaStoreUtil.getContentObserver(mContext, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI)
                .subscribe(selfChange -> refreshArtists(), throwable -> {
                    Timber.e(throwable, "Failed to automatically refresh artists");
                });

        MediaStoreUtil.getContentObserver(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI)
                .subscribe(selfChange -> refreshAlbums(), throwable -> {
                    Timber.e(throwable, "Failed to automatically refresh albums");
                });

        MediaStoreUtil.getContentObserver(mContext, MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI)
                .subscribe(selfChange -> refreshGenres(), throwable -> {
                    Timber.e(throwable, "Failed to automatically refresh genres");
                });
    }

    private void refreshSongs() {
        mSongLoadingState.onNext(true);

        MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .subscribe(granted -> {
                    if (granted && mSongs != null) {
                        mSongs.onNext(getAllSongs());
                    }
                    mSongLoadingState.onNext(false);
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh songs");
                });
    }

    private void refreshArtists() {
        mArtistLoadingState.onNext(true);

        MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .subscribe(granted -> {
                    if (granted && mArtists != null) {
                        mArtists.onNext(getAllArtists());
                    }
                    mArtistLoadingState.onNext(false);
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh artists");
                });
    }

    private void refreshAlbums() {
        mAlbumLoadingState.onNext(true);

        MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .subscribe(granted -> {
                    if (granted && mAlbums != null) {
                        mAlbums.onNext(getAllAlbums());
                    }
                    mAlbumLoadingState.onNext(false);
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh albums");
                });
    }

    private void refreshGenres() {
        mGenreLoadingState.onNext(true);

        MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .subscribe(granted -> {
                    if (granted && mGenres != null) {
                        mGenres.onNext(getAllGenres());
                    }
                    mGenreLoadingState.onNext(false);
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh genres");
                });
    }

    @Override
    public void loadAll() {
        getSongs().take(1).subscribe();
        getArtists().take(1).subscribe();
        getAlbums().take(1).subscribe();
        getGenres().take(1).subscribe();
    }

    @Override
    public Observable<Boolean> refresh() {
        mSongLoadingState.onNext(true);
        mArtistLoadingState.onNext(true);
        mAlbumLoadingState.onNext(true);
        mGenreLoadingState.onNext(true);

        BehaviorSubject<Boolean> result = BehaviorSubject.create();

        MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .map(granted -> {
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
                    mSongLoadingState.onNext(false);
                    mArtistLoadingState.onNext(false);
                    mAlbumLoadingState.onNext(false);
                    mGenreLoadingState.onNext(false);
                    return granted;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result);

        return result.asObservable();
    }

    @Override
    public Observable<Boolean> isLoading() {
        return Observable.combineLatest(mSongLoadingState, mArtistLoadingState, mAlbumLoadingState,
                mGenreLoadingState, (songState, artistState, albumState, genreState) -> {
                    return songState || artistState || albumState || genreState;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            mSongs = BehaviorSubject.create();
            mSongLoadingState.onNext(true);

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mSongs.onNext(getAllSongs());
                        } else {
                            mSongs.onNext(Collections.emptyList());
                        }
                        mSongLoadingState.onNext(false);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to query MediaStore for songs");
                    });
        }
        return mSongs.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Song> getAllSongs() {
        return MediaStoreUtil.getSongs(mContext, getDirectoryInclusionExclusionSelection(), null);
    }

    private String getDirectoryInclusionExclusionSelection() {
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
            return null;
        }

        return "(" + selection + ")";
    }

    private String getDirectoryInclusionSelection() {
        if (mPreferenceStore.getIncludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferenceStore.getIncludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" LIKE \'")
                    .append(directory).append(File.separatorChar)
                    .append("%\'");

            builder.append(" OR ");
        }

        builder.setLength(builder.length() - 4);
        return builder.toString();
    }

    private String getDirectoryExclusionSelection() {
        if (mPreferenceStore.getExcludedDirectories().isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (String directory : mPreferenceStore.getExcludedDirectories()) {
            builder.append(MediaStore.Audio.Media.DATA)
                    .append(" NOT LIKE \'")
                    .append(directory).append(File.separatorChar)
                    .append("%\'");

            builder.append(" AND ");
        }

        builder.setLength(builder.length() - 5);
        return builder.toString();
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            mAlbums = BehaviorSubject.create();
            mAlbumLoadingState.onNext(true);

            MediaStoreUtil.getPermission(mContext)
                    .flatMap(granted -> {
                        if (noDirectoryFilters()) {
                            return Observable.just(granted);
                        } else {
                            return getSongs().map((List<Song> songs) -> granted);
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mAlbums.onNext(getAllAlbums());
                        } else {
                            mAlbums.onNext(Collections.emptyList());
                        }
                        mAlbumLoadingState.onNext(false);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to query MediaStore for albums");
                    });
        }
        return mAlbums.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Album> getAllAlbums() {
        return filterAlbums(MediaStoreUtil.getAlbums(mContext, null, null));
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            mArtists = BehaviorSubject.create();
            mArtistLoadingState.onNext(true);

            MediaStoreUtil.getPermission(mContext)
                    .flatMap(granted -> {
                        if (noDirectoryFilters()) {
                            return Observable.just(granted);
                        } else {
                            return getSongs().map((List<Song> songs) -> granted);
                        }
                    })
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mArtists.onNext(getAllArtists());
                        } else {
                            mArtists.onNext(Collections.emptyList());
                        }
                        mArtistLoadingState.onNext(false);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to query MediaStore for artists");
                    });
        }
        return mArtists.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Artist> getAllArtists() {
        return filterArtists(MediaStoreUtil.getArtists(mContext, null, null));
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        if (mGenres == null) {
            mGenres = BehaviorSubject.create();
            mGenreLoadingState.onNext(true);

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mGenres.onNext(getAllGenres());
                        } else {
                            mGenres.onNext(Collections.emptyList());
                        }
                        mGenreLoadingState.onNext(false);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to query MediaStore for genres");
                    });
        }
        return mGenres.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Genre> getAllGenres() {
        return filterGenres(MediaStoreUtil.getGenres(mContext, null, null));
    }

    private boolean noDirectoryFilters() {
        boolean notIncludingFolders = mPreferenceStore.getIncludedDirectories().isEmpty();
        boolean notExcludingFolders = mPreferenceStore.getExcludedDirectories().isEmpty();

        return notExcludingFolders && notIncludingFolders;
    }

    private List<Album> filterAlbums(List<Album> albumsToFilter) {
        if (noDirectoryFilters()) {
            return albumsToFilter;
        }

        List<Album> filteredAlbums = new ArrayList<>();

        for (Album album : albumsToFilter) {
            for (Song song : mSongs.getValue()) {
                if (album.getAlbumId() == song.getAlbumId()) {
                    filteredAlbums.add(album);
                    break;
                }
            }
        }

        return filteredAlbums;
    }

    private List<Artist> filterArtists(List<Artist> artistsToFilter) {
        if (noDirectoryFilters()) {
            return artistsToFilter;
        }

        List<Artist> filteredArtists = new ArrayList<>();

        for (Artist artist : artistsToFilter) {
            for (Song song : mSongs.getValue()) {
                if (artist.getArtistId() == song.getArtistId()) {
                    filteredArtists.add(artist);
                    break;
                }
            }
        }

        return filteredArtists;
    }

    private List<Genre> filterGenres(List<Genre> genresToFilter) {
        if (noDirectoryFilters()) {
            return genresToFilter;
        }

        List<Genre> filteredGenres = new ArrayList<>();
        String directorySelection = getDirectoryInclusionExclusionSelection();

        for (Genre genre : genresToFilter) {
            boolean hasSongs = !MediaStoreUtil.getGenreSongs(mContext, genre,
                    directorySelection, null).isEmpty();

            if (hasSongs) {
                filteredGenres.add(genre);
            }
        }

        return filteredGenres;
    }

    @Override
    public Observable<List<Song>> getSongs(Artist artist) {
        String selection = MediaStore.Audio.Media.ARTIST_ID + " = ?";
        String[] selectionArgs = {Long.toString(artist.getArtistId())};

        String directorySelection = getDirectoryInclusionExclusionSelection();
        if (directorySelection != null) {
            selection += " AND " + directorySelection;
        }

        return Observable.just(MediaStoreUtil.getSongs(mContext, selection, selectionArgs));
    }

    @Override
    public Observable<List<Song>> getSongs(Album album) {
        String selection = MediaStore.Audio.Media.ALBUM_ID + " = ? ";
        String[] selectionArgs = {Long.toString(album.getAlbumId())};

        String directorySelection = getDirectoryInclusionExclusionSelection();
        if (directorySelection != null) {
            selection += " AND " + directorySelection;
        }

        return Observable.just(MediaStoreUtil.getSongs(mContext, selection, selectionArgs));
    }

    @Override
    public Observable<List<Song>> getSongs(Genre genre) {
        return Observable.just(MediaStoreUtil.getGenreSongs(mContext, genre,
                getDirectoryInclusionExclusionSelection(), null));
    }

    @Override
    public Observable<List<Album>> getAlbums(Artist artist) {
        return getAlbums()
                .flatMap(allAlbums -> {
                    return Observable.from(allAlbums)
                            .filter(album -> album.getArtistName().equals(artist.getArtistName()))
                            .toList()
                            .map(albums -> {
                                if (allAlbumsHaveYears(albums)) {
                                    Collections.sort(albums, Album.YEAR_COMPARATOR);
                                } else {
                                    Collections.sort(albums);
                                }
                                return albums;
                            });
                });
    }

    private boolean allAlbumsHaveYears(List<Album> albums) {
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).getYear() == 0) {
                return false;
            }
        }
        return true;
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
        if (query == null || query.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return getSongs().map(songs -> {
            List<Song> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Song song : songs) {
                if (song.getSongName().toLowerCase().contains(lowerCaseQuery)
                        || song.getAlbumName().toLowerCase().contains(lowerCaseQuery)
                        || song.getArtistName().toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(song);
                }
            }

            return filtered;
        });
    }

    @Override
    public Observable<List<Artist>> searchForArtists(String query) {
        if (query == null || query.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return getArtists().map(artists -> {
            List<Artist> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Artist artist : artists) {
                if (artist.getArtistName().toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(artist);
                }
            }

            return filtered;
        });
    }

    @Override
    public Observable<List<Album>> searchForAlbums(String query) {
        if (query == null || query.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return getAlbums().map(albums -> {
            List<Album> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Album album : albums) {
                if (album.getAlbumName().toLowerCase().contains(lowerCaseQuery)
                        || album.getArtistName().toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(album);
                }
            }

            return filtered;
        });
    }

    @Override
    public Observable<List<Genre>> searchForGenres(String query) {
        if (query == null || query.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return getGenres().map(genres -> {
            List<Genre> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Genre genre : genres) {
                if (genre.getGenreName().toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(genre);
                }
            }

            return filtered;
        });
    }
}
