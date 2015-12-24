package com.marverenic.music.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class ManagedMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    public enum status {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED
    }

    private static final String TAG = "ManagedMediaPlayer";
    private status state;
    private boolean effectivelyComplete;
    private OnPreparedListener onPreparedListener;
    private OnCompletionListener onCompletionListener;
    private OnErrorListener onErrorListener;

    public ManagedMediaPlayer() {
        super();
        super.setOnCompletionListener(this);
        super.setOnPreparedListener(this);
        super.setOnErrorListener(this);
        state = status.IDLE;
    }

    @Override
    public void reset() {
        super.reset();
        state = status.IDLE;
        effectivelyComplete = false;
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException {
        if (state == status.IDLE) {
            super.setDataSource(context, uri);
            state = status.INITIALIZED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to set data source, but media player was in state " + state);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (state == status.IDLE) {
            super.setDataSource(path);
            state = status.INITIALIZED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to set data source, but media player was in state " + state);
        }
    }

    @Override
    public void prepareAsync() {
        if (state == status.INITIALIZED) {
            super.prepareAsync();
            state = status.PREPARING;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to prepare async, but media player was in state " + state);
        }
    }

    @Override
    public void prepare() throws IOException {
        if (state == status.INITIALIZED) {
            super.prepare();
            state = status.PREPARING;
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
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to start, but media player was in state " + state);
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
        if (onErrorListener != null && !onErrorListener.onError(mp, what, extra)) {
            Log.i(TAG, "An error occurred and the player was reset");
            reset();
        }
        return true;
    }

    @Override
    public void seekTo(int mSec) {
        if (state == status.PREPARED || state == status.STARTED || state == status.PAUSED || effectivelyComplete) {
            super.seekTo(mSec);
            effectivelyComplete = false;
        } else if (state == status.COMPLETED) {
            start();
            pause();
            super.seekTo(mSec);
        } else {
            Log.i(TAG, "Attempted to set seek, but media player was in state " + state);
        }
    }

    @Override
    public void stop() {
        if (state == status.STARTED || state == status.PAUSED || state == status.COMPLETED) {
            super.stop();
            state = status.STOPPED;
            effectivelyComplete = false;
        } else {
            Log.i(TAG, "Attempted to stop, but media player was in state " + state);
        }
    }

    public void complete() {
        if (state == status.PREPARED || state == status.STARTED || state == status.PAUSED) {
            effectivelyComplete = true;
            if (state == status.STARTED) {
                pause();
            }
            seekTo(getDuration());
        }
    }

    @Override
    public void pause() {
        if (state == status.STARTED || state == status.PAUSED) {
            super.pause();
            state = status.PAUSED;
        } else {
            Log.i(TAG, "Attempted to pause, but media player was in state " + state);
        }
    }

    @Override
    public int getCurrentPosition(){
        if (state == status.COMPLETED || effectivelyComplete) {
            return getDuration();
        } else if (state == status.PREPARED || state == status.STARTED || state == status.PAUSED) {
            return super.getCurrentPosition();
        }
        else return 0;
    }

    @Override
    public int getDuration(){
        if (state != status.IDLE && state != status.INITIALIZED) {
            return super.getDuration();
        }
        else return 1;
    }

    public status getState() {
        return state;
    }

    public boolean isComplete() {
        return effectivelyComplete || state == status.COMPLETED;
    }
}
