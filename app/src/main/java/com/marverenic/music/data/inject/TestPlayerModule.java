package com.marverenic.music.data.inject;

import com.marverenic.music.player.MockPlayerController;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.player.persistence.PlaybackPersistenceManager;
import com.marverenic.music.player.persistence.PlaybackStateDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class TestPlayerModule {

    @Provides
    @Singleton
    public PlayerController providePlayerController() {
        return new MockPlayerController();
    }

    @Provides
    @Singleton
    public PlaybackStateDatabase providePlaybackStateDatabase() {
        throw new UnsupportedOperationException();
    }

    @Provides
    @Singleton
    public PlaybackPersistenceManager providePlaybackPersistenceManager() {
        throw new UnsupportedOperationException();
    }

}
