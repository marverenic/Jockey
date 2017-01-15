package com.marverenic.music;

import android.support.annotation.NonNull;

import com.marverenic.music.data.inject.ContextModule;
import com.marverenic.music.data.inject.DaggerTestComponent;
import com.marverenic.music.data.inject.JockeyGraph;

public class RobolectricJockeyApplication extends JockeyApplication {

    @NonNull
    @Override
    protected JockeyGraph createDaggerComponent() {
        return DaggerTestComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
    }
}
