package com.marverenic.music.lastfm.data.store;

import com.marverenic.music.lastfm.model.LfmArtist;

import rx.Observable;

public interface LastFmStore {

    Observable<LfmArtist> getArtistInfo(String artistName);

}
