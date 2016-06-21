package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;
import android.support.annotation.IntDef;

public interface PreferenceStore {

    boolean showFirstStart();
    boolean allowLogging();
    boolean useMobileNetwork();

    boolean openNowPlayingOnNewQueue();
    @StartPage int getDefaultPage();
    @PresetTheme int getPrimaryColor();
    @BaseTheme int getBaseColor();

    int getEqualizerPresetId();
    boolean getEqualizerEnabled();
    Equalizer.Settings getEqualizerSettings();

    void setShowFirstStart(boolean showFirstStart);
    void setAllowLogging(boolean allowLogging);
    void setUseMobileNetwork(boolean useMobileNetwork);

    void setOpenNowPlayingOnNewQueue(boolean openNowPlayingOnNewQueue);
    void setDefaultPage(@StartPage int defaultPage);
    void setPrimaryColor(@PresetTheme int colorChoice);
    void setBaseColor(@BaseTheme int theme);

    void setEqualizerPresetId(int equalizerPresetId);
    void setEqualizerEnabled(boolean equalizerEnabled);
    void setEqualizerSettings(Equalizer.Settings settings);

    @IntDef(value = {StartPage.PLAYLISTS, StartPage.SONGS, StartPage.ARTISTS, StartPage.ALBUMS,
            StartPage.GENRES})
    @interface StartPage {
        int PLAYLISTS = 0;
        int SONGS = 1;
        int ARTISTS = 2;
        int ALBUMS = 3;
        int GENRES = 4;
    }

    @IntDef(value = {PresetTheme.GRAY, PresetTheme.RED, PresetTheme.ORANGE, PresetTheme.YELLOW,
            PresetTheme.GREEN, PresetTheme.BLUE, PresetTheme.PURPLE})
    @interface PresetTheme {
        int GRAY = 0;
        int RED = 1;
        int ORANGE = 2;
        int YELLOW = 3;
        int GREEN = 4;
        int BLUE = 5;
        int PURPLE = 6;
    }

    @IntDef(value = {BaseTheme.DARK, BaseTheme.LIGHT, BaseTheme.AUTO})
    @interface BaseTheme {
        int DARK = 0;
        int LIGHT = 1;
        int AUTO = 2;
    }
}
