package com.marverenic.music;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.marverenic.music.data.inject.ContextModule;
import com.marverenic.music.data.inject.DaggerJockeyComponent;
import com.marverenic.music.data.inject.JockeyComponent;
import com.marverenic.music.utils.CrashlyticsTree;
import com.marverenic.music.utils.compat.JockeyPreferencesCompat;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class JockeyApplication extends Application {

    private JockeyComponent mComponent;

    @Override
    public void onCreate() {
        setupStrictMode();
        super.onCreate();

        setupCrashlytics();
        setupTimber();

        createComponent();

        JockeyPreferencesCompat.upgradeSharedPreferences(this);
    }

    private void setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }
    }

    private void setupCrashlytics() {
        Fabric.with(this, new Crashlytics());
    }

    private void setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashlyticsTree());
        }
    }

    private void createComponent() {
        mComponent = DaggerJockeyComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.with(this).onTrimMemory(level);
    }

    public static JockeyComponent getComponent(Fragment fragment) {
        return getComponent(fragment.getContext());
    }

    public static JockeyComponent getComponent(Context context) {
        return ((JockeyApplication) context.getApplicationContext()).mComponent;
    }
}
