package com.marverenic.music.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

public class Prefs {

    // Preference keys
    /**
     * Whether or not to preform first start actions. Default value is true
     */
    public static final String SHOW_FIRST_START = "prefShowFirstStart";
    /**
     * Whether or not to allow usage logging to Crashlytics. Crash logging may not be disabled
     * because of its importance in developing Jockey.
     */
    public static final String ALLOW_LOGGING = "prefAllowLogging";
    /**
     * Which page to show by default when opening Jockey. Must be a numeric value that corresponds
     * to a value given by
     * {@link com.marverenic.music.activity.LibraryActivity.PagerAdapter#getItem(int)}
     */
    public static final String DEFAULT_PAGE = "prefDefaultPage";
    /**
     * What accent color Jockey should use. 0 for black, 1 for red, 2 for orange, 3 for yellow,
     * 4 for green, 5 (default) for blue, and 6 for purple
     */
    public static final String PRIMARY_COLOR = "prefColorPrimary";
    /**
     * Whether Jockey should use a light or dark theme. 1 for light, 0 for dark
     */
    public static final String BASE_COLOR = "prefBaseTheme";
    /**
     * A temporary preference used for adding color-coordinated shortcuts to the launcher
     */
    public static final String ADD_SHORTCUT = "prefAddShortcut";
    /**
     * Whether or not Jockey can use mobile data to retrieve information from Last.Fm. This flag
     * doesn't affect Crashlytics currently
     */
    public static final String USE_MOBILE_NET = "prefUseMobileData";
    /**
     * Whether or not to navigate to the Now Playing Activity when the user picks a new song
     */
    public static final String SWITCH_TO_PLAYING = "prefSwitchToNowPlaying";
    /**
     * The prefix used to record equalizer band levels. Append a numeric suffix to represent the
     * band number up to {@link Equalizer#getNumberOfBands()}. The value associated with this key
     * corresponds to the value used in {@link Equalizer#setBandLevel(short, short)} and
     * {@link Equalizer#getBandLevel(short)}
     */
    public static final String EQ_BAND_PREFIX = "equalizerLevel";
    /**
     * An equalizer preset defined by the system that the user has selected as indexed by
     * {@link Equalizer#getPresetName(short)} and {@link Equalizer#usePreset(short)}.
     * -1 is saved to specify a custom equalizer configuration
     */
    public static final String EQ_PRESET_ID = "equalizerPresetId";


    /**
     * Shorthand to get the default {@link SharedPreferences}. Equivalent to calling
     * {@link PreferenceManager#getDefaultSharedPreferences(Context)}
     * @param context A {@link Context} used to open the preferences
     * @return The default {@link SharedPreferences}
     */
    public static SharedPreferences getPrefs(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Verify that network use is allowed right now. Takes into account the user's current network
     * type and whether or not they have disabled data over mobile networks.
     * @param context A {@link Context} used to query the current network configuration
     * @return Whether Jockey is permitted to use the network right now
     */
    public static boolean allowNetwork(Context context){
        ConnectivityManager network =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return network.getActiveNetworkInfo() != null && network.getActiveNetworkInfo().isAvailable()
                && !network.getActiveNetworkInfo().isRoaming()
                && (Prefs.getPrefs(context).getBoolean(Prefs.USE_MOBILE_NET, true)
                || network.getActiveNetworkInfo().getType() != ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Verify that the user has allowed additional logging to occur. This doesn't affect
     * Crashlytics crashes and caught exception logging
     * @param context A {@link Context} used to verify this preference
     * @return Whether {@link Prefs#ALLOW_LOGGING} is true in the default {@link SharedPreferences}
     */
    public static boolean allowAnalytics(Context context){
        return getPrefs(context).getBoolean(ALLOW_LOGGING, false);
    }

}
