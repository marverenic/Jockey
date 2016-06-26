package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;

import com.marverenic.music.utils.BaseTheme;
import com.marverenic.music.utils.PresetTheme;
import com.marverenic.music.utils.StartPage;

public interface PreferencesStore extends ReadOnlyPreferencesStore {

    void setShowFirstStart(boolean showFirstStart);
    void setAllowLogging(boolean allowLogging);
    void setUseMobileNetwork(boolean useMobileNetwork);

    void setOpenNowPlayingOnNewQueue(boolean openNowPlayingOnNewQueue);
    void setEnableNowPlayingGestures(boolean enabled);
    void setDefaultPage(@StartPage int defaultPage);
    void setPrimaryColor(@PresetTheme int colorChoice);
    void setBaseColor(@BaseTheme int theme);

    void setShuffle(boolean shuffle);
    void setRepeatMode(int repeatMode);

    void setEqualizerPresetId(int equalizerPresetId);
    void setEqualizerEnabled(boolean equalizerEnabled);
    void setEqualizerSettings(Equalizer.Settings settings);

}
