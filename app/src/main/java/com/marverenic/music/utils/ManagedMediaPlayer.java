package com.marverenic.music.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class ManagedMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public enum Status {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED
    }

    private static final String TAG = "ManagedMediaPlayer";
    private Status state;
    private boolean effectivelyComplete;
    private OnPreparedListener onPreparedListener;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;

    public ManagedMediaPlayer() {
        super();
        super.setOnCompletionListener(this);
        super.setOnPreparedListener(this);
        super.setOnErrorListener(this);
        state = Status.IDLE;
    }

    @Override
    public void reset() {
        super.reset();
        state = Status.IDLE;
        effectivelyComplete = false;
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException {
        if (state == Status.IDLE) {
            super.setDataSource(context, uri);
            state = Status.INITIALIZED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to set data source, but media player was in state " + state);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (state == Status.IDLE) {
            super.setDataSource(path);
            state = Status.INITIALIZED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to set data source, but media player was in state " + state);
        }
    }

    @Override
    public void prepareAsync() {
        if (state == Status.INITIALIZED) {
            super.prepareAsync();
            state = Status.PREPARING;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to prepare async, but media player was in state " + state);
        }
    }

    @Override
    public void prepare() throws IOException {
        if (state == Status.INITIALIZED) {
            super.prepare();
            state = Status.PREPARING;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to prepare, but media player was in state " + state);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        state = Status.PREPARED;
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared(mp);
        }
    }

    @Override
    public void start() {
        if (state == Status.PREPARED || state == Status.STARTED || state == Status.PAUSED
                || state == Status.COMPLETED) {
            if (effectivelyComplete) {
                seekTo(0);
            }
            super.start();
            state = Status.STARTED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to start, but media player was in state " + state);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        state = Status.COMPLETED;
        if (onCompletionListener != null) {
            onCompletionListener.onCompletion(mp);
        }
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (onErrorListener != null && !onErrorListener.onError(mp, what, extra)) {
            Log.i(TAG, "An error occurred and the player was reset");
            reset();
        }
        return true;
    }

    @Override
    public void seekTo(int mSec) {
        if (state == Status.PREPARED || state == Status.STARTED || state == Status.PAUSED) {
            super.seekTo(mSec);
            effectivelyComplete = false;
        } else if (state == Status.COMPLETED) {
            start();
            pause();
            super.seekTo(mSec);
        } else {
            Log.i(TAG, "Attempted to set seek, but media player was in state " + state);
        }
    }

    @Override
    public void stop() {
        if (state == Status.STARTED || state == Status.PAUSED || state == Status.COMPLETED) {
            super.stop();
            state = Status.STOPPED;
        } else {
            Log.i(TAG, "Attempted to stop, but media player was in state " + state);
        }
    }

    @Override
    public void pause() {
        if (state == Status.STARTED || state == Status.PAUSED) {
            super.pause();
            state = Status.PAUSED;
        } else {
            Log.i(TAG, "Attempted to pause, but media player was in state " + state);
        }
    }

    public void complete() {
        pause();
        effectivelyComplete = true;
    }

    @Override
    public int getCurrentPosition() {
        if (state == Status.COMPLETED || effectivelyComplete) {
            return getDuration();
        } else if (state == Status.PREPARED || state == Status.STARTED || state == Status.PAUSED) {
            return super.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public int getDuration() {
        if (state != Status.IDLE && state != Status.INITIALIZED) {
            return super.getDuration();
        } else {
            return 0;
        }
    }

    public Status getState() {
        return state;
    }

    public boolean isComplete() {
        return state == Status.COMPLETED || effectivelyComplete;
    }
}
