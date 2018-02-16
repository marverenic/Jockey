package com.marverenic.music.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.AudioEffect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.webkit.MimeTypeMap;

import com.bumptech.glide.Glide;
import com.marverenic.music.R;
import com.marverenic.music.model.Song;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import rx.Observable;
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

    private static final List<String> AUDIO_MIMES = Arrays.asList(
            "application/ogg",
            "application/x-ogg",
            "application/itunes"
    );

    private static final List<String> COVER_IMAGE_NAMES = Arrays.asList(
            "cover.jpg",
            "folder.jpg"
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

    @Nullable
    private static byte[] fetchEmbeddedArtwork(Context context, Uri songLocation) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(context, songLocation);
            return retriever.getEmbeddedPicture();
        } catch (RuntimeException e) {
            Timber.e(e, "Failed to load full song artwork");
        } catch (OutOfMemoryError e) {
            Timber.e(e, "Unable to allocate space on the heap for full song artwork");
        } finally {
            retriever.release();
        }

        return null;
    }

    @Nullable
    private static File resolveArtwork(Context context, Uri songLocation) {
        String path = UriUtils.getPathFromUri(context, songLocation);
        if (path == null) {
            return null;
        }

        File directory = new File(path).getParentFile();
        for (String cover : COVER_IMAGE_NAMES) {
            File image = new File(directory, cover);
            if (image.canRead()) {
                return image;
            }
        }

        return null;
    }

    @Nullable
    private static String resolveMediaStoreArtwork(Context context, Uri songLocation) {
        Cursor songCursor = null;
        Cursor albumCursor = null;
        try {
            String[] songProjection = new String[] { MediaStore.Audio.AudioColumns.ALBUM_ID };

            songCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songProjection, MediaStore.Audio.AudioColumns.DATA + " = ?",
                    new String[] { songLocation.getPath() }, null);

            if (songCursor == null || !songCursor.moveToFirst()) {
                return null;
            }

            long albumId = songCursor.getLong(0);

            String[] albumProjection = new String[] { MediaStore.Audio.AlbumColumns.ALBUM_ART };
            albumCursor = context.getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumProjection,
                    MediaStore.Audio.AudioColumns._ID + " = " + albumId, null, null);

            if (albumCursor == null || !albumCursor.moveToFirst()) {
                return null;
            }

            return albumCursor.getString(0);
        } finally {
            if (songCursor != null) {
                songCursor.close();
            }
            if (albumCursor != null) {
                albumCursor.close();
            }
        }
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Song song) {
        if (song == null) {
            return fetchArtwork(context, (Uri) null);
        } else {
            return fetchArtwork(context, song.getLocation());
        }
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Uri songLocation) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int size = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        return fetchArtwork(context, songLocation, size);
    }

    public static Observable<Bitmap> fetchArtwork(Context context, Uri songLocation, int size) {
        return Observable.fromCallable(() -> {
            if (songLocation == null) {
                return Glide.with(context).load(R.drawable.art_default_xl);
            }

            byte[] embedded = fetchEmbeddedArtwork(context, songLocation);
            if (embedded != null){
                return Glide.with(context).load(embedded);
            }

            File folderImage = resolveArtwork(context, songLocation);
            if (folderImage != null) {
                return Glide.with(context).load(folderImage);
            }

            String mediaStoreThumbnail = resolveMediaStoreArtwork(context, songLocation);
            if (mediaStoreThumbnail != null) {
                return Glide.with(context).load(mediaStoreThumbnail);
            }

            return Glide.with(context).load(R.drawable.art_default_xl);
        }).map(request -> {
            try {
                return request.asBitmap()
                        .atMost()
                        .into(size, size)
                        .get();
            } catch (InterruptedException|ExecutionException e) {
                Timber.e(e, "Failed to load artwork");
                return BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.art_default_xl);
            }
        });
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

}
