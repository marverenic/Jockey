package com.marverenic.music;

import android.app.Application;

import com.marverenic.music.utils.Debug;

public class JockeyApplication extends Application implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException (Thread thread, Throwable t){
        Debug.log(t, this);
        defaultHandler.uncaughtException(thread, t);
    }

}
