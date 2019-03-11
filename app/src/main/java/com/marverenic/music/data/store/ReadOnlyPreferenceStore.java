package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;

import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.BaseTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.data.annotations.StartPage;

import java.util.Set;

public interface ReadOnlyPreferenceStore {

    boolean showFirstStart();
    boolean useMobileNetwork();

    boolean openNowPlayingOnNewQueue();
    boolean enableNowPlayingGestures();
    @StartPage int getDefaultPage();
    @PrimaryTheme int getPrimaryColor();
    @AccentTheme int getAccentColor();
    @BaseTheme int getBaseColor();
    @PrimaryTheme int getIconColor();

    boolean resumeOnHeadphonesConnect();
    boolean isShuffled();
    int getRepeatMode();
    boolean isSlsBroadcastingEnabled();

    long getLastSleepTimerDuration();

    int getEqualizerPresetId();
    boolean getEqualizerEnabled();
    Equalizer.Settings getEqualizerSettings();

    Set<String> getIncludedDirectories();
    Set<String> getExcludedDirectories();

}
