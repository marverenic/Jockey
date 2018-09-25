package com.marverenic.music.data.inject;

import android.content.Context;
import android.content.SharedPreferences;

import com.marverenic.music.data.api.JockeyStatusApi;
import com.marverenic.music.data.api.JockeyStatusService;
import com.marverenic.music.data.manager.PrivacyPolicyManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class JockeyStatusModule {

    @Provides
    @Singleton
    public JockeyStatusService provideJockeyStatusService() {
        return JockeyStatusApi.getService();
    }

    @Provides
    @Singleton
    public PrivacyPolicyManager getPrivacyPolicyManager(Context context,
                                                        JockeyStatusService statusService,
                                                        SharedPreferences sharedPreferences) {
        return new PrivacyPolicyManager(context, statusService, sharedPreferences);
    }

}
