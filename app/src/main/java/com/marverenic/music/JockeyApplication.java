package com.marverenic.music;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.marverenic.music.data.inject.ContextModule;
import com.marverenic.music.data.inject.DaggerJockeyComponent;
import com.marverenic.music.data.inject.JockeyComponent;
import com.marverenic.music.utils.compat.JockeyPreferencesCompat;

import io.fabric.sdk.android.Fabric;

public class JockeyApplication extends Application {

    private JockeyComponent mComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        JockeyPreferencesCompat.upgradeSharedPreferences(this);

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
