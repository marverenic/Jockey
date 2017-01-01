package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.DemoMusicStore;
import com.marverenic.music.data.store.DemoPlaylistStore;
import com.marverenic.music.data.store.LocalPlayCountStore;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DemoModule {

    @Provides
    @Singleton
    public MusicStore provideMusicStore(Context context) {
        return new DemoMusicStore(context);
    }

    @Provides
    @Singleton
    public PlaylistStore providePlaylistStore(Context context) {
        return new DemoPlaylistStore(context);
    }

    @Provides
    @Singleton
    public PlayCountStore providePlayCountStore(Context context) {
        return new LocalPlayCountStore(context);
    }

}
