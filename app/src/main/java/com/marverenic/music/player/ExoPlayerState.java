package com.marverenic.music.player;

import com.google.android.exoplayer.ExoPlayer;

public enum ExoPlayerState implements PlayerState {

    IDLE, PREPARING, BUFFERING, READY, ENDED;

    public static ExoPlayerState fromInt(int exoPlayerState) {
        switch (exoPlayerState) {
            case ExoPlayer.STATE_IDLE:
                return IDLE;
            case ExoPlayer.STATE_PREPARING:
                return PREPARING;
            case ExoPlayer.STATE_BUFFERING:
                return BUFFERING;
            case ExoPlayer.STATE_READY:
                return READY;
            case ExoPlayer.STATE_ENDED:
                return ENDED;
            default:
                throw new IllegalArgumentException(exoPlayerState + " is not a valid state");
        }
    }

    @Override
    public boolean canSetAudioSessionId() {
        return true;
    }

    @Override
    public boolean canSetAudioStreamType() {
        return true;
    }

    @Override
    public boolean canSetDataSource() {
        return true;
    }

    @Override
    public boolean canPrepare() {
        return true;
    }

    @Override
    public boolean canGetDuration() {
        return true;
    }

    @Override
    public boolean canGetCurrentPosition() {
        return true;
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeek() {
        return true;
    }

    @Override
    public boolean canSetVolume() {
        return true;
    }

    @Override
    public boolean canStop() {
        return true;
    }

    @Override
    public boolean canReset() {
        return true;
    }
}
