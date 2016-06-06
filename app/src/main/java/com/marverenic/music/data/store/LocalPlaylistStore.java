package com.marverenic.music.data.store;

import android.content.Context;
import android.support.annotation.Nullable;

import com.jakewharton.rxrelay.BehaviorRelay;
import com.marverenic.music.R;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;

public class LocalPlaylistStore implements PlaylistStore {

    private Context mContext;
    private BehaviorRelay<List<Playlist>> mPlaylists;

    public LocalPlaylistStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
        if (mPlaylists == null) {
            mPlaylists = BehaviorRelay.create();

            MediaStoreUtil.getPermission(mContext).map(granted -> {
                if (granted) {
                    return MediaStoreUtil.getAllPlaylists(mContext);
                } else {
                    return Collections.<Playlist>emptyList();
                }
            }).subscribe(mPlaylists);
        }
        return mPlaylists;
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        return Observable.just(MediaStoreUtil.getPlaylistSongs(mContext, playlist));
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
    public Playlist makePlaylist(String name, @Nullable List<Song> songs) {
        Playlist created = MediaStoreUtil.createPlaylist(mContext, name, songs);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.add(created);
            Collections.sort(updated);

            mPlaylists.call(updated);
        }

        return created;
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        MediaStoreUtil.deletePlaylist(mContext, playlist);

        if (mPlaylists != null && mPlaylists.getValue() != null) {
            List<Playlist> updated = new ArrayList<>(mPlaylists.getValue());
            updated.remove(playlist);

            mPlaylists.call(updated);
        }
    }

    @Override
    public void editPlaylist(Playlist playlist, List<Song> newSongs) {
        MediaStoreUtil.editPlaylist(mContext, playlist, newSongs);
    }

    @Override
    public void addToPlaylist(Playlist playlist, Song song) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, song);
    }

    @Override
    public void addToPlaylist(Playlist playlist, List<Song> songs) {
        MediaStoreUtil.appendToPlaylist(mContext, playlist, songs);
    }
}
