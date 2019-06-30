package com.marverenic.music.data.inject;

import androidx.room.Room;
import android.content.Context;

import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.player.ServicePlayerController;
import com.marverenic.music.player.persistence.PlaybackPersistenceManager;
import com.marverenic.music.player.persistence.PlaybackStateDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PlayerModule {

    @Provides
    @Singleton
    public PlayerController providePlayerController(Context context, PreferenceStore prefs,
                                                    PlaybackPersistenceManager persistenceManager) {
        return new ServicePlayerController(context, prefs, persistenceManager);
    }

    @Provides
    @Singleton
    public PlaybackStateDatabase providePlaybackStateDatabase(Context context) {
        return Room.databaseBuilder(context, PlaybackStateDatabase.class, "playerState")
                .allowMainThreadQueries()
                .build();
    }

    @Provides
    @Singleton
    public PlaybackPersistenceManager providePlaybackPersistenceManager(
            PlaybackStateDatabase playbackStateDatabase) {
        return new PlaybackPersistenceManager(playbackStateDatabase);
    }

}
