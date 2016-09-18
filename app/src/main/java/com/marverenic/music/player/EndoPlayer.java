package com.marverenic.music.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioTrack;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

/**
 * A wrapper class for ExoPlayer that implements the Player interface
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class EndoPlayer extends BasePlayer {

    private Context mContext;
    private SimpleExoPlayer mExoPlayer;
    private ExoPlayerState mState;
    private Uri mDataSource;

    public EndoPlayer(Context context) {
        mContext = context;

        mState = ExoPlayerState.IDLE;
        AudioTrack.enablePreV21AudioSessionWorkaround = true;

        TrackSelector trackSelector = new DefaultTrackSelector(new Handler());
        LoadControl loadControl = new DefaultLoadControl();
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector, loadControl);

        mExoPlayer.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {}

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                EndoPlayer.this.onPlayerStateChanged(playWhenReady, playbackState);
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {}

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                // TODO delegate this
            }

            @Override
            public void onPositionDiscontinuity() {}
        });
    }

    private void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        ExoPlayerState nextState = ExoPlayerState.fromInt(playbackState);
        if (nextState == mState) {
            return;
        }

        mState = nextState;

        if (mState == ExoPlayerState.ENDED) {
            invokeCompletionListeners();
        } else if (mState == ExoPlayerState.READY) {
            invokePreparedListeners();
        }
    }

    @Override
    public void setDataSource(String path) {
        mDataSource = Uri.parse(path);
    }

    @Override
    public void prepare() {
        DataSource.Factory dataSourceFactory = new FileDataSourceFactory();
        MediaSource source = new ExtractorMediaSource(mDataSource, dataSourceFactory,
                new DefaultExtractorsFactory(), null, null);

        mExoPlayer.prepare(source, true);
    }

    @Override
    public void reset() {
        pause();
        mExoPlayer.stop();
        mDataSource = null;
    }

    @Override
    public void setWakeMode(int wakeMode) {
        // TODO
    }

    @Override
    public void setVolume(float volume) {
        mExoPlayer.setVolume(volume);
    }

    @Override
    public void seekTo(int mSec) {
        mExoPlayer.seekTo(mSec);
    }

    @Override
    public void start() {
        mExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void stop() {
        mExoPlayer.seekTo(0);
        mExoPlayer.stop();
    }

    @Override
    public int getAudioSessionId() {
        return mExoPlayer.getAudioSessionId();
    }

    @Override
    public int getCurrentPosition() {
        return (int) mExoPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return (int) mExoPlayer.getDuration();
    }

    @Override
    public PlayerState getPlayerState() {
        return mState;
    }

    @Override
    public boolean isComplete() {
        return mState == ExoPlayerState.ENDED;
    }

    @Override
    public boolean isPlaying() {
        return mExoPlayer.getPlayWhenReady();
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
    public boolean isPrepared() {
        return mState == ExoPlayerState.READY;
    }

    @Override
    public boolean isPreparing() {
        return mState == ExoPlayerState.BUFFERING;
    }

    @Override
    public void release() {
        mExoPlayer.release();
        mExoPlayer = null;
        mContext = null;
    }
}
