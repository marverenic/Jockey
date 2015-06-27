package com.marverenic.music;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.squareup.picasso.Picasso;

import io.fabric.sdk.android.Fabric;

public class JockeyApplication extends Application {

    @Override
    public void onCreate() {
        Fabric.with(this, new Crashlytics());
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Picasso.setSingletonInstance(new Picasso.Builder(this).indicatorsEnabled(true).build());
        }

        PlayerController.startService(getApplicationContext());
    }
}
