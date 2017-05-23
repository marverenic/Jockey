package com.marverenic.music.player;

import android.graphics.Bitmap;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Observable;
import rx.Single;

/**
 * A support interface for connecting the application UI to the media player. This interface defines
 * basic commands and information sent and received from the player. Any data received from the
 * player can be obtained through various get methods in this interface as observable streams. Any
 * observable stream should always be kept up-to-date unless it is infeasible to do so.
 */
public interface PlayerController {

    int MAXIMUM_CHUNK_ENTRIES = 500;

    /**
     * Gets error messages from the service that can be displayed on the UI
     * @return An observable stream of user-presentable error messages
     */
    Observable<String> getError();

    /**
     * Gets information messages from the service that can be displayed on the UI
     * @return An observable stream of user-presentable info messages
     */
    Observable<String> getInfo();

    /**
     * Generates a snapshot of the current playback state (useful for state restoration and undoing
     * edits to the playback state)
     * @return A single up-to-date player state
     * @see #restorePlayerState(PlayerState) To restore this state
     */
    Single<PlayerState> getPlayerState();

    /**
     * Restores a snapshot of a previous playback state. Any data in this player state will
     * overwrite the current playback state
     * @param restoreState The state to be restored
     * @see #getPlayerState() To create a state to be restored
     */
    void restorePlayerState(PlayerState restoreState);

    /**
     * Permanently ends music playback. This should only be called when exiting the application or
     * when music playback will never be used for the rest of the lifetime of the app.
     */
    void stop();

    /**
     * Skips to the next song in the queue according to the currently applied repeat settings.
     */
    void skip();

    /**
     * Restarts the currently playing song if it is more than a couple of seconds into playback or
     * if the current song is at the beginning of the queue, otherwise skips to the previous song
     * in the queue.
     *
     * If the current song is at the beginning of the queue, this method will always restart it
     * from the beginning, unless repeat all is enabled, in which case it will wrap around to the
     * beginning of the last song in the queue.
     *
     * @see #updatePlayerPreferences(ReadOnlyPreferenceStore) for more information on this behavior
     */
    void previous();

    /**
     * Pauses music if it is currently playing, or plays music if it is currently paused.
     */
    void togglePlay();

    /**
     * Starts or resumes music playback. If music is already playing, this method does nothing.
     */
    void play();

    /**
     * Pauses music playback. If music is already paused or stopped, this method does nothing.
     */
    void pause();

    /**
     * Updates the settings used by the music player. Settings updated include equalizer, shuffle,
     * and repeat settings. All settings are handled by the player as described below.
     *
     * <p>
     * Equalizers are maintained internally automatically. When available and enabled with
     * {@code preferenceStore.getEqualizerEnabled()}, the music player will bind and maintain
     * the equalizer with settings obtained from {@code preferenceStore.getEqualizerSettings()}.
     * </p>
     *
     * <p>
     * Shuffle states are maintained internally. When shuffle is toggled and updated with this
     * method, the music player will generate or restore the appropriate shuffled or original linear
     * queue and continue playback seamlessly.
     * </p>
     *
     * <p>
     * Repeat settings will affect the behavior of song playback when a song finishes. With
     * repeat all enabled, the queue will loop around when the last song completes. The behavior of
     * {@link #skip()} and {@link #previous()} are also changed so that they will loop around the
     * ends of the queue instead of their default behavior of pausing playback and restarting the
     * current song unconditionally when at the end and beginning of the queue, respectively. With
     * repeat one enabled, the current song will be replayed once it finishes. With repeat disabled,
     * playback will end once the last song in the queue finishes. Both repeat one and repeat none
     * don't affect the behavior of {@link #skip()} and {@link #previous()}.
     * </p>
     *
     * @param preferenceStore The preferences to apply to the media player.
     */
    void updatePlayerPreferences(ReadOnlyPreferenceStore preferenceStore);

    /**
     * Replaces the current queue. Playback will start from the beginning of {@code newPosition}.
     * If shuffle is enabled, {@code newQueue} will be randomized, but playback will still start at
     * the song at {@code newQueue.get(newPosition)} before the shuffle is applied.
     * @param newQueue The replacement queue
     * @param newPosition The index in the queue to start playback from
     * @see #editQueue(List, int) If there is a small modification to the queue instead of an entire
     *                            replacement to the queue
     */
    void setQueue(List<Song> newQueue, int newPosition);

    /**
     * Empties the now playing queue and stops music playback.
     */
    void clearQueue();

    /**
     * Skips to a different song in the queue.
     * @param newPosition The song index in the queue to skip to.
     */
    void changeSong(int newPosition);

    /**
     * Modifies the current queue. Unlike {@link #setQueue(List, int)}, this method will not
     * reshuffle the current queue if shuffle is enabled.
     * @param queue The replacement queue
     * @param newPosition The index in the queue to start playback from
     */
    void editQueue(List<Song> queue, int newPosition);

    /**
     * Enqueues a song to play after the current song. This method does nothing to prevent
     * duplicates from being added to the queue.
     * @param song The song to be played next.
     */
    void queueNext(Song song);

    /**
     * Enqueues a group of songs to be played after the current song. Songs are enqueued in the
     * same order they are passed in as. This method does nothing to prevent duplicate items from
     * being added to the queue.
     * @param songs The songs to be played next.
     */
    void queueNext(List<Song> songs);

    /**
     * Enqueues a song at the end of the now playing queue. This method does nothing to prevent
     * duplicates from being added to the queue.
     * @param song The song to be added at the end of the queue.
     */
    void queueLast(Song song);

    /**
     * Enqueues a group of songs at the end of the now playing queue. This method does nothing to
     * prevent duplicate items from being added to the queue.
     * @param songs The songs to be added at the end of the queue.
     */
    void queueLast(List<Song> songs);

    /**
     * Seek to a new position within the currently playing track
     * @param position The position (in milliseconds since the start of the current song)
     *                 to seek to.
     */
    void seek(int position);

    /**
     * @return Whether or not music is currently being played. A value of {@code true} in the
     *         observable stream means that music is playing, a value of {@code false} means it is
     *         not playing (nothing else is guaranteed about the state of music playback).
     */
    Observable<Boolean> isPlaying();

    /**
     * @return The currently playing song as an observable stream.
     */
    Observable<Song> getNowPlaying();

    /**
     * @return The queue as an observable stream. This value is updated whenever the queue changes,
     *         including for changes caused by toggling shuffle.
     */
    Observable<List<Song>> getQueue();

    /**
     * @return The index of the currently playing song in the queue as an observable stream.
     */
    Observable<Integer> getQueuePosition();

    /**
     * @return The current seek position in the current song as an observable stream. This value is
     *         kept up-to-date, but may only be updated every 100 ms or so to preserve battery life.
     */
    Observable<Integer> getCurrentPosition();

    /**
     * @return The duration of the currently playing song as an observable stream.
     */
    Observable<Integer> getDuration();

    /**
     * Gets the current shuffle status of the music player. This may be different from the
     * application if {@link #updatePlayerPreferences(ReadOnlyPreferenceStore)} isn't called
     * correctly.
     * @return The current shuffle state as an observable stream. A value of {@code true} means that
     *         shuffle is enabled, and {@code false} means that it is disabled.
     */
    Observable<Boolean> isShuffleEnabled();

    /**
     * Gets the current repeat mode. This will be a number greater than 1 if Multi-Repeat is enabled
     * (with the value representing the number of times the song will be played back-to back), or
     * one of either {@link MusicPlayer#REPEAT_NONE}, {@link MusicPlayer#REPEAT_ONE},
     * or {@link MusicPlayer#REPEAT_ALL}
     * @return The current repeat mode
     * @see #updatePlayerPreferences(ReadOnlyPreferenceStore) To set the repeat mode to one of
     *      the standard repeat modes
     * @see #setMultiRepeatCount(int) To set the multi-repeat count
     */
    Observable<Integer> getRepeatMode();

    /**
     * Enables Multi-Repeat. With Multi-Repeat enabled, the current song will be played back-to-back
     * {@code count} times. When the next song starts (either because the song was skipped or
     * because the counter reached 0), Multi-Repeat will be disabled and the previous repeat mode
     * will be reapplied.
     * @param count The number of times to repeat the current song back-to-back.
     */
    void setMultiRepeatCount(int count);

    /**
     * Gets the ending timestamp of the currently set sleep timer. When
     * {@link System#currentTimeMillis()} exceeds this value, music playback will be paused.
     * @return An observable stream of sleep timer ending timestamps. If the sleep timer is
     *         disabled, a timestamp in the past (normally {@code 0}) will be passed.
     */
    Observable<Long> getSleepTimerEndTime();

    /**
     * Sets a new or updates an existing sleep timer to pause music at a certain timestamp.
     * @param timestampInMillis The time (in milliseconds since the Unix epoch) to pause music
     *                          playback. This value will be compared against
     *                          {@link System#currentTimeMillis()}
     * @see #disableSleepTimer() To disable this sleep timer
     */
    void setSleepTimerEndTime(long timestampInMillis);

    /**
     * Ends any previously set sleep timer without triggering it.
     * @see #setSleepTimerEndTime(long) To enable a sleep timer
     */
    void disableSleepTimer();

    /**
     * Gets the embedded album artwork of the now playing song
     * @return An observable stream of bitmaps for album artwork. If there is no embedded artwork,
     *         or if it can't be loaded, then {@code null} will be passed in the stream.
     */
    Observable<Bitmap> getArtwork();

}
