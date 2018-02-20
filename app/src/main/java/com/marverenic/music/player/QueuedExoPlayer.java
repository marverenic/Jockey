package com.marverenic.music.player;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.marverenic.music.BuildConfig;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.Internal;

import java.util.ArrayList;
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

    private DataSource.Factory mSourceFactory;
    private ExtractorsFactory mExtractorsFactory;
    private DynamicConcatenatingMediaSource mExoPlayerQueue;

    private boolean mWaitingForDuration = false;
    private boolean mHasError;
    private List<Song> mQueue;
    private int mQueueIndex;
    private int mPrevDuration;

    static {
        DefaultAudioSink.enablePreV21AudioSessionWorkaround = true;
    }

    public QueuedExoPlayer(Context context) {
        mContext = context;
        mState = ExoPlayerState.IDLE;
        mQueue = Collections.emptyList();

        mSourceFactory = new DefaultDataSourceFactory(mContext, USER_AGENT);
        mExtractorsFactory = new DefaultExtractorsFactory();

        RenderersFactory renderersFactory = new DefaultRenderersFactory(mContext);
        TrackSelector trackSelector = new DefaultTrackSelector(new FixedTrackSelection.Factory());
        LoadControl loadControl = new DefaultLoadControl();
        SimpleExoPlayer baseInstance = ExoPlayerFactory.newSimpleInstance(
                renderersFactory, trackSelector, loadControl);
        mExoPlayer = new EqualizedExoPlayer(context, baseInstance);

        mExoPlayer.addListener(new Player.EventListener() {
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
            public void onRepeatModeChanged(int repeatMode) {
                Timber.i("onRepeatModeChanged (%d)", repeatMode);
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                Timber.i("onShuffleModeEnabledChanged (%b)", shuffleModeEnabled);
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                Timber.i("onTimelineChanged");
                dispatchDurationUpdateIfNeeded();
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups,
                                        TrackSelectionArray trackSelections) {
                Timber.i("onTracksChanged");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Timber.i("onPlayerError");
                QueuedExoPlayer.this.onPlayerError(error);
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                Timber.i("onPositionDiscontinuity");
                QueuedExoPlayer.this.onPositionDiscontinuity(reason);
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                Timber.i("onPlaybackParametersChanged");
            }

            @Override
            public void onSeekProcessed() {
                Timber.i("okSeekProcessed");
            }
        });
    }

    @Internal void onPlayerStateChanged(int playbackState) {
        boolean stateDiff = mState != ExoPlayerState.fromInt(playbackState);
        mState = ExoPlayerState.fromInt(playbackState);
        mHasError = mHasError && (mState == ExoPlayerState.IDLE);
        mWaitingForDuration = mExoPlayer.getDuration() == C.TIME_UNSET;

        if (stateDiff && playbackState == Player.STATE_ENDED) {
            onCompletion();
            pause();
            setQueueIndex(0);
        }
    }

    private void onCompletion() {
        Song completed = getNowPlaying();
        if (mEventListener != null && completed != null) {
            mEventListener.onCompletion(completed);
        }
    }

    private void onStart() {
        if (mEventListener != null) {
            mEventListener.onSongStart();
        }
    }

    @Internal void onPositionDiscontinuity(int reason) {
        if (mQueue.size() == 0) {
            return;
        }

        if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
            onCompletion();
        }

        mQueueIndex = mExoPlayer.getCurrentWindowIndex() % mQueue.size();
        onStart();
    }

    @Internal void onPlayerError(ExoPlaybackException error) {
        mHasError = true;

        if (mExoPlayer.getCurrentPosition() >= mExoPlayer.getDuration()
                && mExoPlayer.getDuration() > 0) {
            mQueueIndex = (mQueueIndex + 1) % mQueue.size();
        }

        if (mEventListener != null) {
            mEventListener.onError(error.getCause());
        }
    }

    @Internal void dispatchDurationUpdateIfNeeded() {
        if (mWaitingForDuration && mExoPlayer.getDuration() != C.TIME_UNSET) {
            mWaitingForDuration = false;
            onStart();
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
    public void setQueue(@NonNull List<Song> queue, int index, boolean resetSeekPosition) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }

        if (!queue.isEmpty() && index >= queue.size()) {
            throw new IllegalArgumentException("index must be smaller than the queue size ("
                    + queue.size() + ")");
        }

        if (queue.isEmpty()) {
            reset();
        } else if (mExoPlayerQueue == null || !mQueue.get(mQueueIndex).equals(queue.get(index))
                || resetSeekPosition) {
            mQueue = Collections.unmodifiableList(new ArrayList<>(queue));
            mQueueIndex = index;

            mExoPlayerQueue = new DynamicConcatenatingMediaSource();
            List<MediaSource> mediaSources = new ArrayList<>(queue.size());
            for (Song song : queue) {
                mediaSources.add(buildMediaSource(song));
            }

            mExoPlayerQueue.addMediaSources(mediaSources, () -> {
                mExoPlayer.prepare(mExoPlayerQueue);
                mExoPlayer.seekToDefaultPosition(index);
                onStart();
            });
        } else {
            WaitingCallback waiter = new WaitingCallback();

            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mQueue.size();
                }

                @Override
                public int getNewListSize() {
                    return queue.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    Song oldItem = mQueue.get(oldItemPosition);
                    Song newItem = queue.get(newItemPosition);
                    return oldItem.getLocation().equals(newItem.getLocation());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return areItemsTheSame(oldItemPosition, newItemPosition);
                }
            }).dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    List<MediaSource> mediaSources = new ArrayList<>(count);
                    for (int i = position; i < position + count; i++) {
                        mediaSources.add(buildMediaSource(queue.get(i)));
                    }

                    mExoPlayerQueue.addMediaSources(position, mediaSources, waiter.await());
                }

                @Override
                public void onRemoved(int position, int count) {
                    for (int i = position + count - 1; i >= position; i--) {
                        mExoPlayerQueue.removeMediaSource(i, waiter.await());
                    }
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    mExoPlayerQueue.moveMediaSource(fromPosition, toPosition, waiter.await());
                }

                @Override
                public void onChanged(int position, int count, Object payload) {
                    throw new UnsupportedOperationException("This callback should never occur");
                }
            });

            mQueue = Collections.unmodifiableList(new ArrayList<>(queue));
            mQueueIndex = index;
        }
    }

    @Override
    public void setQueueIndex(int index) {
        if (index == mQueueIndex) {
            seekTo(0);
        } else {
            mExoPlayer.seekTo(index, 0);
        }
    }

    @Override
    public int getQueueIndex() {
        return mQueueIndex;
    }

    private MediaSource buildMediaSource(Song song) {
        return new ExtractorMediaSource.Factory(mSourceFactory)
                .setExtractorsFactory(mExtractorsFactory)
                .createMediaSource(song.getLocation());
    }

    @Override
    public void skip() {
        mQueueIndex++;
        mQueueIndex %= mQueue.size();

        mExoPlayer.seekTo(mQueueIndex, 0);
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void skipPrevious() {
        mQueueIndex--;
        mQueueIndex %= mQueue.size();
        if (mQueueIndex < 0) {
            mQueueIndex += mQueue.size();
        }

        mExoPlayer.seekTo(mQueueIndex, 0);
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void seekTo(int mSec) {
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
    }

    @Override
    public int getCurrentPosition() {
        return (int) mExoPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if (mExoPlayer.getDuration() > 0) {
            mPrevDuration = (int) mExoPlayer.getDuration();
        } else if (mPrevDuration <= 0 && getNowPlaying() != null) {
            return (int) getNowPlaying().getSongDuration();
        }

        return mPrevDuration;
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
    public boolean hasError() {
        return mHasError;
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
            mExoPlayer.setRepeatMode(getExoPlayerRepeatMode());
        }
    }

    @Override
    public void enableRepeatOne() {
        if (!mRepeatOne) {
            mRepeatOne = true;
            mRepeatAll = false;
            mExoPlayer.setRepeatMode(getExoPlayerRepeatMode());
        }
    }

    @Override
    public void enableRepeatNone() {
        if (mRepeatAll || mRepeatOne) {
            mRepeatOne = false;
            mRepeatAll = false;
            mExoPlayer.setRepeatMode(getExoPlayerRepeatMode());
        }
    }

    private int getExoPlayerRepeatMode() {
        if (mRepeatAll) {
            return Player.REPEAT_MODE_ALL;
        } else if (mRepeatOne) {
            return Player.REPEAT_MODE_ONE;
        } else {
            return Player.REPEAT_MODE_OFF;
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

        mExoPlayerQueue = null;
        mExoPlayer.stop();
    }

    @Override
    public void release() {
        mExoPlayer.release();
        mExoPlayer = null;
        mContext = null;
    }

    private static class WaitingCallback {

        private int mInFlightCount = 0;

        @Nullable
        private Runnable mOnComplete;

        public Runnable await() {
            mInFlightCount++;
            return () -> {
                mInFlightCount--;
                if (mOnComplete != null) {
                    mOnComplete.run();
                    mOnComplete = null;
                }
            };
        }

        public void whenComplete(Runnable action) {
            if (mInFlightCount == 0) {
                action.run();
            } else {
                mOnComplete = action;
            }
        }

    }
}
