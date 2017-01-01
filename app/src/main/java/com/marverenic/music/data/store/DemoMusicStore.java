package com.marverenic.music.data.store;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class DemoMusicStore implements MusicStore {

    private static final String SONGS_FILENAME = "library-songs.json";
    private static final String ARTISTS_FILENAME = "library-artists.json";
    private static final String ALBUMS_FILENAME = "library-albums.json";
    private static final String GENRES_FILENAME = "library-genres.json";

    private Context mContext;

    private Observable<List<Song>> mSongs;
    private Observable<List<Album>> mAlbums;
    private Observable<List<Artist>> mArtists;
    private Observable<List<Genre>> mGenres;

    public DemoMusicStore(Context context) {
        mContext = context;
    }

    @Override
    public void loadAll() {
        // Do nothing. Performance isn't important in the demo app, so just load lazily.
    }

    @Override
    public Observable<Boolean> refresh() {
        return Observable.just(true);
    }

    @Override
    public Observable<Boolean> isLoading() {
        return Observable.just(false);
    }

    @Override
    public Observable<List<Song>> getSongs() {
        if (mSongs == null) {
            BehaviorSubject<List<Song>> subject = BehaviorSubject.create();

            Observable.fromCallable(() -> this.<Song>parseJson(SONGS_FILENAME, Song[].class))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subject::onNext, subject::onError);

            mSongs = subject.asObservable();
        }

        return mSongs;
    }

    @Override
    public Observable<List<Album>> getAlbums() {
        if (mAlbums == null) {
            BehaviorSubject<List<Album>> subject = BehaviorSubject.create();

            Observable.fromCallable(() -> this.<Album>parseJson(ALBUMS_FILENAME, Album[].class))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subject::onNext, subject::onError);

            mAlbums = subject.asObservable();
        }

        return mAlbums;
    }

    @Override
    public Observable<List<Artist>> getArtists() {
        if (mArtists == null) {
            BehaviorSubject<List<Artist>> subject = BehaviorSubject.create();

            Observable.fromCallable(() -> this.<Artist>parseJson(ARTISTS_FILENAME, Artist[].class))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subject::onNext, subject::onError);

            mArtists = subject.asObservable();
        }

        return mArtists;
    }

    @Override
    public Observable<List<Genre>> getGenres() {
        if (mGenres == null) {
            BehaviorSubject<List<Genre>> subject = BehaviorSubject.create();

            Observable.fromCallable(() -> this.<Genre>parseJson(GENRES_FILENAME, Genre[].class))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subject::onNext, subject::onError);

            mGenres = subject.asObservable();
        }

        return mGenres;
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    private <T> List<T> parseJson(String filename, Class<? extends T[]> type) throws IOException {
        InputStream stream = null;
        InputStreamReader reader = null;

        try {
            File json = new File(mContext.getExternalFilesDir(null), filename);
            stream = new FileInputStream(json);
            reader = new InputStreamReader(stream);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class,
                            (JsonDeserializer) (src, srcType, c) -> Uri.parse(src.getAsString()))
                    .create();

            T[] values = gson.fromJson(reader, type);
            return new ArrayList<>(Arrays.asList(values));
        } finally {
            if (stream != null) stream.close();
            if (reader != null) reader.close();
        }
    }

    @Override
    public Observable<List<Song>> getSongs(Artist artist) {
        return getSongs().map(
                library -> {
                    List<Song> filtered = new ArrayList<>();

                    for (Song song : library) {
                        if (song.getArtistId() == artist.getArtistId()) {
                            filtered.add(song);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<List<Song>> getSongs(Album album) {
        return getSongs().map(
                library -> {
                    List<Song> filtered = new ArrayList<>();

                    for (Song song : library) {
                        if (song.getAlbumId() == album.getAlbumId()) {
                            filtered.add(song);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<List<Song>> getSongs(Genre genre) {
        throw new UnsupportedOperationException("Genre contents are not supported in demo library");
    }

    @Override
    public Observable<List<Album>> getAlbums(Artist artist) {
        return getAlbums().map(
                library -> {
                    List<Album> filtered = new ArrayList<>();

                    for (Album album : library) {
                        if (album.getArtistId() == artist.getArtistId()) {
                            filtered.add(album);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<Artist> findArtistById(long artistId) {
        return getArtists().flatMap(
                library -> {
                    for (Artist artist : library) {
                        if (artist.getArtistId() == artistId) {
                            return Observable.just(artist);
                        }
                    }
                    return Observable.just(null);
                });
    }

    @Override
    public Observable<Album> findAlbumById(long albumId) {
        return getAlbums().flatMap(
                library -> {
                    for (Album album : library) {
                        if (album.getAlbumId() == albumId) {
                            return Observable.just(album);
                        }
                    }
                    return Observable.just(null);
                });
    }

    @Override
    public Observable<Artist> findArtistByName(String artistName) {
        return getArtists().flatMap(
                library -> {
                    for (Artist artist : library) {
                        if (artist.getArtistName().equals(artistName)) {
                            return Observable.just(artist);
                        }
                    }
                    return Observable.just(null);
                });
    }

    @Override
    public Observable<List<Song>> searchForSongs(String query) {
        return getSongs().map(
                library -> {
                    List<Song> filtered = new ArrayList<>();

                    for (Song song : library) {
                        if (song.getSongName().contains(query)) {
                            filtered.add(song);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<List<Artist>> searchForArtists(String query) {
        return getArtists().map(
                library -> {
                    List<Artist> filtered = new ArrayList<>();

                    for (Artist artist : library) {
                        if (artist.getArtistName().contains(query)) {
                            filtered.add(artist);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<List<Album>> searchForAlbums(String query) {
        return getAlbums().map(
                library -> {
                    List<Album> filtered = new ArrayList<>();

                    for (Album album : library) {
                        if (album.getArtistName().contains(query)) {
                            filtered.add(album);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public Observable<List<Genre>> searchForGenres(String query) {
        return getGenres().map(
                library -> {
                    List<Genre> filtered = new ArrayList<>();

                    for (Genre genre : library) {
                        if (genre.getGenreName().contains(query)) {
                            filtered.add(genre);
                        }
                    }

                    return filtered;
                });
    }
}
