package com.marverenic.music;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Themes;
import com.squareup.picasso.Picasso;
import io.fabric.sdk.android.Fabric;

public class JockeyApplication extends Application implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        setTheme(Themes.getTheme(this));
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Picasso.setSingletonInstance(new Picasso.Builder(this).indicatorsEnabled(true).build());
        }
        else {
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
            Fabric.with(this, new Crashlytics());
        }

        PlayerController.startService(getApplicationContext());
    }

    @Override
    public void uncaughtException (Thread thread, Throwable t) {
        Debug.log(t, this);
        defaultHandler.uncaughtException(thread, t);
    }
}
