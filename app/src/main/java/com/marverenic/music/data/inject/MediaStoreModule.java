package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.LocalMusicStore;
import com.marverenic.music.data.store.MusicStore;

import javax.inject.Singleton;

import dagger.Module;

@Module
public class MediaStoreModule {

    @Singleton
    public MusicStore provideMusicStore(Context context) {
        return new LocalMusicStore(context);
    }

}
