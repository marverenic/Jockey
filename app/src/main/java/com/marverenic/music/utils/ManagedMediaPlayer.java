package com.marverenic.music.utils;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class ManagedMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public static final byte STATE_IDLE = 0;
    public static final byte STATE_INITIALIZED = 1;
    public static final byte STATE_PREPARING = 2;
    public static final byte STATE_PREPARED = 3;
    public static final byte STATE_STARTED = 4;
    public static final byte STATE_PAUSED = 5;
    public static final byte STATE_STOPPED = 6;
    public static final byte STATE_COMPLETED = 7;

    private static final String TAG = "ManagedMediaPlayer";

    private Context context;
    private byte state;

    private OnPreparedListener onPreparedListener;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;

    public ManagedMediaPlayer(Context context) {
        super();
        super.setOnCompletionListener(this);
        super.setOnPreparedListener(this);
        super.setOnErrorListener(this);
        state = STATE_IDLE;
        this.context = context;
    }

    @Override
    public void reset() {
        super.reset();
        state = STATE_IDLE;
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (state == STATE_IDLE) {
            super.setDataSource(path);
            state = STATE_INITIALIZED;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to set data source, but media player was in state " + state, context);
        }
    }

    @Override
    public void prepareAsync() {
        if (state == STATE_INITIALIZED) {
            super.prepareAsync();
            state = STATE_PREPARING;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to prepare async, but media player was in state " + state, context);
        }
    }

    @Override
    public void prepare() throws IOException {
        if (state == STATE_INITIALIZED) {
            super.prepare();
            state = STATE_PREPARING;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to prepare, but media player was in state " + state, context);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        state = STATE_PREPARED;
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared(mp);
        }
    }

    @Override
    public void start() {
        if (state == STATE_PREPARED || state == STATE_STARTED || state == STATE_PAUSED || state == STATE_COMPLETED) {
            super.start();
            state = STATE_STARTED;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to start, but media player was in state " + state, context);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        state = STATE_COMPLETED;
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
        Debug.log(Debug.ERROR, TAG, "An error occurred and the player was reset", context);
        reset();
        return true;
    }

    @Override
    public void seekTo(int mSec) {
        if (state == STATE_PREPARED || state == STATE_STARTED || state == STATE_PAUSED || state == STATE_COMPLETED) {
            super.seekTo(mSec);
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to set seek, but media player was in state " + state, context);
        }
    }

    @Override
    public void stop() {
        if (state == STATE_STARTED || state == STATE_PAUSED || state == STATE_COMPLETED) {
            super.stop();
            state = STATE_STOPPED;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to stop, but media player was in state " + state, context);
        }
    }

    @Override
    public void pause() {
        if (state == STATE_STARTED || state == STATE_PAUSED) {
            super.pause();
            state = STATE_PAUSED;
        } else {
            Debug.log(Debug.INFO, TAG, "Attempted to pause, but media player was in state " + state, context);
        }
    }

    public byte getState() {
        return state;
    }
}
