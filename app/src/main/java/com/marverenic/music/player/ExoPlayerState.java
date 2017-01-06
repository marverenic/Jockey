package com.marverenic.music.player;

import com.google.android.exoplayer2.ExoPlayer;

public enum ExoPlayerState {

    IDLE, BUFFERING, READY, ENDED;

    public static ExoPlayerState fromInt(int exoPlayerState) {
        switch (exoPlayerState) {
            case ExoPlayer.STATE_IDLE:
                return IDLE;
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

}
