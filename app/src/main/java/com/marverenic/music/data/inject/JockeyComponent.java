package com.marverenic.music.data.inject;

import com.marverenic.music.lastfm.data.inject.LastFmModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, PlayerModule.class, MediaStoreModule.class,
        LastFmModule.class})
public interface JockeyComponent extends JockeyGraph {
}
