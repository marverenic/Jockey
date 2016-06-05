package com.marverenic.music.lastfm2.data.store;

import com.marverenic.music.lastfm2.model.LfmArtist;

import rx.Observable;

public interface LastFmStore {

    Observable<LfmArtist> getArtistInfo(String artistName);

}
