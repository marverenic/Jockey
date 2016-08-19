package com.marverenic.music.player;

import android.content.Context;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DeferredMediaPlayer implements Player {

    private Player mPlayer;

    private boolean mDeferredStart;
    private int mDeferredSeek;

    private Set<OnPreparedListener> mPreparedListeners;
    private Set<OnErrorListener> mErrorListeners;
    private Set<OnCompletionListener> mCompletionListeners;

    public DeferredMediaPlayer(Context context) {
        this(new StatefulMediaPlayer(context));
    }

    public DeferredMediaPlayer(Player player) {
        mPlayer = player;

        mPreparedListeners = new HashSet<>();
        mErrorListeners = new HashSet<>();
        mCompletionListeners = new HashSet<>();

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

        for (OnPreparedListener preparedListener : mPreparedListeners) {
            preparedListener.onPrepared(this);
        }
    }

    private void onCompletion(Player player) {
        clearDeferredActions();

        for (OnCompletionListener completionListener : mCompletionListeners) {
            completionListener.onCompletion(this);
        }
    }

    private boolean onError(Player player, Throwable error) {
        clearDeferredActions();

        boolean handled = false;

        for (OnErrorListener errorListener : mErrorListeners) {
            handled |= errorListener.onError(this, error);
        }
        return handled;
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
    public void setAudioSessionId(int audioSessionId) {
        mPlayer.setAudioSessionId(audioSessionId);
    }

    @Override
    public void setVolume(float volume) {
        mPlayer.setVolume(volume);
    }

    @Override
    public void addOnPreparedListener(OnPreparedListener onPreparedListener) {
        mPreparedListeners.add(onPreparedListener);
    }

    @Override
    public void addOnErrorListener(OnErrorListener onErrorListener) {
        mErrorListeners.add(onErrorListener);
    }

    @Override
    public void addOnCompletionListener(OnCompletionListener onCompletionListener) {
        mCompletionListeners.add(onCompletionListener);
    }

    @Override
    public void removeOnPreparedListener(OnPreparedListener onPreparedListener) {
        mPreparedListeners.remove(onPreparedListener);
    }

    @Override
    public void removeOnErrorListener(OnErrorListener onErrorListener) {
        mErrorListeners.remove(onErrorListener);
    }

    @Override
    public void removeOnCompletionListener(OnCompletionListener onCompletionListener) {
        mCompletionListeners.remove(onCompletionListener);
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
