package com.marverenic.music.player;

import android.content.Context;

import java.io.IOException;

public class DeferredMediaPlayer extends BasePlayer {

    private Player mPlayer;

    private boolean mDeferredStart;
    private int mDeferredSeek;

    public DeferredMediaPlayer(Context context) {
        this(new StatefulMediaPlayer(context));
    }

    public DeferredMediaPlayer(Player player) {
        mPlayer = player;

        mPlayer.addOnPreparedListener(this::onPrepared);
        mPlayer.addOnCompletionListener(this::onCompletion);
        mPlayer.addOnErrorListener(this::onError);
    }

    private void onPrepared(Player player) {
        if (mDeferredStart) {
            mPlayer.start();
            mDeferredStart = false;
        }

        mPlayer.seekTo(mDeferredSeek);
        invokePreparedListeners();
    }

    private void onCompletion(Player player) {
        clearDeferredActions();
        invokeCompletionListeners();
    }

    private boolean onError(Player player, Throwable error) {
        clearDeferredActions();
        return invokeErrorListeners(error);
    }

    private void clearDeferredActions() {
        mDeferredStart = false;
        mDeferredSeek = 0;
    }

    @Override
    public void setDataSource(String path) throws IOException {
        mPlayer.setDataSource(path);
        clearDeferredActions();
    }

    @Override
    public void prepare() {
        mPlayer.prepare();
    }

    @Override
    public void reset() {
        mPlayer.reset();
    }

    @Override
    public void setWakeMode(int wakeMode) {
        mPlayer.setWakeMode(wakeMode);
    }

    @Override
    public void setVolume(float volume) {
        mPlayer.setVolume(volume);
    }

    @Override
    public void seekTo(int mSec) {
        if (mPlayer.getPlayerState().canSeek()) {
            mPlayer.seekTo(mSec);
        } else {
            mDeferredSeek = mSec;
        }
    }

    @Override
    public void start() {
        if (mPlayer.getPlayerState().canStart()) {
            mPlayer.start();
        } else {
            mDeferredStart = true;
        }
    }

    @Override
    public void pause() {
        if (mPlayer.getPlayerState().canPause()) {
            mPlayer.pause();
        } else {
            mDeferredStart = false;
        }
    }

    @Override
    public void stop() {
        if (mPlayer.getPlayerState().canStop()) {
            mPlayer.stop();
        } else {
            mDeferredStart = false;
        }
    }

    @Override
    public int getAudioSessionId() {
        return mPlayer.getAudioSessionId();
    }

    @Override
    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public PlayerState getPlayerState() {
        return mPlayer.getPlayerState();
    }

    @Override
    public boolean isComplete() {
        return mPlayer.isComplete();
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public boolean isPaused() {
        return mPlayer.isPaused();
    }

    @Override
    public boolean isStopped() {
        return mPlayer.isStopped();
    }

    @Override
    public boolean isPrepared() {
        return mPlayer.isPrepared();
    }

    @Override
    public boolean isPreparing() {
        return mPlayer.isPreparing();
    }

    @Override
    public void release() {
        mPlayer.release();
    }
}
