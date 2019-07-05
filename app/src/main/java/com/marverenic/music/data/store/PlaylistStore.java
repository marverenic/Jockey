package com.marverenic.music.data.store;

import androidx.annotation.Nullable;

import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Observable;

public interface PlaylistStore {

    void loadPlaylists();

    Observable<Boolean> refresh();

    Observable<Boolean> isLoading();

    Observable<List<Playlist>> getPlaylists();

    Observable<List<Song>> getSongs(Playlist playlist);

    Observable<List<Playlist>> searchForPlaylists(String query);

    String verifyPlaylistName(String playlistName);

    AutoPlaylist makePlaylist(AutoPlaylist model);

    Playlist makePlaylist(String name, @Nullable List<Song> songs);

    void removePlaylist(Playlist playlist);

    void editPlaylist(Playlist playlist, List<Song> newSongs);

    void editPlaylist(AutoPlaylist replacementModel);

    void addToPlaylist(Playlist playlist, Song song);

    void addToPlaylist(Playlist playlist, List<Song> songs);

}
