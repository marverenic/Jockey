package com.marverenic.music.lastfm.data.store;

import android.content.Context;

import com.google.gson.Gson;
import com.marverenic.music.lastfm.model.LfmArtist;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import rx.Observable;

public class DemoLastFmStore implements LastFmStore {

    private Context mContext;

    public DemoLastFmStore(Context context) {
        mContext = context;
    }

    @Override
    public Observable<LfmArtist> getArtistInfo(String artistName) {
        return Observable.fromCallable(() -> {
            InputStream stream = null;
            InputStreamReader reader = null;

            try {
                File json = new File(mContext.getExternalCacheDir(), "lastfm/" + artistName);
                stream = new FileInputStream(json);
                reader = new InputStreamReader(stream);

                return new Gson().fromJson(reader, LfmArtist.class);
            } finally {
                if (stream != null) stream.close();
                if (reader != null) reader.close();
            }
        });
    }

}
