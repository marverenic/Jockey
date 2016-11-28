package com.marverenic.music.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.AudioEffect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import com.marverenic.music.model.Song;

import java.util.UUID;

import timber.log.Timber;

import static android.content.Context.CONNECTIVITY_SERVICE;

public final class Util {

    /**
     * This UUID corresponds to the UUID of an Equalizer Audio Effect. It has been copied from
     * {@link AudioEffect#EFFECT_TYPE_EQUALIZER} for backwards compatibility since this field was
     * added in API level 18.
     */
    private static final UUID EQUALIZER_UUID;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            EQUALIZER_UUID = AudioEffect.EFFECT_TYPE_EQUALIZER;
        } else {
            EQUALIZER_UUID = UUID.fromString("0bed4300-ddd6-11db-8f34-0002a5d5c51b");
        }
    }

    /**
     * This class is never instantiated
     */
    private Util() {

    }

    /**
     * Checks whether the device is in a state where we're able to access the internet. If the
     * device is not connected to the internet, this will return {@code false}. If the device is
     * only connected to a mobile network, this will return {@code allowMobileNetwork}. If the
     * device is connected to an active WiFi network, this will return {@code true.}
     * @param context A context used to check the current network status
     * @param allowMobileNetwork Whether or not the user allows the application to use mobile
     *                           data. This is an internal implementation that is not enforced
     *                           by the system, but is exposed to the user in our app's settings.
     * @return Whether network calls should happen in the current connection state or not
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public static boolean canAccessInternet(Context context, boolean allowMobileNetwork) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info == null) {
            // No network connections are active
            return false;
        } else if (!info.isAvailable() || info.isRoaming()) {
            // The network isn't active, or is a roaming network
            return false;
        } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
            // If it's a mobile network, return the user preference
            return allowMobileNetwork;
        } else {
            // The network is a wifi network
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermission(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static Intent getSystemEqIntent(Context c) {
        Intent systemEq = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        systemEq.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, c.getPackageName());

        ActivityInfo info = systemEq.resolveActivityInfo(c.getPackageManager(), 0);
        if (info != null && !info.name.startsWith("com.android.musicfx")) {
            return systemEq;
        } else {
            return null;
        }
    }

    /**
     * Checks whether the current device is capable of instantiating and using an
     * {@link android.media.audiofx.Equalizer}
     * @return True if an Equalizer may be used at runtime
     */
    public static boolean hasEqualizer() {
        for (AudioEffect.Descriptor effect : AudioEffect.queryEffects()) {
            if (EQUALIZER_UUID.equals(effect.type)) {
                return true;
            }
        }
        return false;
    }

    public static Bitmap fetchFullArt(Song song) {
        if (song == null) {
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(song.getLocation());
            byte[] stream = retriever.getEmbeddedPicture();
            if (stream != null) {
                return BitmapFactory.decodeByteArray(stream, 0, stream.length);
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to load full song artwork");
        }
        return null;
    }

}
