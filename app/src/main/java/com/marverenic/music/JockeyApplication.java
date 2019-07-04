package com.marverenic.music;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.marverenic.music.data.inject.JockeyComponentFactory;
import com.marverenic.music.data.inject.JockeyGraph;
import com.marverenic.music.utils.compat.JockeyPreferencesCompat;
import com.marverenic.music.utils.compat.PlayerQueueMigration;

import timber.log.Timber;

public class JockeyApplication extends Application {

    private JockeyGraph mComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber();
        AppInitializer.initialize(this);

        mComponent = createDaggerComponent();
        JockeyPreferencesCompat.upgradeSharedPreferences(this);
        new PlayerQueueMigration(this).migrateLegacyQueueFile();
    }

    @NonNull
    protected JockeyGraph createDaggerComponent() {
        return JockeyComponentFactory.create(this);
    }

    private void setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.with(this).onTrimMemory(level);
    }

    public static JockeyGraph getComponent(Fragment fragment) {
        return getComponent(fragment.getContext());
    }

    public static JockeyGraph getComponent(Context context) {
        return ((JockeyApplication) context.getApplicationContext()).mComponent;
    }
}
