package com.marverenic.music.data.store;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.marverenic.music.R;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class LocalPlaylistStore implements PlaylistStore {

    private static final String AUTO_PLAYLIST_EXTENSION = ".jpl";

    // Used to generate Auto Playlist contents
    private MusicStore mMusicStore;
    private PlayCountStore mPlayCountStore;

    private Context mContext;
    private BehaviorSubject<List<Playlist>> mPlaylists;
    private Map<Playlist, BehaviorSubject<List<Song>>> mPlaylistContents;

    private BehaviorSubject<Boolean> mLoadingState;

    public LocalPlaylistStore(Context context, MusicStore musicStore,
                              PlayCountStore playCountStore) {
        mContext = context;
        mMusicStore = musicStore;
        mPlayCountStore = playCountStore;
        mPlaylistContents = new ArrayMap<>();
        mLoadingState = BehaviorSubject.create(false);
    }

    @Override
    public void loadPlaylists() {
        getPlaylists().take(1).subscribe();
    }

    @Override
    public Observable<Boolean> refresh() {
        if (mPlaylists == null) {
            return Observable.just(true);
        }

        mLoadingState.onNext(true);

        return MediaStoreUtil.promptPermission(mContext)
                .observeOn(Schedulers.io())
                .map(granted -> {
                    if (granted && mPlaylists != null) {
                        mPlaylists.onNext(getAllPlaylists());
                        mPlaylistContents.clear();
                    }
                    mLoadingState.onNext(false);
                    return granted;
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Boolean> isLoading() {
        return mLoadingState.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
        if (mPlaylists == null) {
            mPlaylists = BehaviorSubject.create();
            mLoadingState.onNext(true);

            MediaStoreUtil.getPermission(mContext)
                    .observeOn(Schedulers.io())
                    .subscribe(granted -> {
                        if (granted) {
                            mPlaylists.onNext(getAllPlaylists());
                        } else {
                            mPlaylists.onNext(Collections.emptyList());
                        }
                        mLoadingState.onNext(false);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to query MediaStore for playlists");
                    });
        }
        return mPlaylists.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private List<Playlist> getAllPlaylists() {
        return MediaStoreUtil.getAllPlaylists(mContext);
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        if (playlist instanceof AutoPlaylist) {
            return getAutoPlaylistSongs((AutoPlaylist) playlist);
        } else {
            return getPlaylistSongs(playlist);
        }
    }

    private Observable<List<Song>> getPlaylistSongs(Playlist playlist) {
        BehaviorSubject<List<Song>> subject;

        if (mPlaylistContents.containsKey(playlist)) {
            subject = mPlaylistContents.get(playlist);
        } else {
            subject = BehaviorSubject.create(MediaStoreUtil.getPlaylistSongs(mContext, playlist));
            mPlaylistContents.put(playlist, subject);
        }

        return subject.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<List<Song>> getAutoPlaylistSongs(AutoPlaylist playlist) {
        BehaviorSubject<List<Song>> subject;

        if (mPlaylistContents.containsKey(playlist)) {
            subject = mPlaylistContents.get(playlist);
        } else {
            subject = BehaviorSubject.create();
            mPlaylistContents.put(playlist, subject);

            playlist.generatePlaylist(mMusicStore, this, mPlayCountStore)
                    .subscribe(subject::onNext, subject::onError);

            subject.observeOn(Schedulers.io())
                    .subscribe(contents -> {
                        editPlaylist(playlist, contents);
                    }, throwable -> {
                        Timber.e(throwable, "Failed to save playlist contents");
                    });
        }

        return subject.asObservable().observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Playlist>> searchForPlaylists(String query) {
        if (query == null || query.isEmpty()) {
            return Observable.just(Collections.emptyList());
        }

        return getPlaylists().map(playlists -> {
            List<Playlist> filtered = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Playlist playlist : playlists) {
                if (playlist.getPlaylistName().toLowerCase().contains(lowerCaseQuery)) {
                    filtered.add(playlist);
                }
            }

            return filtered;
        });
    }

    @Override
    public String verifyPlaylistName(String playlistName) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            return mContext.getString(R.string.error_hint_empty_playlist);
        }

        if (MediaStoreUtil.findPlaylistByName(mContext, playlistName) != null) {
            return mContext.getString(R.string.error_hint_duplicate_playlist);
        }

        return null;
    }

    @Override
    public Playlist makePlaylist(String name) {
        return makePlaylist(name, null);
    }

    @Override
    public AutoPlaylist makePlaylist(AutoPlaylist playlist) {
        Playlist localReference = MediaStoreUtil.createPlaylist(mContext,
                playlist.getPlaylistName(), Collections.emptyList());

        AutoPlaylist created = new AutoPlaylist.Builder(playlist)
                .setId(localReference.getPlaylistId())
                .build();

        saveAutoPlaylistConfiguration(created);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updatedPlaylists = new ArrayList<>(mPlaylists.getValue());
            updatedPlaylists.add(created);
            Collections.sort(updatedPlaylists);

            mPlaylists.onNext(updatedPlaylists);
        }

        return created;
    }

    @Override
    public Playlist makePlaylist(String name, @Nullable List<Song> songs) {
        Playlist created = MediaStoreUtil.createPlaylist(mContext, name, songs);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.add(created);
            Collections.sort(updated);

            mPlaylists.onNext(updated);
        }

        return created;
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        MediaStoreUtil.deletePlaylist(mContext, playlist);
        mPlaylistContents.remove(playlist);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.remove(playlist);

            mPlaylists.onNext(updated);
        }
    }

    @Override
    public void editPlaylist(Playlist playlist, List<Song> newSongs) {
        MediaStoreUtil.editPlaylist(mContext, playlist, newSongs);
        if (mPlaylistContents.containsKey(playlist)) {
            mPlaylistContents.get(playlist).onNext(new ArrayList<>(newSongs));
        }
    }

    @Override
    public void editPlaylist(AutoPlaylist replacement) {
        saveAutoPlaylistConfiguration(replacement);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updatedPlaylists = new ArrayList<>(mPlaylists.getValue());

            int index = updatedPlaylists.indexOf(replacement);
            updatedPlaylists.set(index, replacement);

            mPlaylists.onNext(updatedPlaylists);
        }
    }

    private void saveAutoPlaylistConfiguration(AutoPlaylist playlist) {
        // Write an initial set of values to the MediaStore so other apps can see this playlist
        playlist.generatePlaylist(mMusicStore, this, mPlayCountStore)
                .take(1)
                .observeOn(Schedulers.io())
                .subscribe(contents -> {
                    editPlaylist(playlist, contents);

                    // Cache this result in memory
                    BehaviorSubject<List<Song>> contentsSubject;
                    if (mPlaylistContents.containsKey(playlist)) {
                        contentsSubject = mPlaylistContents.get(playlist);
                    } else {
                        contentsSubject = BehaviorSubject.create();
                        mPlaylistContents.put(playlist, contentsSubject);
                    }
                    contentsSubject.onNext(contents);

                    try {
                        writeAutoPlaylistConfiguration(playlist);
                    } catch (IOException e) {
                        Timber.e(e, "Failed to write autoPlaylist configuration");
                    }
                }, throwable -> {
                    Timber.e(throwable, "makePlaylist: Failed to initialize contents");
                });
    }

    private void writeAutoPlaylistConfiguration(AutoPlaylist playlist) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(AutoPlaylistRule.class, new AutoPlaylistRule.RuleTypeAdapter())
                .create();
        FileWriter writer = null;

        try {
            String filename = playlist.getPlaylistName() + AUTO_PLAYLIST_EXTENSION;
            String fullPath = mContext.getExternalFilesDir(null) + File.separator + filename;

            writer = new FileWriter(fullPath);
            writer.write(gson.toJson(playlist, AutoPlaylist.class));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public void addToPlaylist(Playlist playlist, Song song) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, song);

        if (mPlaylistContents.containsKey(playlist)) {
            BehaviorSubject<List<Song>> observableContents = mPlaylistContents.get(playlist);
            List<Song> updatedContents = new ArrayList<>(observableContents.getValue());
            updatedContents.add(song);
            observableContents.onNext(updatedContents);
        }
    }

    @Override
    public void addToPlaylist(Playlist playlist, List<Song> songs) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, songs);

        if (mPlaylistContents.containsKey(playlist)) {
            BehaviorSubject<List<Song>> observableContents = mPlaylistContents.get(playlist);
            List<Song> updatedContents = new ArrayList<>(observableContents.getValue());
            updatedContents.addAll(songs);
            observableContents.onNext(updatedContents);
        }
    }
}
