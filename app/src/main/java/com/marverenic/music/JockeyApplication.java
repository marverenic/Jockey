package com.marverenic.music;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.marverenic.music.data.inject.JockeyComponentFactory;
import com.marverenic.music.data.inject.JockeyGraph;
import com.marverenic.music.utils.CrashlyticsTree;
import com.marverenic.music.utils.compat.JockeyPreferencesCompat;
import com.marverenic.music.utils.compat.PlayerQueueMigration;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class JockeyApplication extends Application {

    private JockeyGraph mComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        setupCrashlytics();
        setupTimber();

        mComponent = createDaggerComponent();
        JockeyPreferencesCompat.upgradeSharedPreferences(this);
        new PlayerQueueMigration(this).migrateLegacyQueueFile();
    }

    @NonNull
    protected JockeyGraph createDaggerComponent() {
        return JockeyComponentFactory.create(this);
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
