package com.marverenic.music.data.inject;

import com.marverenic.music.player.MockPlayerController;
import com.marverenic.music.player.PlayerController;

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

}
