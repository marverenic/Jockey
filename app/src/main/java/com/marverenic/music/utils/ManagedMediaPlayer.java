package com.marverenic.music.utils;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class ManagedMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static enum status {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED
    }

    private static final String TAG = "ManagedMediaPlayer";
    private Context context;
    private status state;
    private OnPreparedListener onPreparedListener;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;

    public ManagedMediaPlayer(Context context) {
        super();
        super.setOnCompletionListener(this);
        super.setOnPreparedListener(this);
        super.setOnErrorListener(this);
        state = status.IDLE;
        this.context = context;
    }

    @Override
    public boolean isPlaying (){
        return state == status.PREPARING || super.isPlaying();
    }

    @Override
    public void reset() {
        super.reset();
        state = status.IDLE;
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (state == status.IDLE) {
            super.setDataSource(path);
            state = status.INITIALIZED;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to set data source, but media player was in state " + state, context);
        }
    }

    @Override
    public void prepareAsync() {
        if (state == status.INITIALIZED) {
            super.prepareAsync();
            state = status.PREPARING;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to prepare async, but media player was in state " + state, context);
        }
    }

    @Override
    public void prepare() throws IOException {
        if (state == status.INITIALIZED) {
            super.prepare();
            state = status.PREPARING;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to prepare, but media player was in state " + state, context);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        state = status.PREPARED;
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared(mp);
        }
    }

    @Override
    public void start() {
        if (state == status.PREPARED || state == status.STARTED || state == status.PAUSED || state == status.COMPLETED) {
            super.start();
            state = status.STARTED;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to start, but media player was in state " + state, context);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        state = status.COMPLETED;
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
        if (onErrorListener != null && onErrorListener.onError(mp, what, extra)) {
            return true;
        }
        Debug.log(Debug.LogLevel.ERROR, TAG, "An error occurred and the player was reset", context);
        reset();
        return true;
    }

    @Override
    public void seekTo(int mSec) {
        if (state == status.PREPARED || state == status.STARTED || state == status.PAUSED || state == status.COMPLETED) {
            super.seekTo(mSec);
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to set seek, but media player was in state " + state, context);
        }
    }

    @Override
    public void stop() {
        if (state == status.STARTED || state == status.PAUSED || state == status.COMPLETED) {
            super.stop();
            state = status.STOPPED;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to stop, but media player was in state " + state, context);
        }
    }

    @Override
    public void pause() {
        if (state == status.STARTED || state == status.PAUSED) {
            super.pause();
            state = status.PAUSED;
        } else {
            Debug.log(Debug.LogLevel.INFO, TAG, "Attempted to pause, but media player was in state " + state, context);
        }
    }

    public status getState() {
        return state;
    }
}
