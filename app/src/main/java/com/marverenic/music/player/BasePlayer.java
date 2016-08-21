package com.marverenic.music.player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class BasePlayer implements Player {

    private Set<AudioEffectController<?>> mEffects;

    private Set<OnPreparedListener> mPreparedListeners;
    private Set<OnErrorListener> mErrorListeners;
    private Set<OnCompletionListener> mCompletionListeners;
    private Set<OnAudioSessionIdChangeListener> mAudioSessionIdListeners;

    public BasePlayer() {
        mPreparedListeners = new HashSet<>();
        mErrorListeners = new HashSet<>();
        mCompletionListeners = new HashSet<>();
        mAudioSessionIdListeners = new HashSet<>();
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

    protected void invokeAudioSessionIdListeners() {
        for (OnAudioSessionIdChangeListener listener : mAudioSessionIdListeners) {
            listener.onAudioSessionIdChanged(getAudioSessionId());
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
    public void addAudioSessionIdListener(OnAudioSessionIdChangeListener sessionIdListener) {
        mAudioSessionIdListeners.add(sessionIdListener);
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
    public void removeAudioSessionIdListener(OnAudioSessionIdChangeListener sessionIdListener) {
        mAudioSessionIdListeners.remove(sessionIdListener);
    }

    @Override
    public void setAudioEffects(AudioEffectController<?>[] effects) {
        if (mEffects != null) {
            for (AudioEffectController<?> effect : mEffects) {
                effect.release();
            }
        }

        mEffects = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(effects)));

        for (AudioEffectController<?> effect : mEffects) {
            effect.setPlayer(this);
        }
    }

    @Override
    public void release() {
        for (AudioEffectController<?> effect : mEffects) {
            effect.release();
        }
    }
}
