package com.marverenic.music.data.store;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.instances.Song;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class LocalPlayCountStore implements PlayCountStore {

    private static final String TAG = "LocalPlayCountStore";

    private static final String PLAY_COUNT_FILENAME = ".playcount";
    private static final String PLAY_COUNT_HEADER = "This file contains play count information for "
            + "Jockey and should not be edited";

    private Context mContext;
    private final LongSparseArray<Count> mCounts;

    public LocalPlayCountStore(Context context) {
        mContext = context;
        mCounts = new LongSparseArray<>();
    }

    @Override
    public Observable<Void> refresh() {
        return Observable.fromCallable(
                () -> {
                    mCounts.clear();

                    // TODO read counts from disk

                    return (Void) null;
                })
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread());
    }

    private File getPlayCountFile() {
        return new File(mContext.getExternalFilesDir(null), PLAY_COUNT_FILENAME);
    }

    @Override
    public void save() {
        Properties properties = new Properties();

        for (int i = 0; i < mCounts.size(); i++) {
            String key = Long.toString(mCounts.keyAt(i));
            String value = mCounts.valueAt(i).getCommaSeparatedValues();
            properties.put(key, value);
        }

        try {
            writePlayCounts(properties);
        } catch (IOException e) {
            Log.e(TAG, "save: Failed to write play counts to disk", e);
            Crashlytics.logException(e);
        }
    }

    private void writePlayCounts(Properties playCounts) throws IOException {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(getPlayCountFile());
            playCounts.store(fileWriter, PLAY_COUNT_HEADER);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    @Override
    public int getPlayCount(Song song) {
        Count count = mCounts.get(song.getSongId());
        if (count == null) {
            return 0;
        } else {
            return count.mPlays;
        }
    }

    @Override
    public int getSkipCount(Song song) {
        Count count = mCounts.get(song.getSongId());
        if (count == null) {
            return 0;
        } else {
            return count.mSkips;
        }
    }

    @Override
    public long getPlayDate(Song song) {
        Count count = mCounts.get(song.getSongId());
        if (count == null) {
            return 0;
        } else {
            return count.mDate;
        }
    }

    @Override
    public void incrementPlayCount(Song song) {
        setPlayCount(song, getPlayCount(song) + 1);
    }

    @Override
    public void incrementSkipCount(Song song) {
        setSkipCount(song, getSkipCount(song) + 1);
    }

    @Override
    public void setPlayDateToNow(Song song) {
        setPlayDate(song, System.currentTimeMillis());
    }

    private Count getOrInitializeCount(Song song) {
        Count count = mCounts.get(song.getSongId());
        if (count == null) {
            count = new Count();
            mCounts.put(song.getSongId(), count);
        }

        return count;
    }

    @Override
    public void setPlayCount(Song song, int count) {
        getOrInitializeCount(song).mPlays = count;
    }

    @Override
    public void setSkipCount(Song song, int count) {
        getOrInitializeCount(song).mSkips = count;
    }

    @Override
    public void setPlayDate(Song song, long time) {
        getOrInitializeCount(song).mDate = time;
    }

    private static class Count {

        int mPlays;
        int mSkips;
        long mDate;

    }
}
