package com.marverenic.music.lastfm2.data.store;

import com.marverenic.music.instances.Artist;
import com.marverenic.music.lastfm2.api.LastFmService;

import rx.Observable;

public class NetworkLastFmStore implements LastFmStore {

    private LastFmService mService;

    public NetworkLastFmStore(LastFmService service) {
        mService = service;
    }

    @Override
    public Observable<Artist> getArtistInfo(String artistName) {
        return null;
    }
}
