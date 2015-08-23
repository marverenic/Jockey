package com.marverenic.music.utils;

import android.content.Context;
import android.content.SharedPreferences;
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
    public static final String DEFAULT_PAGE = "prefDefaultPage";
    public static final String PRIMARY_COLOR = "prefColorPrimary";
    public static final String BASE_COLOR = "prefBaseTheme";
    public static final String ADD_SHORTCUT = "prefAddShortcut";
    public static final String USE_MOBILE_NET = "prefUseMobileData";
    public static final String SWITCH_TO_PLAYING = "prefSwitchToNowPlaying";


    public static SharedPreferences getPrefs(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
