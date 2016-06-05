package com.marverenic.music.lastfm2.data.inject;

import com.marverenic.music.lastfm2.api.LastFmApi;
import com.marverenic.music.lastfm2.api.LastFmService;
import com.marverenic.music.lastfm2.data.store.LastFmStore;
import com.marverenic.music.lastfm2.data.store.NetworkLastFmStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LastFmModule {

    @Provides
    @Singleton
    public LastFmService provideLastFmService() {
        return LastFmApi.getService();
    }

    @Provides
    @Singleton
    public LastFmStore provideLastFmStore(LastFmService service) {
        return new NetworkLastFmStore(service);
    }

}
