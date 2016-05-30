package com.marverenic.music.data.store;

import android.content.Context;

import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public class LocalPlaylistStore implements PlaylistStore {

    private Context mContext;

    public LocalPlaylistStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<List<Playlist>> getPlaylists() {
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
