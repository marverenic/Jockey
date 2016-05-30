package com.marverenic.music.data.inject;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, MediaStoreModule.class})
public interface JockeyComponent extends JockeyGraph {
}
