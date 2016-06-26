package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;

import com.marverenic.music.utils.BaseTheme;
import com.marverenic.music.utils.PresetTheme;
import com.marverenic.music.utils.StartPage;

public interface ReadOnlyPreferencesStore {

    boolean showFirstStart();
    boolean allowLogging();
    boolean useMobileNetwork();

    boolean openNowPlayingOnNewQueue();
    boolean enableNowPlayingGestures();
    @StartPage int getDefaultPage();
    @PresetTheme int getPrimaryColor();
    @BaseTheme int getBaseColor();

    boolean isShuffled();
    int getRepeatMode();

    int getEqualizerPresetId();
    boolean getEqualizerEnabled();
    Equalizer.Settings getEqualizerSettings();

}
