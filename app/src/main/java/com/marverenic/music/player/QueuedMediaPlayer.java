package com.marverenic.music.player;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marverenic.music.instances.Song;

import java.util.List;

public interface QueuedMediaPlayer {

    void setPlaybackEventListener(@Nullable PlaybackEventListener listener);

    Song getNowPlaying();

    List<Song> getQueue();

    int getQueueSize();

    void setQueue(@NonNull List<Song> queue);

    void setQueue(@NonNull List<Song> queue, int index);

    void setQueueIndex(int index);

    int getQueueIndex();

    void prepare(boolean playWhenReady);

    void skip();

    void skipPrevious();

    void seekTo(int mSec);

    void stop();

    void play();

    void pause();

    int getCurrentPosition();

    int getDuration();

    PlayerState getState();

    boolean isComplete();

    boolean isPaused();

    boolean isStopped();

    boolean isPreparing();

    void setAudioEffects(AudioEffectController.Generator... effects);

    void setVolume(float volume);

    int getAudioSessionId();

    boolean isPlaying();

    void setWakeMode(int mode);

    void reset();

    void release();

    /**
     * Interface definition to act as a callback when important lifecycle events occur within a
     * QueuedMediaPlayer. This allows higher-level behaviors to be defined more
     * flexibly.
     */
    interface PlaybackEventListener {
        /**
         * Invoked when the current song has finished playing. If the implementor wishes to
         * start the next song after the previous song completes, it should call {@link #skip()}
         * here. It may also do so conditionally by testing that {@link #getQueueIndex()} isn't
         * {@link #getQueueSize()} - 1 to prevent the queue from always looping in a repeat-all
         * pattern.
         *
         * Implementors should not assume that {@link #skip()} will instantly begin the next song.
         * In the event that the next player has failed to initialize or has gone out-of sync
         * somehow, any implementations that depend on this assumption will fail. To implement
         * such behavior, implement {@link #onSongStart()} instead.
         */
        void onCompletion();

        /**
         * Invoked when a new song is currently loaded by the active media player after calling
         * {@link #skip()}, {@link #skipPrevious()}, or any other method that directly changes the
         * currently playing song. This is called even if the song isn't playing and only implies
         * that {@link #getNowPlaying()} has a new value.
         *
         * Implementors may use this method to update the app's UI or preform additional work
         * dependent on song changes
         */
        void onSongStart();

        /**
         * Invoked when an error has occurred while preparing music or during playback. This may be
         * called with respect to either the current or next media player.
         * @param error The exception that was raised by one of the internal Players
         * @return {@code true} if the error was handled, {@code false} otherwise
         */
        boolean onError(Throwable error);
    }
}
