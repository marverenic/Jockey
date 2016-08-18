package com.marverenic.music.player;

enum PlayerState {

    IDLE(true, true, true, false, false, true, false, false, false, true, false, true),
    INITIALIZED(false, true, false, true, false, true, false, false, false, true, false, true),
    PREPARING(false, false, false, false, false, false, false, false, false, false, false, true),
    PREPARED(false, false, false, false, true, true, true, false, true, true, true, true),
    STARTED(false, false, false, false, true, true, true, true, true, true, true, true),
    PAUSED(false, false, false, false, true, true, true, true, true, true, true, true),
    STOPPED(false, false, false, true, true, true, false, false, false, true, true, true),
    COMPLETED(false, false, false, false, true, true, true, true, true, true, true, true),
    ERROR(false, false, false, false, false, false, false, false, false, false, false, true),
    RELEASED(false, false, false, false, false, false, false, false, false, false, false, false);

    private boolean mCanSetAudioSessionId;
    private boolean mCanSetAudioStreamType;
    private boolean mCanSetDataSource;
    private boolean mCanPrepare;
    private boolean mCanGetDuration;
    private boolean mCanGetCurrentPosition;
    private boolean mCanStart;
    private boolean mCanPause;
    private boolean mCanSeek;
    private boolean mCanSetVolume;
    private boolean mCanStop;
    private boolean mCanReset;

    PlayerState(boolean canSetAudioSessionId,
                boolean canSetAudioStreamType,
                boolean canSetDataSource,
                boolean canPrepare,
                boolean canGetDuration,
                boolean canGetCurrentPosition,
                boolean canStart,
                boolean canPause,
                boolean canSeek,
                boolean canSetVolume,
                boolean canStop,
                boolean canReset) {

        mCanSetAudioSessionId = canSetAudioSessionId;
        mCanSetAudioStreamType = canSetAudioStreamType;
        mCanSetDataSource = canSetDataSource;
        mCanPrepare = canPrepare;
        mCanGetDuration = canGetDuration;
        mCanGetCurrentPosition = canGetCurrentPosition;
        mCanStart = canStart;
        mCanPause = canPause;
        mCanSeek = canSeek;
        mCanSetVolume = canSetVolume;
        mCanStop = canStop;
        mCanReset = canReset;
    }

    public boolean canSetAudioSessionId() {
        // IDLE
        return mCanSetAudioSessionId;
    }

    public boolean canSetAudioStreamType() {
        // IDLE, INITIALIZED
        return mCanSetAudioStreamType;
    }

    public boolean canSetDataSource() {
        // IDLE
        return mCanSetDataSource;
    }

    public boolean canPrepare() {
        // INITIALIZED, STOPPED
        return mCanPrepare;
    }

    public boolean canGetDuration() {
        // PREPARED, STARTED, PAUSED, STOPPED, COMPLETED
        return mCanGetDuration;
    }

    public boolean canGetCurrentPosition() {
        // IDLE, INITIALIZED, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED
        return mCanGetCurrentPosition;
    }

    public boolean canStart() {
        // PREPARED, STARTED, PAUSED, COMPLETED
        return mCanStart;
    }

    public boolean canPause() {
        // STARTED, PAUSED, COMPLETED
        return mCanPause;
    }

    public boolean canSeek() {
        // PREPARED, STARTED, PAUSED, COMPLETED
        return mCanSeek;
    }

    public boolean canSetVolume() {
        // IDLE, INITIALIZED, STOPPED, PREPARED, STARTED, PAUSED, COMPLETED
        return mCanSetVolume;
    }

    public boolean canStop() {
        // PREPARED, STARTED, STOPPED, PAUSED, COMPLETED
        return mCanStop;
    }

    public boolean canReset() {
        // IDLE, INITIALIZED, PREPARED, STARTED, PAUSED, STOPPED, COMPLETED, ERROR
        return mCanReset;
    }

}
