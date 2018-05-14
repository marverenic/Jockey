package com.marverenic.music.player.persistence;

import android.net.Uri;

import java.util.ArrayList;
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

    public void setState(State state) {
        mDatabase.beginTransaction();

        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();

        dao.clearPlaybackItems(QUEUE);
        dao.clearPlaybackItems(SHUFFLED_QUEUE);

        dao.setPlaybackItems(convertUrisToDbRow(QUEUE, state.getQueue()));
        dao.setPlaybackItems(convertUrisToDbRow(SHUFFLED_QUEUE, state.getShuffledQueue()));

        dao.putMetadataItem(new PlaybackMetadataItem(SEEK_POSITION, state.getSeekPosition()));
        dao.putMetadataItem(new PlaybackMetadataItem(QUEUE_INDEX, state.getQueuePosition()));

        mDatabase.endTransaction();
    }

    public Observable<State> getState() {
        return Observable.fromCallable(this::getStateBlocking)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public State getStateBlocking() {
        PlaybackItemDao dao = mDatabase.getPlaybackItemDao();
        long seekPosition = dao.getMetadataItem(SEEK_POSITION).value;
        int queuePosition = (int) dao.getMetadataItem(QUEUE_INDEX).value;

        List<Uri> queue = convertDbRowsToUri(dao.getPlaybackItems(QUEUE));
        List<Uri> shuffledQueue = convertDbRowsToUri(dao.getPlaybackItems(SHUFFLED_QUEUE));

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
