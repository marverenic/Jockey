package com.marverenic.music.data.inject;

import com.marverenic.music.lastfm.data.inject.DemoLastFmModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, PlayerModule.class, DemoModule.class,
        DemoLastFmModule.class, JockeyStatusModule.class})
public interface DemoComponent extends JockeyGraph {
}
