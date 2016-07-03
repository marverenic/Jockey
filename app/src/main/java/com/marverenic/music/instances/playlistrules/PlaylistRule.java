package com.marverenic.music.instances.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class PlaylistRule extends AutoPlaylistRule implements Parcelable {

    protected PlaylistRule(@Field int field, @Match int match, String value) {
        super(PLAYLIST, field, match, value);
    }

    protected PlaylistRule(Parcel in) {
        super(in);
    }

    @Override
    public Observable<List<Song>> applyFilter(PlaylistStore playlistStore, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return playlistStore.getPlaylists()
                .map(library -> {
                    List<Playlist> filtered = new ArrayList<>();
                    for (Playlist playlist : library) {
                        if (includePlaylist(playlist)) {
                            filtered.add(playlist);
                        }
                    }

                    return filtered;
                })
                .flatMap(Observable::from)
                .concatMap(playlistStore::getSongs);
    }

    @SuppressLint("SwitchIntDef")
    private boolean includePlaylist(Playlist playlist) {
        switch (getField()) {
            case ID:
                return checkId(playlist.getPlaylistId());
            case NAME:
                return checkString(playlist.getPlaylistName());
        }
        throw new IllegalArgumentException("Cannot compare against field " + getField());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PlaylistRule> CREATOR = new Creator<PlaylistRule>() {
        @Override
        public PlaylistRule createFromParcel(Parcel in) {
            return new PlaylistRule(in);
        }

        @Override
        public PlaylistRule[] newArray(int size) {
            return new PlaylistRule[size];
        }
    };
}
