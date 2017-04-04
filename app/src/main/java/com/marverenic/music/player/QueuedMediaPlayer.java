package com.marverenic.music.player;

import android.media.audiofx.Equalizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marverenic.music.model.Song;

import java.util.Collections;
import java.util.List;

public interface QueuedMediaPlayer {

    /**
     * Attaches a listener to be notified of various lifecycle events. See the
     * {@link PlaybackEventListener} interface for more information.
     *
     * @param listener The listener to be informed of lifecycle events, or {@link null}
     */
    void setPlaybackEventListener(@Nullable PlaybackEventListener listener);

    /**
     * Gets the song that is currently playing
     * @return The song that is at the current index in the queue, or {@code null} if nothing
     *         is playing. This is roughly equivalent to {@code getQueue().get(getQueueIndex())}
     */
    Song getNowPlaying();

    /**
     * Gets the current queue
     * @return The queue that is currently set. This collection should not be directly edited as it
     *         will cause undefined behavior.
     */
    List<Song> getQueue();

    /**
     * Gets the number of songs in the queue
     * @return The size of the queue. This is equivalent to calling {@code getQueue().size()}
     */
    int getQueueSize();

    /**
     * Replaces the current queue and index with a new one. If the result of
     * {@link #getNowPlaying()} is unaffected by this call, then playback will be unaffected.
     * Otherwise, playback will start from the beginning of the song at {@code index}
     * in {@code queue}.
     * @param queue The replacement queue. Cannot be {@code null}. To clear the queue, pass in an
     *              empty list like {@link Collections#emptyList()}.
     * @param index The starting index to play music from. Must be between {@code 0}
     *              and {@code queue.size() - 1}. This value is ignored if {@code queue} is empty.
     */
    void setQueue(@NonNull List<Song> queue, int index);

    /**
     * Changes the current song to the one at {@code index} in the current queue, and begins
     * playback from that song.
     * @param index The queue index to play from. Must be between {@code 0}
     *              and {@code getQueueSize() - 1}
     */
    void setQueueIndex(int index);

    /**
     * Gets the index of the currently playing song in the queue
     * @return The currently playing song index
     */
    int getQueueIndex();

    /**
     * Begins playback of the next song in the queue. If repeat all is enabled, skipping the last
     * song in the queue will begin playback of the first song in the queue. Otherwise, the first
     * song will be loaded, but not started.
     */
    void skip();

    /**
     * Begins playback of the previous song if playback is close to the beginning of the song,
     * otherwise restarts the current song.
     */
    void skipPrevious();

    /**
     * Seeks to a different position in the current song
     * @param mSec The time (in milliseconds) to seek to since the start of the current song
     */
    void seekTo(int mSec);

    /**
     * Ends all playback and resets the seek position to the beginning of the current song.
     * @see #release() to free resources held by this object when it is no longer needed
     */
    void stop();

    /**
     * Begins or resumes playback. If already playing, this call is ignored.
     */
    void play();

    /**
     * Pauses playback. If already paused, this call is ignored.
     */
    void pause();

    /**
     * @return The current seek position of the now playing song
     * @see #seekTo(int) to seek to a different position in the song
     */
    int getCurrentPosition();

    /**
     * @return The duration of the currently playing song
     */
    int getDuration();

    /**
     * Returns whether playback has completed.
     * @return {@code true} if playback has ended, {@code false} otherwise
     */
    boolean isComplete();

    /**
     * Returns whether or not playback is paused. A return value of false does not necessarily mean
     * that music is currently playing.
     * @return {@code true} if playback has been paused, {@code false} otherwise
     */
    boolean isPaused();

    /**
     * Returns whether or not playback has been stopped
     * @return {@code true} if playback is stopped, {@code false} otherwise
     */
    boolean isStopped();

    /**
     * Returns whether or not the player is currently in an error state
     * @return {@code true} if the player currently has an error, {@code false} otherwise
     */
    boolean hasError();

    /**
     * Sets the output volume of music playback
     * @param volume The new volume as a decimal number between {@code 0.0f} (silent)
     *               and {@code 1.0f} (loudest – default)
     */
    void setVolume(float volume);

    /**
     * Sets a custom equalizer to apply to music playback
     * @param enabled Whether or not to apply the specified equalizer. {@code true} will apply the
     *                equalizer settings, {@code false} will turn off the equalizer.
     * @param settings The equalizer settings to apply
     */
    void setEqualizer(boolean enabled, Equalizer.Settings settings);

    /**
     * Enables repeat all and disables other repeat settings. Playback will continue through the
     * queue as normal, but will restart at the beginning of the queue once the last song finishes.
     * @see #enableRepeatOne()
     * @see #enableRepeatNone() to turn off repeat
     */
    void enableRepeatAll();

    /**
     * Enables repeat one and disables other repeat settings. The current song will loop infinitely
     * until it is manually skipped
     * @see #enableRepeatAll()
     * @see #enableRepeatNone() to turn off repeat
     */
    void enableRepeatOne();

    /**
     * Disables repeat. Playback will continue through the queue as normal, and playback will stop
     * once the last song finishes.
     * @see #enableRepeatAll()
     * @see #enableRepeatOne()
     */
    void enableRepeatNone();

    /**
     * @return {@code true} if playback is currently ongoing, {@code false} otherwise
     */
    boolean isPlaying();

    /**
     * Resets the playback state of the media player to its default state (i.e. stops playback,
     * clears queue, etc.)
     */
    void reset();

    /**
     * Releases all resources used by this music player. Any methods called after this will fail.
     */
    void release();

    /**
     * Interface definition to act as a callback when important lifecycle events occur within a
     * QueuedMediaPlayer. This allows higher-level behaviors to be defined more
     * flexibly.
     */
    interface PlaybackEventListener {
        /**
         * Invoked when the current song has finished playing. Do not use this callback to control
         * looping behavior – use {@link #enableRepeatAll()}, {@link #enableRepeatOne()}, and
         * {@link #enableRepeatNone()} instead
         *
         * @param completed The Song that just finished playing
         */
        void onCompletion(Song completed);

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
