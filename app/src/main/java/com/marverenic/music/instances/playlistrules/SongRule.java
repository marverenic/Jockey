package com.marverenic.music.instances.playlistrules;

import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

public class SongRule extends AutoPlaylistRule implements Parcelable {

    protected SongRule(@Field int field, @Match int match, String value) {
        super(AutoPlaylistRule.SONG, field, match, value);
    }

    protected SongRule(Parcel in) {
        super(in);
    }

    @Override
    public Observable<List<Song>> applyFilter(PlaylistStore playlistStore, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return musicStore.getSongs()
                .observeOn(Schedulers.computation())
                .take(1)
                .map(library -> {
                    List<Song> filtered = new ArrayList<>();
                    for (Song song : library) {
                        if (includeSong(song, playCountStore)) {
                            filtered.add(song);
                        }
                    }
                    return filtered;
                });
    }

    private boolean includeSong(Song song, PlayCountStore playCountStore) {
        switch (getField()) {
            case ID:
                return checkId(song.getSongId());
            case NAME:
                return checkString(song.getSongName());
            case PLAY_COUNT:
                return checkInt(playCountStore.getPlayCount(song));
            case SKIP_COUNT:
                return checkInt(playCountStore.getPlayCount(song));
            case YEAR:
                return checkInt(song.getYear());
            case DATE_ADDED:
                return checkInt(song.getDateAdded());
            case DATE_PLAYED:
                return checkInt(playCountStore.getPlayDate(song));
        }
        throw new IllegalArgumentException("Cannot compare against field " + getField());
    }

    public static final Creator<SongRule> CREATOR = new Creator<SongRule>() {
        @Override
        public SongRule createFromParcel(Parcel in) {
            return new SongRule(in);
        }

        @Override
        public SongRule[] newArray(int size) {
            return new SongRule[size];
        }
    };
}
