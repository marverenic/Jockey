package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.LocalMusicStore;
import com.marverenic.music.data.store.LocalPlayCountStore;
import com.marverenic.music.data.store.LocalPlaylistStore;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MediaStoreModule {

    @Provides
    @Singleton
    public MusicStore provideMusicStore(Context context, PreferenceStore preferenceStore) {
        return new LocalMusicStore(context, preferenceStore);
    }

    @Provides
    @Singleton
    public PlaylistStore providePlaylistStore(Context context, MusicStore musicStore,
                                              PlayCountStore playCountStore) {
        return new LocalPlaylistStore(context, musicStore, playCountStore);
    }

    @Provides
    @Singleton
    public PlayCountStore providePlayCountStore(Context context) {
        return new LocalPlayCountStore(context);
    }
}
