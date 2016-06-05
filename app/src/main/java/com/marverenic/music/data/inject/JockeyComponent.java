package com.marverenic.music.data.inject;

import com.marverenic.music.lastfm2.data.inject.LastFmModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, MediaStoreModule.class, LastFmModule.class})
public interface JockeyComponent {
}
