package com.marverenic.music.data.inject;

import android.content.Context;

public class JockeyComponentFactory {

    private JockeyComponentFactory() {
        throw new RuntimeException("This class is not instantiable");
    }

    public static JockeyGraph create(Context context) {
        return DaggerJockeyComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
    }

}