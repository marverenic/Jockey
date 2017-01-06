package com.marverenic.music.player;

import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayerState {

    private final boolean mIsPlaying;
    private final List<Song> mQueue;
    private final int mQueuePosition;
    private final int mSeekPosition;

    private PlayerState(boolean isPlaying, List<Song> queue, int queuePosition, int seekPosition) {
        mIsPlaying = isPlaying;
        mQueue = queue;
        mQueuePosition = queuePosition;
        mSeekPosition = seekPosition;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public List<Song> getQueue() {
        return mQueue;
    }

    public int getQueuePosition() {
        return mQueuePosition;
    }

    public int getSeekPosition() {
        return mSeekPosition;
    }

    public static final class Builder {

        private boolean mIsPlaying;
        private List<Song> mQueue;
        private int mQueuePosition;
        private int mSeekPosition;

        public Builder setPlaying(boolean isPlaying) {
            mIsPlaying = isPlaying;
            return this;
        }

        public Builder setQueue(List<Song> queue) {
            // TODO save the shuffled and unshuffled queue separately
            mQueue = Collections.unmodifiableList(new ArrayList<>(queue));
            return this;
        }

        public Builder setQueuePosition(int queuePosition) {
            mQueuePosition = queuePosition;
            return this;
        }

        public Builder setSeekPosition(int seekPosition) {
            mSeekPosition = seekPosition;
            return this;
        }

        public PlayerState build() {
            return new PlayerState(mIsPlaying, mQueue, mQueuePosition, mSeekPosition);
        }

    }

}
