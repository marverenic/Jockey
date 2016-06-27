package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.LocalMusicStore;
import com.marverenic.music.data.store.LocalPlayCountStore;
import com.marverenic.music.data.store.LocalPlaylistStore;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferencesStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MediaStoreModule {

    @Provides
    @Singleton
    public MusicStore provideMusicStore(Context context, PreferencesStore preferencesStore) {
        return new LocalMusicStore(context, preferencesStore);
    }

    @Provides
    @Singleton
    public PlaylistStore providePlaylistStore(Context context) {
        return new LocalPlaylistStore(context);
    }

    @Provides
    @Singleton
    public PlayCountStore providePlayCountStore(Context context) {
        return new LocalPlayCountStore(context);
    }
}
