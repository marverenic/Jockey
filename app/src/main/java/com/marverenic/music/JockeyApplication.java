package com.marverenic.music;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;

public class JockeyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this,
                new Crashlytics.Builder()
                        .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                        .build());
        PlayerController.startService(getApplicationContext());
    }
}
