package com.marverenic.music.player;

import com.google.android.exoplayer2.Player;

public enum ExoPlayerState {

    IDLE, BUFFERING, READY, ENDED;

    public static ExoPlayerState fromInt(int exoPlayerState) {
        switch (exoPlayerState) {
            case Player.STATE_IDLE:
                return IDLE;
            case Player.STATE_BUFFERING:
                return BUFFERING;
            case Player.STATE_READY:
                return READY;
            case Player.STATE_ENDED:
                return ENDED;
            default:
                throw new IllegalArgumentException(exoPlayerState + " is not a valid state");
        }
    }

}
