package com.marverenic.music.data.inject;

import com.marverenic.music.lastfm.data.inject.DemoLastFmModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ContextModule.class, TestPlayerModule.class, DemoModule.class,
        DemoLastFmModule.class})
public interface TestComponent extends JockeyGraph {
}
