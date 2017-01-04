package com.marverenic.music.utils.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.preference.PreferenceManager;

import com.marverenic.music.data.annotations.BaseTheme;
import com.marverenic.music.data.annotations.PresetTheme;
import com.marverenic.music.data.annotations.StartPage;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.SharedPreferenceStore;

import timber.log.Timber;

import static com.marverenic.music.data.annotations.BaseTheme.LIGHT;
import static com.marverenic.music.data.annotations.PresetTheme.BLUE;
import static com.marverenic.music.data.annotations.StartPage.SONGS;

public class JockeyPreferencesCompat {

    public static void upgradeSharedPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (shouldUpgradeFromJockey1_2(prefs)) {
            Timber.i("upgradeSharedPreferences: Updating from version 1.2");
            updateFromJockey1_2(context, prefs);
            Timber.i("upgradeSharedPreferences: Finished updating from version 1.2");
        }
    }

    private static boolean shouldUpgradeFromJockey1_2(SharedPreferences prefs) {
        return prefs.contains("prefShowFirstStart");
    }

    private static void updateFromJockey1_2(Context context, SharedPreferences prefs) {
        boolean showFirstStart = prefs.getBoolean("prefShowFirstStart", true);
        boolean allowLogging = prefs.getBoolean("prefAllowLogging", false);
        String firstPage = prefs.getString("prefDefaultPage", Integer.toString(SONGS));
        String primaryColor = prefs.getString("prefColorPrimary", Integer.toString(BLUE));
        String baseTheme = prefs.getString("prefBaseTheme", Integer.toString(LIGHT));
        boolean useMobileData = prefs.getBoolean("prefUseMobileData", true);
        boolean openNowPlaying = prefs.getBoolean("prefSwitchToNowPlaying", true);
        boolean enableGestures = prefs.getBoolean("prefEnableNowPlayingGestures", true);
        int eqPreset = prefs.getInt("equalizerPresetId", -1);
        boolean eqEnabled = prefs.getBoolean("prefUseEqualizer", false);
        String eqSettings = prefs.getString("prefEqualizerSettings", null);
        int repeat = prefs.getInt("prefRepeat", 0);
        boolean shuffle = prefs.getBoolean("prefShuffle", false);

        prefs.edit().clear().apply();
        PreferenceStore preferenceStore = new SharedPreferenceStore(context);

        preferenceStore.setShowFirstStart(showFirstStart);
        preferenceStore.setAllowLogging(allowLogging);
        preferenceStore.setDefaultPage(convertStartPage1_2(firstPage));
        preferenceStore.setPrimaryColor(convertPrimaryColor1_2(primaryColor));
        preferenceStore.setBaseColor(convertBaseTheme1_2(baseTheme));
        preferenceStore.setUseMobileNetwork(useMobileData);
        preferenceStore.setOpenNowPlayingOnNewQueue(openNowPlaying);
        preferenceStore.setEnableNowPlayingGestures(enableGestures);
        preferenceStore.setEqualizerPresetId(eqPreset);
        preferenceStore.setEqualizerEnabled(eqEnabled);

        try {
            if (eqSettings != null) {
                preferenceStore.setEqualizerSettings(new Equalizer.Settings(eqSettings));
            }
        } catch (IllegalArgumentException ignored) {}

        preferenceStore.setRepeatMode(repeat);
        preferenceStore.setShuffle(shuffle);
    }

    @StartPage
    private static int convertStartPage1_2(String startPage) {
        try {
            int convertedDefaultPage = Integer.parseInt(startPage);
            if (convertedDefaultPage < 0 || convertedDefaultPage > 4) {
                return SONGS;
            } else {
                //noinspection WrongConstant
                return convertedDefaultPage;
            }
        } catch (NumberFormatException ignored) {
            return SONGS;
        }
    }

    @PresetTheme
    private static int convertPrimaryColor1_2(String primaryColor) {
        try {
            int convertedColor = Integer.parseInt(primaryColor);
            if (convertedColor < 0 || convertedColor > 6) {
                return BLUE;
            } else {
                //noinspection WrongConstant
                return convertedColor;
            }
        } catch (NumberFormatException ignored) {
            return BLUE;
        }
    }

    @BaseTheme
    private static int convertBaseTheme1_2(String baseTheme) {
        try {
            int convertedBaseTheme = Integer.parseInt(baseTheme);
            if (convertedBaseTheme < 0 || convertedBaseTheme > 2) {
                return LIGHT;
            } else {
                //noinspection WrongConstant
                return convertedBaseTheme;
            }
        } catch (NumberFormatException ignored) {
            return LIGHT;
        }
    }

}
