package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.PresetThemeStore;
import com.marverenic.music.data.store.SharedPreferenceStore;
import com.marverenic.music.data.store.ThemeStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ContextModule {

    private Context mContext;

    public ContextModule(Context context) {
        mContext = context;
    }

    @Provides
    public Context provideContext() {
        return mContext;
    }

    @Provides
    @Singleton
    public PreferenceStore providePreferencesStore(Context context) {
        return new SharedPreferenceStore(context);
    }

    @Provides
    @Singleton
    public ThemeStore provideThemeStore(Context context, PreferenceStore preferenceStore) {
        return new PresetThemeStore(context, preferenceStore);
    }

}
