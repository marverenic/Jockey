package com.marverenic.music;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.utils.CrashlyticsTree;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class AppInitializer {

    public static void initialize(JockeyApplication application) {
        Fabric.with(application, new Crashlytics());
        Timber.plant(new CrashlyticsTree());
    }

}
