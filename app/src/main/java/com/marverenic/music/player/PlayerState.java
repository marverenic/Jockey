package com.marverenic.music.player;

public interface PlayerState {

    boolean canSetAudioSessionId();

    boolean canSetAudioStreamType();

    boolean canSetDataSource();

    boolean canPrepare();

    boolean canGetDuration();

    boolean canGetCurrentPosition();

    boolean canStart();

    boolean canPause();

    boolean canSeek();

    boolean canSetVolume();

    boolean canStop();

    boolean canReset();
}
