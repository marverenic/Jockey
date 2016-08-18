package com.marverenic.music.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class StatefulMediaPlayer implements Player {

    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private MediaPlayerState mState;

    private Set<OnPreparedListener> mPreparedListeners;
    private Set<OnErrorListener> mErrorListeners;
    private Set<OnCompletionListener> mCompletionListeners;

    public StatefulMediaPlayer(Context context) {
        mContext = context;
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPreparedListeners = new HashSet<>();
        mErrorListeners = new HashSet<>();
        mCompletionListeners = new HashSet<>();

        mMediaPlayer.setOnPreparedListener(mediaPlayer -> onPrepared());
        mMediaPlayer.setOnErrorListener((mediaPlayer, what, extra) -> onError(what, extra));
        mMediaPlayer.setOnCompletionListener(mediaPlayer -> onCompletion());

        mState = MediaPlayerState.IDLE;
    }

    private void onPrepared() {
        mState = MediaPlayerState.PREPARED;
        for (OnPreparedListener preparedListener : mPreparedListeners) {
            preparedListener.onPrepared(this);
        }
    }

    private boolean onError(int what, int extra) {
        boolean handled = false;
        mState = MediaPlayerState.ERROR;

        Throwable error;
        String message = "Error (" + what + ", " + extra + ")";

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_IO:
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                error = new IOException(message);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                error = new UnknownError(message);
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                error = new UnsupportedOperationException(message);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            default:
                error = new RuntimeException(message);
        }


        for (OnErrorListener errorListener : mErrorListeners) {
            handled |= errorListener.onError(this, error);
        }
        return handled;
    }

    private void onCompletion() {
        mState = MediaPlayerState.COMPLETED;
        for (OnCompletionListener completionListener : mCompletionListeners) {
            completionListener.onCompletion(this);
        }
    }

    @Override
    public void setDataSource(String path) throws IOException {
        if (mState.canSetDataSource()) {
            File source = new File(path);
            mMediaPlayer.setDataSource(mContext, Uri.fromFile(source));
            mState = MediaPlayerState.INITIALIZED;
        } else {
            Timber.e("Cannot set data source while in state %s", mState);
        }
    }

    @Override
    public void prepare() {
        if (mState.canPrepare()) {
            mMediaPlayer.prepareAsync();
            mState = MediaPlayerState.PREPARING;
        } else {
            Timber.e("Cannot prepare while in state %s", mState);
        }
    }

    @Override
    public void reset() {
        if (mState.canReset()) {
            mMediaPlayer.reset();
            mState = MediaPlayerState.IDLE;
        } else {
            Timber.e("Cannot reset while in state %s", mState);
        }
    }

    @Override
    public void setWakeMode(int wakeMode) {
        mMediaPlayer.setWakeMode(mContext, wakeMode);
    }

    @Override
    public void setAudioSessionId(int audioSessionId) {
        if (mState.canSetAudioSessionId()) {
            mMediaPlayer.setAudioSessionId(audioSessionId);
        } else {
            Timber.e("Cannot set audio session id while in state %s", mState);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (mState.canSetVolume()) {
            mMediaPlayer.setVolume(volume, volume);
        } else {
            Timber.e("Cannot set volume while in state %s", mState);
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

    @Override
    public void seekTo(int mSec) {
        if (mState.canSeek()) {
            mMediaPlayer.seekTo(mSec);
        } else {
            Timber.e("Cannot seek while in state %s", mState);
        }
    }

    @Override
    public void start() {
        if (mState.canStart()) {
            mMediaPlayer.start();
            mState = MediaPlayerState.STARTED;
        } else {
            Timber.e("Cannot start while in state %s", mState);
        }
    }

    @Override
    public void pause() {
        if (mState.canPause()) {
            mMediaPlayer.pause();
            mState = MediaPlayerState.PAUSED;
        } else {
            Timber.e("Cannot pause while in state %s", mState);
        }
    }

    @Override
    public void stop() {
        if (mState.canStop()) {
            mMediaPlayer.stop();
            mState = MediaPlayerState.STOPPED;
        } else {
            Timber.e("Cannot stop while in state %s", mState);
        }
    }

    @Override
    public int getAudioSessionId() {
        return mMediaPlayer.getAudioSessionId();
    }

    @Override
    public int getCurrentPosition() {
        if (!mState.canGetCurrentPosition()) {
            return 0;
        }
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if (!mState.canGetDuration()) {
            return Integer.MAX_VALUE;
        }
        return mMediaPlayer.getDuration();
    }

    @Override
    public PlayerState getPlayerState() {
        return mState;
    }

    @Override
    public boolean isComplete() {
        return mState == MediaPlayerState.COMPLETED;
    }

    @Override
    public boolean isPlaying() {
        return mState == MediaPlayerState.STARTED;
    }

    @Override
    public boolean isPaused() {
        return mState == MediaPlayerState.PAUSED;
    }

    @Override
    public boolean isStopped() {
        return mState == MediaPlayerState.STOPPED;
    }

    @Override
    public boolean isPrepared() {
        return mState == MediaPlayerState.PREPARED;
    }

    @Override
    public boolean isPreparing() {
        return mState == MediaPlayerState.PREPARING;
    }

    @Override
    public void release() {
        mContext = null;
        mMediaPlayer.release();
        mMediaPlayer = null;
        mState = MediaPlayerState.RELEASED;
    }
}
