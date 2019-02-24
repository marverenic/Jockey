package com.marverenic.music.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.audiofx.AudioEffect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    private static final List<String> AUDIO_MIMES = Arrays.asList(
            "application/ogg",
            "application/x-ogg",
            "application/itunes"
    );

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

    public static boolean isFileMusic(File file) {
        String fileName = file.getName();
        if (!fileName.contains(".") || fileName.lastIndexOf('.') <= 0) {
            return false;
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return isMimeTypeAudio(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
    }

    private static boolean isMimeTypeAudio(String mime) {
        return mime != null && mime.startsWith("audio") || AUDIO_MIMES.contains(mime);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        int width = Math.max(drawable.getIntrinsicWidth(), 1);
        int height = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
