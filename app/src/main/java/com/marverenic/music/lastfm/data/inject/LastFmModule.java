package com.marverenic.music.lastfm.data.inject;

import android.content.Context;

import com.marverenic.music.lastfm.api.LastFmApi;
import com.marverenic.music.lastfm.api.LastFmService;
import com.marverenic.music.lastfm.data.store.LastFmStore;
import com.marverenic.music.lastfm.data.store.NetworkLastFmStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class LastFmModule {

    @Provides
    @Singleton
    public LastFmService provideLastFmService(Context context) {
        return LastFmApi.getService(context);
    }

    @Provides
    @Singleton
    public LastFmStore provideLastFmStore(LastFmService service) {
        return new NetworkLastFmStore(service);
    }

}
