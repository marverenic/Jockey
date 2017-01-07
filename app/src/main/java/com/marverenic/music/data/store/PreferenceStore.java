package com.marverenic.music.data.store;

import android.media.audiofx.Equalizer;

import com.marverenic.music.data.annotations.AccentTheme;
import com.marverenic.music.data.annotations.BaseTheme;
import com.marverenic.music.data.annotations.PrimaryTheme;
import com.marverenic.music.data.annotations.StartPage;

import java.util.Collection;

public interface PreferenceStore extends ReadOnlyPreferenceStore {

    boolean commit();

    void setShowFirstStart(boolean showFirstStart);
    void setAllowLogging(boolean allowLogging);
    void setUseMobileNetwork(boolean useMobileNetwork);

    void setOpenNowPlayingOnNewQueue(boolean openNowPlayingOnNewQueue);
    void setEnableNowPlayingGestures(boolean enabled);
    void setDefaultPage(@StartPage int defaultPage);
    void setPrimaryColor(@PrimaryTheme int colorChoice);
    void setAccentColor(@AccentTheme int accentColor);
    void setBaseColor(@BaseTheme int theme);
    void setIconColor(@PrimaryTheme int theme);

    void toggleShuffle();
    void setShuffle(boolean shuffle);
    void setRepeatMode(int repeatMode);

    void setLastSleepTimerDuration(long timeInMillis);

    void setEqualizerPresetId(int equalizerPresetId);
    void setEqualizerEnabled(boolean equalizerEnabled);
    void setEqualizerSettings(Equalizer.Settings settings);

    void setIncludedDirectories(Collection<String> directories);
    void setExcludedDirectories(Collection<String> directories);

}
