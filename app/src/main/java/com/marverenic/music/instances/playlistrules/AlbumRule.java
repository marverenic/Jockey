package com.marverenic.music.instances.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

public class AlbumRule extends AutoPlaylistRule implements Parcelable {

    protected AlbumRule(@Field int field, @Match int match, String value) {
        super(ALBUM, field, match, value);
    }

    protected AlbumRule(Parcel in) {
        super(in);
    }

    @Override
    public Observable<List<Song>> applyFilter(PlaylistStore playlistStore, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return musicStore.getAlbums()
                .observeOn(Schedulers.computation())
                .take(1)
                .map(library -> {
                    List<Album> filtered = new ArrayList<>();
                    for (Album album : library) {
                        if (includeAlbum(album)) {
                            filtered.add(album);
                        }
                    }

                    return filtered;
                })
                .map(albums -> {
                    Set<Long> albumIds = new HashSet<>();

                    for (Album album : albums) {
                        albumIds.add(album.getAlbumId());
                    }

                    return albumIds;
                })
                .zipWith(musicStore.getSongs(), (albumIds, songs) -> {
                    List<Song> filtered = new ArrayList<>();

                    for (Song song : songs) {
                        if (albumIds.contains(song.getAlbumId())) {
                            filtered.add(song);
                        }
                    }

                    return filtered;
                });
    }

    @SuppressLint("SwitchIntDef")
    private boolean includeAlbum(Album album) {
        switch (getField()) {
            case AutoPlaylistRule.ID:
                return checkId(album.getAlbumId());
            case AutoPlaylistRule.NAME:
                return checkString(album.getAlbumName());
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

    public static final Creator<AlbumRule> CREATOR = new Creator<AlbumRule>() {
        @Override
        public AlbumRule createFromParcel(Parcel in) {
            return new AlbumRule(in);
        }

        @Override
        public AlbumRule[] newArray(int size) {
            return new AlbumRule[size];
        }
    };
}
