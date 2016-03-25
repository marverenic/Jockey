package com.marverenic.music.lastfm;

import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class Cache {

    private static final String CACHE_EXTENSION = ".lfm";
    private static final int CACHE_DURATION = 7 * 24 * 60 * 60 * 1000; // 1 week in milliseconds
    private static boolean initialized = false;

    /**
     * This class is never instantiated
     */
    private Cache() {

    }

    private static void initializeCache(Context context) {
        //noinspection ResultOfMethodCallIgnored
        new File(context.getExternalCacheDir() + "/lastfm/").mkdirs();
        initialized = true;
    }

    private static String getItemPath(Context context, long id) {
        if (!initialized) {
            initializeCache(context);
        }
        return context.getExternalCacheDir() + "/lastfm/" + id + CACHE_EXTENSION;
    }

    public static boolean hasItem(Context context, long id) {
        File reference = new File(getItemPath(context, id));
        return reference.exists()
                && System.currentTimeMillis() - reference.lastModified() < CACHE_DURATION;
    }

    protected static void cacheArtist(Context context, long artistId, LArtist artist) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter cacheWriter = new FileWriter(getItemPath(context, artistId));
            cacheWriter.write(gson.toJson(artist, LArtist.class));
            cacheWriter.close();
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    protected static LArtist getCachedArtist(Context context, long artistId) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(new FileReader(getItemPath(context, artistId)), LArtist.class);
        } catch (IOException e) {
            Crashlytics.logException(e);
            return null;
        }
    }

}
