package com.marverenic.music.model.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.schedulers.Schedulers;

public class GenreRule extends AutoPlaylistRule implements Parcelable {

    protected GenreRule(@Field int field, @Match int match, String value) {
        super(GENRE, field, match, value);
    }

    protected GenreRule(Parcel in) {
        super(in);
    }

    @Override
    public Observable<List<Song>> applyFilter(PlaylistStore playlistStore, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return musicStore.getGenres()
                .observeOn(Schedulers.computation())
                .take(1)
                .map(library -> {
                    List<Genre> filtered = new ArrayList<>();
                    for (Genre genre : library) {
                        if (includeGenre(genre)) {
                            filtered.add(genre);
                        }
                    }

                    return filtered;
                })
                .flatMap(Observable::from)
                .concatMap(musicStore::getSongs)
                .reduce((songs, songs2) -> {
                    List<Song> merged = new ArrayList<>(songs);
                    merged.addAll(songs2);
                    return merged;
                });
    }

    @SuppressLint("SwitchIntDef")
    private boolean includeGenre(Genre genre) {
        switch (getField()) {
            case ID:
                return checkId(genre.getGenreId());
            case NAME:
                return checkString(genre.getGenreName());
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

    public static final Creator<GenreRule> CREATOR = new Creator<GenreRule>() {
        @Override
        public GenreRule createFromParcel(Parcel in) {
            return new GenreRule(in);
        }

        @Override
        public GenreRule[] newArray(int size) {
            return new GenreRule[size];
        }
    };
}
