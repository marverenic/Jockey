package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.player.ServicePlayerController;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PlayerModule {

    @Provides
    @Singleton
    public PlayerController providePlayerController(Context context, PreferenceStore prefs) {
        return new ServicePlayerController(context, prefs);
    }

}
