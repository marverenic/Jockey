package com.marverenic.music.utils;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class CrashlyticsTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable throwable) {
        if (throwable != null) {
            Crashlytics.log(priority, tag, message);
            Crashlytics.logException(throwable);
        }
    }

}
