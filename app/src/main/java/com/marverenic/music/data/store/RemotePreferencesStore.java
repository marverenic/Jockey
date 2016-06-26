package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

public class RemotePreferencesStore implements ReadOnlyPreferencesStore {

    private static final String TAG = "RemotePreferencesStore";

    private final boolean mShowFirstStart;
    private final boolean mAllowLogging;
    private final boolean mUseMobileNetwork;
    private final boolean mOpenNowPlayingOnNewQueue;
    private final boolean mEnableNowPlayingGestures;
    private final int mDefaultPage;
    private final int mPrimaryColor;
    private final int mBaseColor;
    private final int mEqualizerPresetId;
    private final boolean mEqualizerEnabled;
    private final String mEqualizerSettings;

    public RemotePreferencesStore(ReadOnlyPreferencesStore preferencesStore) {
        mShowFirstStart = preferencesStore.showFirstStart();
        mAllowLogging = preferencesStore.allowLogging();
        mUseMobileNetwork = preferencesStore.useMobileNetwork();
        mOpenNowPlayingOnNewQueue = preferencesStore.openNowPlayingOnNewQueue();
        mEnableNowPlayingGestures = preferencesStore.enableNowPlayingGestures();
        mDefaultPage = preferencesStore.getDefaultPage();
        mPrimaryColor = preferencesStore.getPrimaryColor();
        mBaseColor = preferencesStore.getBaseColor();
        mEqualizerPresetId = preferencesStore.getEqualizerPresetId();
        mEqualizerEnabled = preferencesStore.getEqualizerEnabled();
        mEqualizerSettings = preferencesStore.getEqualizerSettings().toString();
    }

    @Override
    public boolean showFirstStart() {
        return mShowFirstStart;
    }

    @Override
    public boolean allowLogging() {
        return mAllowLogging;
    }

    @Override
    public boolean useMobileNetwork() {
        return mUseMobileNetwork;
    }

    @Override
    public boolean openNowPlayingOnNewQueue() {
        return mOpenNowPlayingOnNewQueue;
    }

    @Override
    public boolean enableNowPlayingGestures() {
        return mEnableNowPlayingGestures;
    }

    @Override
    public int getDefaultPage() {
        return mDefaultPage;
    }

    @Override
    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    @Override
    public int getBaseColor() {
        return mBaseColor;
    }

    @Override
    public int getEqualizerPresetId() {
        return mEqualizerPresetId;
    }

    @Override
    public boolean getEqualizerEnabled() {
        return mEqualizerEnabled;
    }

    @Override
    public Equalizer.Settings getEqualizerSettings() {
        if (mEqualizerSettings != null) {
            try {
                return new Equalizer.Settings(mEqualizerSettings);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getEqualizerSettings: failed to parse equalizer settings", e);
                Crashlytics.logException(e);
            }
        }
        return null;
    }
}
