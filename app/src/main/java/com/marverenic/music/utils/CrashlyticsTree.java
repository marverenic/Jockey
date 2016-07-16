package com.marverenic.music.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class CrashlyticsTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable throwable) {
        if (throwable != null) {
            Crashlytics.logException(throwable);
        } else if (priority == Log.ERROR) {
            Crashlytics.log(priority, tag, message);
        }
    }

}
