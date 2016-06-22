package com.marverenic.music.data.inject;

import android.content.Context;

import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.data.store.SharedPreferencesStore;

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
    public PreferencesStore providePreferencesStore(Context context) {
        return new SharedPreferencesStore(context);
    }

}
