package com.marverenic.music.player;

import java.util.HashSet;
import java.util.Set;

public abstract class BasePlayer implements Player {

    private Set<OnPreparedListener> mPreparedListeners;
    private Set<OnErrorListener> mErrorListeners;
    private Set<OnCompletionListener> mCompletionListeners;

    public BasePlayer() {
        mPreparedListeners = new HashSet<>();
        mErrorListeners = new HashSet<>();
        mCompletionListeners = new HashSet<>();
    }

    protected void invokePreparedListeners() {
        for (OnPreparedListener listener : mPreparedListeners) {
            listener.onPrepared(this);
        }
    }

    protected boolean invokeErrorListeners(Throwable error) {
        boolean handled = false;

        for (OnErrorListener listener : mErrorListeners) {
            handled |= listener.onError(this, error);
        }

        return handled;
    }

    protected void invokeCompletionListeners() {
        for (OnCompletionListener listener : mCompletionListeners) {
            listener.onCompletion(this);
        }
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
}
