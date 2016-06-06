package com.marverenic.music.data.store;

import android.support.annotation.Nullable;

import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.List;

import rx.Observable;

public interface PlaylistStore {

    Observable<List<Playlist>> getPlaylists();

    Observable<List<Song>> getSongs(Playlist playlist);

    Observable<List<Playlist>> searchForPlaylists(String query);

    String verifyPlaylistName(String playlistName);

    Playlist makePlaylist(String name);

    Playlist makePlaylist(String name, @Nullable List<Song> songs);

    void removePlaylist(Playlist playlist);

    void editPlaylist(Playlist playlist, List<Song> newSongs);

    void addToPlaylist(Playlist playlist, Song song);

    void addToPlaylist(Playlist playlist, List<Song> songs);

}
