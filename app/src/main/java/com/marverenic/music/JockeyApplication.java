package com.marverenic.music;

import android.app.Application;

import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Themes;
import com.squareup.picasso.Picasso;

public class JockeyApplication extends Application implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        setTheme(Themes.getTheme(this));
        super.onCreate();

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        if (BuildConfig.DEBUG)
            Picasso.setSingletonInstance(new Picasso.Builder(this).indicatorsEnabled(true).build());

        PlayerController.startService(getApplicationContext());
    }

    @Override
    public void uncaughtException (Thread thread, Throwable t){
        Debug.log(t, this);
        defaultHandler.uncaughtException(thread, t);
    }
}
