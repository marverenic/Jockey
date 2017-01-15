package com.marverenic.music.player;

import android.os.Parcel;
import android.os.Parcelable;

import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayerState implements Parcelable {

    private final boolean mIsPlaying;
    private final List<Song> mQueue;
    private final List<Song> mShuffledQueue;
    private final int mQueuePosition;
    private final int mSeekPosition;

    private PlayerState(boolean isPlaying, List<Song> queue, List<Song> shuffledQueue,
                        int queuePosition, int seekPosition) {
        mIsPlaying = isPlaying;
        mQueue = queue;
        mShuffledQueue = shuffledQueue;
        mQueuePosition = queuePosition;
        mSeekPosition = seekPosition;
    }

    protected PlayerState(Parcel in) {
        mIsPlaying = in.readByte() != 0;
        mQueue = in.createTypedArrayList(Song.CREATOR);
        mShuffledQueue = in.createTypedArrayList(Song.CREATOR);
        mQueuePosition = in.readInt();
        mSeekPosition = in.readInt();
    }

    public static final Creator<PlayerState> CREATOR = new Creator<PlayerState>() {
        @Override
        public PlayerState createFromParcel(Parcel in) {
            return new PlayerState(in);
        }

        @Override
        public PlayerState[] newArray(int size) {
            return new PlayerState[size];
        }
    };

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public List<Song> getQueue() {
        return mQueue;
    }

    public List<Song> getShuffledQueue() {
        return mShuffledQueue;
    }

    public int getQueuePosition() {
        return mQueuePosition;
    }

    public int getSeekPosition() {
        return mSeekPosition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mIsPlaying ? 1 : 0));
        dest.writeTypedList(mQueue);
        dest.writeTypedList(mShuffledQueue);
        dest.writeInt(mQueuePosition);
        dest.writeInt(mSeekPosition);
    }

    public static final class Builder {

        private boolean mIsPlaying;
        private List<Song> mQueue;
        private List<Song> mShuffledQueue;
        private int mQueuePosition;
        private int mSeekPosition;

        public Builder setPlaying(boolean isPlaying) {
            mIsPlaying = isPlaying;
            return this;
        }

        public Builder setQueue(List<Song> queue) {
            mQueue = Collections.unmodifiableList(new ArrayList<>(queue));
            return this;
        }

        public Builder setShuffledQueue(List<Song> shuffledQueue) {
            mShuffledQueue = Collections.unmodifiableList(new ArrayList<>(shuffledQueue));
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
            return new PlayerState(mIsPlaying, mQueue, mShuffledQueue,
                    mQueuePosition, mSeekPosition);
        }

    }

}
