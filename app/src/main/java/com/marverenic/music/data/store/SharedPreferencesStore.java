package com.marverenic.music.data.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;

import com.marverenic.music.R;
import com.marverenic.music.data.annotations.BaseTheme;
import com.marverenic.music.data.annotations.PresetTheme;
import com.marverenic.music.data.annotations.StartPage;
import com.marverenic.music.player.MusicPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class SharedPreferencesStore implements PreferencesStore {

    private Context mContext;
    private SharedPreferences mPrefs;

    public SharedPreferencesStore(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    private boolean contains(@StringRes int keyRes) {
        return mPrefs.contains(mContext.getString(keyRes));
    }

    private boolean getBoolean(@StringRes int keyRes, boolean defaultValue) {
        return mPrefs.getBoolean(mContext.getString(keyRes), defaultValue);
    }

    private int getInt(@StringRes int keyRes, int defaultValue) {
        return mPrefs.getInt(mContext.getString(keyRes), defaultValue);
    }

    private long getLong(@StringRes int keyRes, long defaultValue) {
        return mPrefs.getLong(mContext.getString(keyRes), defaultValue);
    }

    private String getString(@StringRes int keyRes, String defaultValue) {
        return mPrefs.getString(mContext.getString(keyRes), defaultValue);
    }

    private Set<String> getStringSet(@StringRes int keyRes) {
        return mPrefs.getStringSet(mContext.getString(keyRes), Collections.emptySet());
    }

    private void putBoolean(@StringRes int keyRes, boolean value) {
        mPrefs.edit()
                .putBoolean(mContext.getString(keyRes), value)
                .apply();
    }

    private void putInt(@StringRes int keyRes, int value) {
        mPrefs.edit()
                .putInt(mContext.getString(keyRes), value)
                .apply();
    }

    private void putLong(@StringRes int keyRes, long value) {
        mPrefs.edit()
                .putLong(mContext.getString(keyRes), value)
                .apply();
    }

    private void putString(@StringRes int keyRes, String value) {
        mPrefs.edit()
                .putString(mContext.getString(keyRes), value)
                .apply();
    }

    private void putStringSet(@StringRes int keyRes, Set<String> value) {
        mPrefs.edit()
                .putStringSet(mContext.getString(keyRes), value)
                .apply();
    }

    @Override
    public boolean showFirstStart() {
        return getBoolean(R.string.pref_key_show_first_start, true);
    }

    @Override
    public boolean allowLogging() {
        return getBoolean(R.string.pref_key_allow_logging, false);
    }

    @Override
    public boolean useMobileNetwork() {
        return getBoolean(R.string.pref_key_use_mobile_net, true);
    }

    @Override
    public boolean openNowPlayingOnNewQueue() {
        return getBoolean(R.string.pref_key_switch_to_playing, true);
    }

    @Override
    public boolean enableNowPlayingGestures() {
        return getBoolean(R.string.pref_key_enable_gestures, true);
    }

    @Override
    @SuppressWarnings("WrongConstant")
    public int getDefaultPage() {
        return getInt(R.string.pref_key_default_page, StartPage.SONGS);
    }

    @Override
    @SuppressWarnings("WrongConstant")
    public int getPrimaryColor() {
        return getInt(R.string.pref_key_color_primary, PresetTheme.BLUE);
    }

    @Override
    @SuppressWarnings("WrongConstant")
    public int getBaseColor() {
        return getInt(R.string.pref_key_color_base, BaseTheme.LIGHT);
    }

    @Override
    public int getIconColor() {
        //noinspection WrongConstant
        return getInt(R.string.pref_key_color_icon, PresetTheme.BLUE);
    }

    @Override
    public boolean isShuffled() {
        return getBoolean(R.string.pref_key_shuffle, false);
    }

    @Override
    public int getRepeatMode() {
        return getInt(R.string.pref_key_repeat, MusicPlayer.REPEAT_NONE);
    }

    @Override
    public long getLastSleepTimerDuration() {
        return getLong(R.string.pref_key_last_sleep_timer, TimeUnit.MINUTES.toMillis(15));
    }

    @Override
    public int getEqualizerPresetId() {
        return getInt(R.string.pref_key_eq_id, -1);
    }

    @Override
    public boolean getEqualizerEnabled() {
        return getBoolean(R.string.pref_key_eq_enabled, false);
    }

    @Override
    public Equalizer.Settings getEqualizerSettings() {
        if (contains(R.string.pref_key_eq_settings)) {
            try {
                return new Equalizer.Settings(getString(R.string.pref_key_eq_settings, null));
            } catch (IllegalArgumentException exception) {
                Timber.e(exception, "getEqualizerSettings: failed to parse equalizer settings");
            }
        }
        return null;
    }

    @Override
    public Set<String> getIncludedDirectories() {
        return getStringSet(R.string.pref_key_included_dirs);
    }

    @Override
    public Set<String> getExcludedDirectories() {
        return getStringSet(R.string.pref_key_excluded_dirs);
    }

    @Override
    public void setShowFirstStart(boolean showFirstStart) {
        putBoolean(R.string.pref_key_show_first_start, showFirstStart);
    }

    @Override
    public void setAllowLogging(boolean allowLogging) {
        putBoolean(R.string.pref_key_allow_logging, allowLogging);
    }

    @Override
    public void setUseMobileNetwork(boolean useMobileNetwork) {
        putBoolean(R.string.pref_key_use_mobile_net, useMobileNetwork);
    }

    @Override
    public void setOpenNowPlayingOnNewQueue(boolean openNowPlayingOnNewQueue) {
        putBoolean(R.string.pref_key_switch_to_playing, openNowPlayingOnNewQueue);
    }

    @Override
    public void setEnableNowPlayingGestures(boolean enabled) {
        putBoolean(R.string.pref_key_enable_gestures, enabled);
    }

    @Override
    public void setDefaultPage(@StartPage int defaultPage) {
        putInt(R.string.default_page_pref, defaultPage);
    }

    @Override
    public void setPrimaryColor(@PresetTheme int colorChoice) {
        putInt(R.string.pref_key_color_primary, colorChoice);
    }

    @Override
    public void setBaseColor(@BaseTheme int themeChoice) {
        putInt(R.string.pref_key_color_base, themeChoice);
    }

    @Override
    public void setIconColor(@PresetTheme int theme) {
        putInt(R.string.pref_key_color_icon, theme);
    }

    @Override
    public void toggleShuffle() {
        setShuffle(!isShuffled());
    }

    @Override
    public void setShuffle(boolean shuffle) {
        putBoolean(R.string.pref_key_shuffle, shuffle);
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        putInt(R.string.pref_key_repeat, repeatMode);
    }

    @Override
    public void setLastSleepTimerDuration(long timeInMillis) {
        putLong(R.string.pref_key_last_sleep_timer, timeInMillis);
    }

    @Override
    public void setEqualizerPresetId(int equalizerPresetId) {
        putInt(R.string.pref_key_eq_id, equalizerPresetId);
    }

    @Override
    public void setEqualizerEnabled(boolean equalizerEnabled) {
        putBoolean(R.string.pref_key_eq_enabled, equalizerEnabled);
    }

    @Override
    public void setEqualizerSettings(Equalizer.Settings settings) {
        putString(R.string.pref_key_eq_settings, settings.toString());
    }

    @Override
    public void setIncludedDirectories(Collection<String> directories) {
        putStringSet(R.string.pref_key_included_dirs, new HashSet<>(directories));
    }

    @Override
    public void setExcludedDirectories(Collection<String> directories) {
        putStringSet(R.string.pref_key_excluded_dirs, new HashSet<>(directories));
    }
}
