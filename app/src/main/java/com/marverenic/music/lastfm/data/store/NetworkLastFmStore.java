package com.marverenic.music.lastfm.data.store;

import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.lastfm.api.LastFmService;
import com.marverenic.music.lastfm.model.LfmArtist;

import java.io.IOException;

import rx.Observable;
import rx.exceptions.Exceptions;

public class NetworkLastFmStore implements LastFmStore {

    private static final String TAG = "NetworkLastFmStore";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private LastFmService mService;
    private SimpleArrayMap<String, Observable<LfmArtist>> mCachedArtistInfo;

    public NetworkLastFmStore(LastFmService service) {
        mService = service;
        mCachedArtistInfo = new SimpleArrayMap<>();
    }

    @Override
    public Observable<LfmArtist> getArtistInfo(String artistName) {
        Observable<LfmArtist> result = mCachedArtistInfo.get(artistName);
        if (result == null) {
            result = mService.getArtistInfo(artistName)
                    .map(response -> {
                        if (!response.isSuccessful()) {
                            String message = "Call to getArtistInfo failed with response code "
                                    + response.code()
                                    + "\n" + response.message();

                            if (DEBUG) Log.e(TAG, message);
                            throw Exceptions.propagate(new IOException(message));
                        }

                        return response.body().getArtist();
                    })
                    .cache();

            mCachedArtistInfo.put(artistName, result);
        }

        return result;
    }
}
