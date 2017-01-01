package com.marverenic.music.data.store;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class DemoPlaylistStore implements PlaylistStore {

    private static final String PLAYLISTS_FILENAME = "library-playlists.json";

    private Context mContext;
    private Observable<List<Playlist>> mPlaylists;

    public DemoPlaylistStore(Context context) {
        mContext = context;
    }

    @Override
    public void loadPlaylists() {
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
    public Observable<List<Playlist>> getPlaylists() {
        if (mPlaylists == null) {
            BehaviorSubject<List<Playlist>> subject = BehaviorSubject.create();
            Observable.fromCallable(this::readPlaylists)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subject);

            mPlaylists = subject;
        }

        return mPlaylists;
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    private List<Playlist> readPlaylists() throws IOException {
        InputStream stream = null;
        InputStreamReader reader = null;

        try {
            File json = new File(mContext.getExternalFilesDir(null), PLAYLISTS_FILENAME);
            stream = new FileInputStream(json);
            reader = new InputStreamReader(stream);

            return new Gson().fromJson(reader, new TypeToken<List<Playlist>>(){}.getType());
        } finally {
            if (stream != null) stream.close();
            if (reader != null) reader.close();
        }
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        throw new UnsupportedOperationException("Cannot get playlist contents in a demo library");
    }

    @Override
    public Observable<List<Playlist>> searchForPlaylists(String query) {
        return getPlaylists().map(
                playlists -> {
                    List<Playlist> filtered = new ArrayList<>();

                    for (Playlist playlist : playlists) {
                        if (playlist.getPlaylistName().contains(query)) {
                            filtered.add(playlist);
                        }
                    }

                    return filtered;
                });
    }

    @Override
    public String verifyPlaylistName(String playlistName) {
        // Never return any errors
        return null;
    }

    @Override
    public Playlist makePlaylist(String name) {
        throw new UnsupportedOperationException("Cannot create playlists in a demo library");
    }

    @Override
    public AutoPlaylist makePlaylist(AutoPlaylist model) {
        throw new UnsupportedOperationException("Cannot create playlists in a demo library");
    }

    @Override
    public Playlist makePlaylist(String name, @Nullable List<Song> songs) {
        throw new UnsupportedOperationException("Cannot create playlists in a demo library");
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        throw new UnsupportedOperationException("Cannot delete fake playlists");
    }

    @Override
    public void editPlaylist(Playlist playlist, List<Song> newSongs) {
        throw new UnsupportedOperationException("Cannot edit fake playlists");
    }

    @Override
    public void editPlaylist(AutoPlaylist replacementModel) {
        throw new UnsupportedOperationException("Cannot edit fake playlists");
    }

    @Override
    public void addToPlaylist(Playlist playlist, Song song) {
        throw new UnsupportedOperationException("Cannot edit fake playlists");
    }

    @Override
    public void addToPlaylist(Playlist playlist, List<Song> songs) {
        throw new UnsupportedOperationException("Cannot edit fake playlists");
    }
}
