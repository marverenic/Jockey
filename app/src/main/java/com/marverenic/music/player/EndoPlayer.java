package com.marverenic.music.player;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.PlayerControl;
import com.marverenic.music.BuildConfig;

import java.io.IOException;

/**
 * A wrapper class for ExoPlayer that implements the Player interface
 */
public class EndoPlayer extends BasePlayer {

    private static final int RENDERER_COUNT = 1;
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024; // 64KB
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final int BUFFER_SIZE = BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT;

    private static final String USER_AGENT = "Jockey/" + BuildConfig.VERSION_NAME;

    private Context mContext;
    private PlayerControl mPlayerControl;
    private ExoPlayer mExoPlayer;
    private Allocator mAllocator;
    private TrackRenderer mRenderer;

    private ExoPlayerState mState;
    private float mVolume;
    private int mAudioSessionId;

    public EndoPlayer(Context context) {
        mContext = context;

        mState = ExoPlayerState.IDLE;
        mVolume = 1f;
        mAudioSessionId = AudioTrack.SESSION_ID_NOT_SET;

        mExoPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT);
        mPlayerControl = new PlayerControl(mExoPlayer);
        mAllocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        mExoPlayer.addListener(new ExoPlayer.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                EndoPlayer.this.onPlayerStateChanged(playWhenReady, playbackState);
            }

            @Override
            public void onPlayWhenReadyCommitted() {
                EndoPlayer.this.onPlayWhenReadyCommitted();
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                invokeErrorListeners(error);
            }
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

    private void onPlayWhenReadyCommitted() {
        // Do nothing.
    }

    @Override
    public void setDataSource(String path) throws IOException {
        Uri uri = Uri.parse(path);
        mRenderer = getRendererForDataSource(uri);
    }

    private SampleSource getSampleSourceForDataSource(Uri uri) {
        DataSource dataSource = new DefaultUriDataSource(mContext, USER_AGENT);
        return new ExtractorSampleSource(uri, dataSource, mAllocator, BUFFER_SIZE);
    }

    private TrackRenderer getRendererForDataSource(Uri uri) {
        SampleSource sampleSource = getSampleSourceForDataSource(uri);
        return new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT) {
            @Override
            protected void onAudioSessionId(int audioSessionId) {
                mAudioSessionId = audioSessionId;
                invokeAudioSessionIdListeners();
            }
        };
    }

    @Override
    public void prepare() {
        mAudioSessionId = AudioTrack.SESSION_ID_NOT_SET;
        mExoPlayer.seekTo(0);
        mExoPlayer.setPlayWhenReady(false);
        mExoPlayer.prepare(mRenderer);
        applyVolume();
    }

    @Override
    public void reset() {
        // TODO
    }

    @Override
    public void setWakeMode(int wakeMode) {
        // TODO
    }

    @Override
    public void setVolume(float volume) {
        mVolume = volume;
        applyVolume();
    }

    private void applyVolume() {
        mExoPlayer.sendMessage(mRenderer, MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, mVolume);
    }

    @Override
    public void seekTo(int mSec) {
        mPlayerControl.seekTo(mSec);
    }

    @Override
    public void start() {
        mPlayerControl.start();
    }

    @Override
    public void pause() {
        mPlayerControl.pause();
    }

    @Override
    public void stop() {
        mExoPlayer.seekTo(0);
        mExoPlayer.stop();
    }

    @Override
    public int getAudioSessionId() {
        return mAudioSessionId;
    }

    @Override
    public int getCurrentPosition() {
        return mPlayerControl.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mPlayerControl.getDuration();
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
        return mPlayerControl.isPlaying();
    }

    @Override
    public boolean isPaused() {
        return !mPlayerControl.isPlaying();
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
        return mState == ExoPlayerState.PREPARING;
    }

    @Override
    public void release() {
        mExoPlayer.release();
        mPlayerControl = null;
        mExoPlayer = null;
        mContext = null;
    }
}
