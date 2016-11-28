package com.marverenic.music.model.playlistrules;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.schedulers.Schedulers;

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
                .observeOn(Schedulers.computation())
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
                .map(artists -> {
                    Set<Long> artistIds = new HashSet<>();

                    for (Artist artist : artists) {
                        artistIds.add((long) artist.getArtistId());
                    }

                    return artistIds;
                })
                .zipWith(musicStore.getSongs(), (artistIds, songs) -> {
                    List<Song> filtered = new ArrayList<>();

                    for (Song song : songs) {
                        if (artistIds.contains(song.getArtistId())) {
                            filtered.add(song);
                        }
                    }

                    return filtered;
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
