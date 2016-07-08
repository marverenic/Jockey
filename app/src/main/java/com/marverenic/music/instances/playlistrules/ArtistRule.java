package com.marverenic.music.instances.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class ArtistRule extends AutoPlaylistRule implements Parcelable {

    protected ArtistRule(@Field int field, @Match int match, String value) {
        super(ARTIST, field, match, value);
    }

    protected ArtistRule(Parcel in) {
        super(in);
    }

    @Override
    public Observable<List<Song>> applyFilter(PlaylistStore playlistStore, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return musicStore.getArtists()
                .take(1)
                .map(library -> {
                    List<Artist> filtered = new ArrayList<>();
                    for (Artist artist : library) {
                        if (includeArtist(artist)) {
                            filtered.add(artist);
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
    private boolean includeArtist(Artist artist) {
        switch (getField()) {
            case AutoPlaylistRule.ID:
                return checkId(artist.getArtistId());
            case AutoPlaylistRule.NAME:
                return checkString(artist.getArtistName());
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

    public static final Creator<ArtistRule> CREATOR = new Creator<ArtistRule>() {
        @Override
        public ArtistRule createFromParcel(Parcel in) {
            return new ArtistRule(in);
        }

        @Override
        public ArtistRule[] newArray(int size) {
            return new ArtistRule[size];
        }
    };
}
