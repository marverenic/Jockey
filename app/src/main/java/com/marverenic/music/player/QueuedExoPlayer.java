package com.marverenic.music.player;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioTrack;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.marverenic.music.BuildConfig;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.Internal;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class QueuedExoPlayer implements QueuedMediaPlayer {

    private static final String USER_AGENT = "Jockey/" + BuildConfig.VERSION_NAME;

    private Context mContext;
    private EqualizedExoPlayer mExoPlayer;
    private ExoPlayerState mState;

    private boolean mRepeatAll;
    private boolean mRepeatOne;

    @Nullable
    private PlaybackEventListener mEventListener;

    private boolean mInvalid;
    private List<Song> mQueue;
    private int mQueueIndex;

    static {
        AudioTrack.enablePreV21AudioSessionWorkaround = true;
    }

    public QueuedExoPlayer(Context context) {
        mContext = context;
        mState = ExoPlayerState.IDLE;

        TrackSelector trackSelector = new DefaultTrackSelector(new Handler());
        LoadControl loadControl = new DefaultLoadControl();
        SimpleExoPlayer baseInstance = ExoPlayerFactory.newSimpleInstance(mContext,
                trackSelector, loadControl);
        mExoPlayer = new EqualizedExoPlayer(context, baseInstance);


        mExoPlayer.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
                Timber.i("onLoadingChanged (%b)", isLoading);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Timber.i("onPlayerStateChanged");
                QueuedExoPlayer.this.onPlayerStateChanged(playbackState);
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                Timber.i("onTimelineChanged");
                QueuedExoPlayer.this.onTimelineChanged();
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Timber.i("onPlayerError");
                QueuedExoPlayer.this.onPlayerError(error);
            }

            @Override
            public void onPositionDiscontinuity() {
                Timber.i("onPositionDiscontinuity");
                QueuedExoPlayer.this.onPositionDiscontinuity();
            }
        });
    }

    @Internal void onPlayerStateChanged(int playbackState) {
        boolean stateDiff = mState != ExoPlayerState.fromInt(playbackState);
        mState = ExoPlayerState.fromInt(playbackState);

        if (stateDiff && playbackState == ExoPlayer.STATE_ENDED) {
            onCompletion();
            pause();
            setQueueIndex(0);
        }
    }

    private void onCompletion() {
        Song completed = getNowPlaying();

        if (mInvalid) {
            boolean ended = false;
            if (!mRepeatOne) {
                if (mRepeatAll && mQueueIndex == mQueue.size() - 1) {
                    mQueueIndex = 0;
                } else if (mQueueIndex < mQueue.size() - 1) {
                    mQueueIndex++;
                } else {
                    mQueueIndex = 0;
                    ended = true;
                }
            }

            prepare(!ended, true);

            if (!ended && mEventListener != null) {
                mEventListener.onCompletion(completed);
            }
        } else if (mEventListener != null) {
            mEventListener.onCompletion(completed);
        }
    }

    private void onStart() {
        if (mEventListener != null) {
            mEventListener.onSongStart();
        }
    }

    @Internal void onTimelineChanged() {
        onStart();
    }

    @Internal void onPositionDiscontinuity() {
        int currentQueueIndex = mExoPlayer.getCurrentWindowIndex() % mQueue.size();
        boolean invalid = mInvalid;
        if (mQueueIndex != currentQueueIndex || invalid) {
            onCompletion();
            if (!mRepeatOne && !invalid) {
                mQueueIndex = currentQueueIndex;
                onStart();
            }
        }
    }

    @Internal void onPlayerError(ExoPlaybackException error) {
        if (mEventListener != null) {
            mEventListener.onError(error);
        }
    }

    @Override
    public void setPlaybackEventListener(@Nullable PlaybackEventListener listener) {
        mEventListener = listener;
    }

    @Override
    public Song getNowPlaying() {
        if (mQueue == null || mQueue.isEmpty()) {
            return null;
        }
        return mQueue.get(mQueueIndex);
    }

    @Override
    public List<Song> getQueue() {
        return mQueue;
    }

    @Override
    public int getQueueSize() {
        return mQueue.size();
    }

    @Override
    public void setQueue(@NonNull List<Song> queue, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }

        if (!queue.isEmpty() && index >= queue.size()) {
            throw new IllegalArgumentException("index must be smaller than the queue size ("
                    + queue.size() + ")");
        }

        if (queue.isEmpty()) {
            reset();
        } else {
            boolean nowPlayingDiff = !queue.get(index).equals(getNowPlaying());
            boolean queueDiff = !queue.equals(mQueue);

            mQueue = queue;
            mQueueIndex = index;

            if (nowPlayingDiff) {
                prepare(isPlaying(), true);
            } else {
                mInvalid |= queueDiff;
            }
        }
    }

    @Override
    public void setQueueIndex(int index) {
        if (index == mQueueIndex) {
            seekTo(0);
        } else {
            mQueueIndex = index;
            if (mRepeatOne || mInvalid) {
                prepare(true, true);
            } else {
                mExoPlayer.seekTo(index, 0);
            }
        }
    }

    @Override
    public int getQueueIndex() {
        return mQueueIndex;
    }

    private void prepare(boolean playWhenReady, boolean resetPosition) {
        mInvalid = false;

        if (mQueue == null) {
            return;
        }

        DataSource.Factory srcFactory = new DefaultDataSourceFactory(mContext, USER_AGENT);
        ExtractorsFactory extFactory = new DefaultExtractorsFactory();

        int startingPosition = resetPosition ? 0 : getCurrentPosition();

        if (mRepeatOne) {
            mExoPlayer.prepare(buildRepeatOneMediaSource(srcFactory, extFactory));
        } else if (mRepeatAll) {
            mExoPlayer.prepare(buildRepeatAllMediaSource(srcFactory, extFactory));
        } else {
            mExoPlayer.prepare(buildNoRepeatMediaSource(srcFactory, extFactory));
        }

        mExoPlayer.seekTo(mQueueIndex, startingPosition);
        mExoPlayer.setPlayWhenReady(playWhenReady);
    }

    private MediaSource buildRepeatOneMediaSource(DataSource.Factory srcFactory,
                                                  ExtractorsFactory extFactory) {

        Uri uri = mQueue.get(mQueueIndex).getLocation();
        MediaSource source = new ExtractorMediaSource(uri, srcFactory, extFactory, null, null);
        return new LoopingMediaSource(source);
    }

    private MediaSource buildNoRepeatMediaSource(DataSource.Factory srcFactory,
                                                 ExtractorsFactory extFactory) {

        MediaSource[] queue = new MediaSource[mQueue.size()];

        for (int i = 0; i < queue.length; i++) {
            Uri uri = mQueue.get(i).getLocation();
            queue[i] = new ExtractorMediaSource(uri, srcFactory, extFactory, null, null);
        }

        return new ConcatenatingMediaSource(queue);
    }

    private MediaSource buildRepeatAllMediaSource(DataSource.Factory sourceFactory,
                                                  ExtractorsFactory extractorsFactory) {

        MediaSource queue = buildNoRepeatMediaSource(sourceFactory, extractorsFactory);
        return new LoopingMediaSource(queue);
    }

    @Override
    public void skip() {
        mQueueIndex++;
        mQueueIndex %= mQueue.size();

        if (mRepeatAll) {
            prepare(true, true);
        } else {
            mExoPlayer.seekTo(mQueueIndex, 0);
            mExoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void skipPrevious() {
        mQueueIndex--;
        mQueueIndex %= mQueue.size();
        if (mQueueIndex < 0) {
            mQueueIndex += mQueue.size();
        }

        if (mRepeatAll) {
            prepare(true, true);
        } else {
            mExoPlayer.seekTo(mQueueIndex, 0);
            mExoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void seekTo(int mSec) {
        if (mInvalid) {
            prepare(isPlaying(), false);
        }
        mExoPlayer.seekTo(mQueueIndex, mSec);
    }

    @Override
    public void stop() {
        mExoPlayer.stop();
        mExoPlayer.seekToDefaultPosition();
    }

    @Override
    public void play() {
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
        if (mInvalid) {
            prepare(false, false);
        }
    }

    @Override
    public int getCurrentPosition() {
        return (int) Math.min(mExoPlayer.getCurrentPosition(), getDuration());
    }

    @Override
    public int getDuration() {
        return (int) mExoPlayer.getDuration();
    }

    @Override
    public PlayerState getState() {
        return mState;
    }

    @Override
    public boolean isComplete() {
        return mState == ExoPlayerState.ENDED;
    }

    @Override
    public boolean isPaused() {
        return !mExoPlayer.getPlayWhenReady();
    }

    @Override
    public boolean isStopped() {
        return mState == ExoPlayerState.IDLE;
    }

    @Override
    public void setVolume(float volume) {
        mExoPlayer.setVolume(volume);
    }

    @Override
    public void setEqualizer(boolean enabled, Equalizer.Settings settings) {
        mExoPlayer.setEqualizerSettings(enabled, settings);
    }

    @Override
    public void enableRepeatAll() {
        if (!mRepeatAll) {
            mRepeatAll = true;
            mRepeatOne = false;
            mInvalid = true;
        }
    }

    @Override
    public void enableRepeatOne() {
        if (!mRepeatOne) {
            mRepeatOne = true;
            mRepeatAll = false;
            mInvalid = true;
        }
    }

    @Override
    public void enableRepeatNone() {
        if (mRepeatAll || mRepeatOne) {
            mRepeatOne = false;
            mRepeatAll = false;
            mInvalid = true;
        }
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayer.getPlayWhenReady();
    }

    @Override
    public void reset() {
        mQueue = Collections.emptyList();
        mQueueIndex = 0;
        prepare(false, true);
    }

    @Override
    public void release() {
        mExoPlayer.release();
        mExoPlayer = null;
        mContext = null;
    }
}
