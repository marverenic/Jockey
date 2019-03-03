package com.marverenic.music.utils;

import android.content.Context;
import android.content.Intent;

public class BroadcastUtils {

    /**
     * An {@link Intent} extra sent with {@link #getUpdateBroadcast} intents which maps to a boolean
     * representing whether or not the update is a minor update (i.e. an update that was triggered
     * by the user).
     */
    public static final String UPDATE_EXTRA_MINOR = "marverenic.jockey.player.REFRESH:minor";

    /**
     * An {@link Intent} extra sent with {@link #getInfoBroadcast} intents which maps to a
     * user-friendly information message
     */
    public static final String INFO_EXTRA_MESSAGE = "marverenic.jockey.player.INFO:MSG";

    /**
     * An {@link Intent} extra sent with {@link #getErrorBroadcast(Context)} intents which maps to a
     * user-friendly error message
     */
    public static final String ERROR_EXTRA_MSG = "marverenic.jockey.player.ERROR:MSG";

    /**
     * Package permission that is required to receive broadcasts
     */
    public static String getBroadcastPermission(Context context) {
        return context.getPackageName() + ".MUSIC_BROADCAST_PERMISSION";
    }

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has changed its state automatically
     */
    public static String getUpdateBroadcast(Context context) {
        return context.getPackageName() + ":player.REFRESH";
    }

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has information that should be
     * presented to the user
     * @see #INFO_EXTRA_MESSAGE
     */
    public static String getInfoBroadcast(Context context) {
        return context.getPackageName() + ":player.INFO";
    }

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has encountered an error when
     * setting the current playback source
     * @see #ERROR_EXTRA_MSG
     */
    public static String getErrorBroadcast(Context context) {
        return context.getPackageName() + ":player.ERROR";
    }

}
