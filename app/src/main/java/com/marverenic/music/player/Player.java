package com.marverenic.music.player;

import java.io.IOException;

public interface Player {

    void setDataSource(String path) throws IOException;
    void prepare();
    void reset();

    void setWakeMode(int wakeMode);
    void setVolume(float volume);

    void addOnPreparedListener(OnPreparedListener onPreparedListener);
    void addOnErrorListener(OnErrorListener onErrorListener);
    void addOnCompletionListener(OnCompletionListener onCompletionListener);

    void removeOnPreparedListener(OnPreparedListener onPreparedListener);
    void removeOnErrorListener(OnErrorListener onErrorListener);
    void removeOnCompletionListener(OnCompletionListener onCompletionListener);

    void seekTo(int mSec);

    void start();
    void pause();
    void stop();

    int getAudioSessionId();
    int getCurrentPosition();
    int getDuration();

    PlayerState getPlayerState();

    boolean isComplete();
    boolean isPlaying();
    boolean isPaused();
    boolean isStopped();
    boolean isPrepared();
    boolean isPreparing();

    void release();

    interface OnPreparedListener {
        void onPrepared(Player player);
    }

    interface OnErrorListener {
        boolean onError(Player player, Throwable error);
    }

    interface OnCompletionListener {
        void onCompletion(Player player);
    }

}
