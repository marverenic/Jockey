package com.marverenic.music.lastfm2.data.store;

import com.marverenic.music.instances.Artist;

import rx.Observable;

public interface LastFmStore {

    Observable<Artist> getArtistInfo(String artistName);

}
