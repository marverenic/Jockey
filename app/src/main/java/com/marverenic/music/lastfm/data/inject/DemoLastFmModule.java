package com.marverenic.music.lastfm.data.inject;

import android.content.Context;

import com.marverenic.music.lastfm.data.store.DemoLastFmStore;
import com.marverenic.music.lastfm.data.store.LastFmStore;
import com.marverenic.music.lastfm.data.store.NetworkLastFmStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DemoLastFmModule {

    @Provides
    @Singleton
    public LastFmStore provideLastFmStore(Context context) {
        return new DemoLastFmStore(context);
    }

}
