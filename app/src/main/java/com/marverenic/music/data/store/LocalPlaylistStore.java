package com.marverenic.music.data.store;

import android.content.Context;
import android.support.annotation.Nullable;

import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.Collections;
import java.util.List;

import rx.Observable;

public class LocalPlaylistStore implements PlaylistStore {

    private Context mContext;
    private List<Playlist> mPlaylists;

    public LocalPlaylistStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
        if (mPlaylists == null) {
            return MediaStoreUtil.getPermission(mContext).map(granted -> {
                if (granted) {
                    mPlaylists = MediaStoreUtil.getAllPlaylists(mContext);
                } else {
                    mPlaylists = Collections.emptyList();
                }
                return mPlaylists;
            });
        }
        return Observable.just(mPlaylists);
    }

    @Override
    public Observable<List<Song>> getSongs(Playlist playlist) {
        return Observable.just(MediaStoreUtil.getPlaylistSongs(mContext, playlist));
    }

    @Override
    public void makePlaylist(String name) {
        makePlaylist(name, null);
    }

    @Override
    public void makePlaylist(String name, @Nullable List<Song> songs) {
        MediaStoreUtil.createPlaylist(mContext, name, songs);
    }

    @Override
    public void removePlaylist(Playlist playlist) {
        MediaStoreUtil.deletePlaylist(mContext, playlist);
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
