package com.marverenic.music.player.persistence;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlaybackPersistenceManager {

    private static final String QUEUE = "queue";
    private static final String SHUFFLED_QUEUE = "shuffled_queue";

    private static final String SEEK_POSITION = "seek_position";
    private static final String QUEUE_INDEX = "queue_position";

    private PlaybackStateDatabase mDatabase;

    public PlaybackPersistenceManager(PlaybackStateDatabase database) {
        mDatabase = database;
    }

    public boolean hasState() {
        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();
        return dao.getMetadataItemCount() > 0 && dao.getPlaybackItemCount() > 0;
    }

    public void setState(State state) {
        mDatabase.runInTransaction(() -> {
            PlaybackItemDao dao = mDatabase.getPlaybackItemDao();

            dao.clearPlaybackItems(QUEUE);
            dao.clearPlaybackItems(SHUFFLED_QUEUE);

            dao.setPlaybackItems(convertUrisToDbRow(QUEUE, state.getQueue()));
            dao.setPlaybackItems(convertUrisToDbRow(SHUFFLED_QUEUE, state.getShuffledQueue()));

            dao.putMetadataItem(new PlaybackMetadataItem(SEEK_POSITION, state.getSeekPosition()));
            dao.putMetadataItem(new PlaybackMetadataItem(QUEUE_INDEX, state.getQueuePosition()));
        });
    }

    public void setPosition(int seekPosition, int queueIndex) {
        mDatabase.runInTransaction(() -> {
            PlaybackItemDao dao = mDatabase.getPlaybackItemDao();
            dao.putMetadataItem(new PlaybackMetadataItem(SEEK_POSITION, seekPosition));
            dao.putMetadataItem(new PlaybackMetadataItem(QUEUE_INDEX, queueIndex));
        });
    }

    public Observable<State> getState() {
        return Observable.fromCallable(this::getStateBlocking)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public List<Song> getQueue(Context context, boolean shuffle) {
        if (!hasState()) {
            return Collections.emptyList();
        }

        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();
        List<PlaybackItem> items = dao.getPlaybackItems(shuffle ? SHUFFLED_QUEUE : QUEUE);
        return MediaStoreUtil.buildSongListFromUris(convertDbRowsToUri(items), context);
    }

    public int getQueueIndex() {
        if (!hasState()) {
            return 0;
        }
        return (int) mDatabase.getPlaybackItemDao().getMetadataItem(QUEUE_INDEX).value;
    }

    @Nullable
    public Song getNowPlaying(Context context, boolean shuffle) {
        if (!hasState()) {
            return null;
        }

        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();
        PlaybackItem item = dao.getPlaybackItemAtIndex(
                (shuffle) ? SHUFFLED_QUEUE : QUEUE,
                getQueueIndex()
        );
        return MediaStoreUtil.buildSongFromUri(Uri.parse(item.songUri), context);
    }

    public int getSeekPosition() {
        if (!hasState()) {
            return 0;
        } else {
            return (int) mDatabase.getPlaybackItemDao().getMetadataItem(SEEK_POSITION).value;
        }
    }

    public State getStateBlocking() {
        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();

        long seekPosition;
        int queuePosition;
        List<Uri> queue;
        List<Uri> shuffledQueue;

        if (hasState()) {
            seekPosition = dao.getMetadataItem(SEEK_POSITION).value;
            queuePosition = (int) dao.getMetadataItem(QUEUE_INDEX).value;

            queue = convertDbRowsToUri(dao.getPlaybackItems(QUEUE));
            shuffledQueue = convertDbRowsToUri(dao.getPlaybackItems(SHUFFLED_QUEUE));
        } else {
            seekPosition = 0;
            queuePosition = 0;
            queue = Collections.emptyList();
            shuffledQueue = Collections.emptyList();
        }

        return new State(seekPosition, queuePosition, queue, shuffledQueue);
    }

    private List<PlaybackItem> convertUrisToDbRow(String listName, List<Uri> uris) {
        List<PlaybackItem> playbackItems = new ArrayList<>();
        for (int i = 0; i < uris.size(); i++) {
            playbackItems.add(new PlaybackItem(listName, i, uris.get(i).toString()));
        }
        return playbackItems;
    }

    private List<Uri> convertDbRowsToUri(List<PlaybackItem> rows) {
        List<Uri> uris = new ArrayList<>();
        for (PlaybackItem row : rows) {
            uris.add(Uri.parse(row.songUri));
        }
        return uris;
    }

    public static class State {

        private long mSeekPosition;
        private int mQueuePosition;
        private List<Uri> mQueue;
        private List<Uri> mShuffledQueue;

        public State(long seekPosition, int queuePosition,
                     List<Uri> queue, List<Uri> shuffledQueue) {
            mSeekPosition = seekPosition;
            mQueuePosition = queuePosition;
            mQueue = queue;
            mShuffledQueue = shuffledQueue;
        }

        public long getSeekPosition() {
            return mSeekPosition;
        }

        public int getQueuePosition() {
            return mQueuePosition;
        }

        public List<Uri> getQueue() {
            return mQueue;
        }

        public List<Uri> getShuffledQueue() {
            return mShuffledQueue;
        }
    }

}
