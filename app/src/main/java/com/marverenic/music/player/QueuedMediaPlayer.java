package com.marverenic.music.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marverenic.music.instances.Song;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

/**
 * QueuedMediaPlayer is a wrapper class that holds and manages two {@link Player} Objects.
 *
 * This class will manage its own playback by default. The playback queue may be modified through
 * {@link #setQueue(List, int)}, {@link #setQueue(List)}, and {@link #setQueueIndex(int)}.
 *
 * When a song is started, the active MediaPlayer will prepare asynchronously if necessary, and
 * another MediaPlayer will be prepared in the background with the next track to be played. The
 * queue acts as a circular array, so if the current song is at the end of the queue, the next
 * media player will be prepared to play the song at the start of the queue.
 *
 * These MediaPlayers frequently swap between playing and being on standby throughout the lifetime
 * of this Object, and allow for gapless music playback.
 *
 * The default implementation of this class will not begin playback of the next song if the current
 * song finishes. To override this, set a custom {@link QueuedMediaPlayer.PlaybackEventListener}
 * with {@link #setPlaybackEventListener(PlaybackEventListener)} and implement
 * {@link PlaybackEventListener#onCompletion()} to call {@link #skip()} as appropriate.
 */
public class QueuedMediaPlayer implements Player.OnPreparedListener,
        Player.OnCompletionListener, Player.OnErrorListener {

    private Player mCurrentPlayer;
    private Player mNextPlayer;
    private PlaybackEventListener mCallback;
    private List<Song> mQueue;
    private int mQueueIndex;
    private int mRequestedSeekPosition;
    private boolean mPlayWhenPrepared;

    /**
     * Initialize a new QueuedMediaPlayer
     * @param context A Context used to read song files from the filesystem. This Context is kept
     *                for the lifetime of this QueuedMediaPlayer until {@link #release()} is called
     */
    public QueuedMediaPlayer(Context context) {
        mCurrentPlayer = new StatefulMediaPlayer(context);
        mNextPlayer = new StatefulMediaPlayer(context);
        mNextPlayer.setAudioSessionId(mCurrentPlayer.getAudioSessionId());

        mCurrentPlayer.addOnPreparedListener(this);
        mCurrentPlayer.addOnErrorListener(this);
        mCurrentPlayer.addOnCompletionListener(this);

        mNextPlayer.addOnPreparedListener(this);
        mNextPlayer.addOnErrorListener(this);
        mNextPlayer.addOnCompletionListener(this);

        mQueue = Collections.emptyList();
        mQueueIndex = 0;
    }

    public void setPlaybackEventListener(@Nullable PlaybackEventListener listener) {
        mCallback = listener;
    }

    /**
     * @return The {@link Song} that is currently being played (or that the active backing
     *         {@link MediaPlayer} has loaded)
     */
    public Song getNowPlaying() {
        if (mQueue.isEmpty()) {
            return null;
        }
        return mQueue.get(mQueueIndex);
    }

    /**
     * @return The next {@link Song} in the queue. If the current queue index is the last position
     *         in the queue, then this will wrap to the first item in the queue.
     */
    private Song getNext() {
        if (mQueue.isEmpty()) {
            return null;
        }
        return mQueue.get((mQueueIndex + 1) % mQueue.size());
    }

    /**
     * @return A reference to the queue that this media player is managing
     */
    public List<Song> getQueue() {
        return mQueue;
    }

    /**
     * @return The number of Objects in the backing queue
     */
    public int getQueueSize() {
        return mQueue.size();
    }

    /**
     * Changes the current queue without modifying the current index. If the index would become
     * invalid after this operation (because it is beyond the end of the replacement queue), then
     * it will be reset to 0.
     * @param queue The replacement queue
     */
    public void setQueue(@NonNull List<Song> queue) {
        Timber.i("setQueue called (%d items)", queue.size());
        if (queue.size() >= mQueue.size()) {
            setQueue(queue, mQueueIndex);
        } else {
            setQueue(queue, 0);
        }
    }

    /**
     * Changes the current queue index and modifies the current index. If the song at the new
     * queue's new index isn't currently playing, then both backing {@link MediaPlayer}s will be
     * prepared with the new songs and playback will restart if it is currently ongoing.
     * @param queue The replacement queue
     * @param index The replacement queue index
     */
    public void setQueue(@NonNull List<Song> queue, int index) {
        Timber.i("setQueue called (%d items, start index: %d)", queue.size(), index);
        if (queue.isEmpty()) {
            reset();
            return;
        }

        boolean songChanged = getNowPlaying() == null || !getNowPlaying().equals(queue.get(index));
        mQueue = queue;
        setQueueIndex(index);
        if (songChanged) {
            prepare(isPlaying());
        } else {
            prepareNextPlayer();
        }
    }

    /**
     * Changes the queue's current index. Playback will not be affected by this operation, even if
     * the song at the new index differs from the current song.
     * @param index The replacement queue index
     * @see #prepare(boolean) to apply the changes of this operation and change the current song to
     *                        match the modification
     */
    public void setQueueIndex(int index) {
        Timber.i("setQueueIndex(%d) called", index);
        if (index < 0 || index >= mQueue.size() && index != 0) {
            throw new IllegalArgumentException("index must be between 0 and queue.size");
        }
        mQueueIndex = index;
    }

    /**
     * @return The index of the currently playing song in the queue
     */
    public int getQueueIndex() {
        return mQueueIndex;
    }

    /**
     * Increments the queue index wrapping it to 0 if it exceeds the size of the queue. This method
     * won't affect playback until {@link #prepare(boolean)} is called
     */
    private void incrementQueueIndex() {
        Timber.i("incrementQueueIndex called");
        mQueueIndex++;
        if (mQueueIndex >= mQueue.size()) {
            mQueueIndex -= mQueue.size();
        }
    }

    /**
     * Decrements the queue index wrapping it to the queue's last index if it goes below 0. This
     * method won't affect playback until {@link #prepare(boolean)} is called
     */
    private void decrementQueueIndex() {
        Timber.i("decrementQueueIndex called");
        mQueueIndex--;
        if (mQueueIndex < 0) {
            mQueueIndex += mQueue.size();
        }
    }

    /**
     * Swaps the references of {@link #mCurrentPlayer} and {@link #mNextPlayer}
     */
    private void swapMediaPlayers() {
        Timber.i("Swapping media players");
        Player previous = mCurrentPlayer;
        mCurrentPlayer = mNextPlayer;
        mNextPlayer = previous;
    }

    /**
     * Asynchronously sets the data source of both the current and next {@link MediaPlayer} Objects.
     * This should only be called externally when the queue has been modified.
     * @param playWhenReady May be used to start playback of the current song as soon as it has
     *                      finished being prepared. {@link #isPlaying()} may be passed in to
     *                      preserve the player's current status
     */
    public void prepare(boolean playWhenReady) {
        Timber.i("prepare(%b) called", playWhenReady);
        prepareCurrentPlayer(playWhenReady);
        prepareNextPlayer();
    }

    /**
     * Asynchronously sets the data source of the current player reference
     * @param playWhenReady May be used to start playback of the current media player as soon as it
     *                      has finished being prepared.
     */
    private void prepareCurrentPlayer(boolean playWhenReady) {
        Timber.i("Preparing current player...");

        mCurrentPlayer.reset();
        Song curr = getNowPlaying();

        if (curr != null) {
            mPlayWhenPrepared = playWhenReady;

            try {
                mCurrentPlayer.setDataSource(getNowPlaying().getLocation());
                mCurrentPlayer.prepare();
            } catch (IOException e) {
                Timber.e(e, "Failed to prepare current player");
                if (mCallback != null) {
                    Timber.i("Delegating callback to handle exception");
                    mCallback.onSetDataSourceException(e);
                } else {
                    Timber.i("Handling error by resetting current player");
                    mCurrentPlayer.reset();
                }
            }
        }
    }

    /**
     * Asynchronously sets the data source of the next player reference
     */
    private void prepareNextPlayer() {
        Timber.i("Preparing next player...");

        mNextPlayer.reset();
        Song next = getNext();
        if (next != null) {
            try {
                mNextPlayer.setDataSource(getNext().getLocation());
                mNextPlayer.prepare();
            } catch (IOException e) {
                // If the song couldn't be loaded in advance, try again when it's about to be played
                Timber.e(e, "Failed to prepare next MediaPlayer");
                mNextPlayer.reset();
            }
        }
    }

    /**
     * Ends playback of the current song and begins the next song. This method will automatically
     * begin preparing the next song to be played
     *
     * When this method is called, the two backing {@link MediaPlayer} references will be swapped,
     * and the new current media player will begin playback immediately if it was successfully
     * prepared, otherwise it will attempt to be prepared again and begin playback as soon as
     * possible.
     */
    public void skip() {
        Timber.i("Skipping current song");

        incrementQueueIndex();

        mCurrentPlayer.reset();
        swapMediaPlayers();
        if (mCurrentPlayer.isPrepared() || mCurrentPlayer.isComplete()) {
            Timber.i("MediaPlayer is ready. Beginning playback now.");
            mCurrentPlayer.start();
            if (mCallback != null) {
                mCallback.onSongStart();
            }
        } else {
            Timber.i("MediaPlayer isn't ready yet.");
            prepareCurrentPlayer(true);
        }
        prepareNextPlayer();
    }

    /**
     * Ends playback of the current song and begins the previous song. This method will handle
     * preparing the new configuration of MediaPlayers
     *
     * This method does not guarantee instant playback because the next MediaPlayer will most
     * likely have been repurposed and will be reset before it can be used to play the previous song
     */
    public void skipPrevious() {
        Timber.i("Skipping to previous song");

        decrementQueueIndex();

        Timber.i("Resetting current media player to reuse it later");
        mCurrentPlayer.seekTo(0);
        mCurrentPlayer.pause();

        swapMediaPlayers();
        Timber.i("Starting playback of previous song");
        prepareCurrentPlayer(true);
    }

    @Override
    public void onCompletion(Player player) {
        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    @Override
    public boolean onError(Player player, int what, int extra) {
        Timber.e("Error (%d, %d)", what, extra);

        if (player.equals(mCurrentPlayer)) {
            mPlayWhenPrepared = false;
        }

        //noinspection SimplifiableIfStatement
        if (mCallback != null) {
            return mCallback.onError(what, extra);
        } else {
            return false;
        }
    }

    @Override
    public void onPrepared(Player player) {
        if (player.equals(mCurrentPlayer)) {
            Timber.i("Current MediaPlayer is prepared");

            mCurrentPlayer.seekTo(mRequestedSeekPosition);
            mRequestedSeekPosition = 0;

            if (mPlayWhenPrepared) {
                Timber.i("This MediaPlayer was waiting to be started. Beginning playback...");
                mPlayWhenPrepared = false;
                play();
            }
            if (mCallback != null) {
                mCallback.onSongStart();
            }
        }
    }

    /**
     * Seeks to a different time within the current song
     * @param mSec The new time position to seek to, in milliseconds since the beginning of the song
     */
    public void seekTo(int mSec) {
        if (getState().canSeek()) {
            Timber.i("Seeking to %d", mSec);
            mCurrentPlayer.seekTo(mSec);
        } else {
            Timber.i("Current MediaPlayer isn't ready yet. Deferring seek to %d", mSec);
            mRequestedSeekPosition = mSec;
        }
    }

    /**
     * Ends playback of the current song
     */
    public void stop() {
        Timber.w("Stop called. Future playback may be inconsistent.");
        // TODO make sure that the state is correctly restored when calling play() after this
        mCurrentPlayer.stop();
    }

    /**
     * Resumes playback of the current song
     * To begin new playback, use {@link #prepare(boolean)} and pass in {@code true} instead
     */
    public void play() {
        Timber.i("play() called");
        mCurrentPlayer.start();
    }

    /**
     * Pauses playback of the current song
     */
    public void pause() {
        Timber.i("pause() called");
        mCurrentPlayer.pause();
    }

    /**
     * @return The current seek position of the now playing song in milliseconds
     */
    public int getCurrentPosition() {
        return mCurrentPlayer.getCurrentPosition();
    }

    /**
     * @return The duration of the currently playing song in milliseconds
     */
    public int getDuration() {
        return mCurrentPlayer.getDuration();
    }

    /**
     * @return The current state of the backing {@link Player}
     */
    public PlayerState getState() {
        return mCurrentPlayer.getPlayerState();
    }

    /**
     * @return {@link true} if the current {@link MediaPlayer} has become idle after completing
     *         playback.
     */
    public boolean isComplete() {
        return mCurrentPlayer.isComplete();
    }

    public boolean isPaused() {
        return mCurrentPlayer.isPaused();
    }

    public boolean isStopped() {
        return mCurrentPlayer.isStopped();
    }

    public boolean isPreparing() {
        return mCurrentPlayer.isPreparing();
    }

    /**
     * Interface definition to act as a callback when important lifecycle events occur within the
     * backing {@link MediaPlayer} objects. This allows higher-level behaviors to be defined more
     * flexibly.
     */
    public interface PlaybackEventListener {
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
         * Invoked when an error has occurred during playback. This may be called with respect
         * to either the current or next media player.
         * @param what The type of error that occurred. May be one of
         *             {@link MediaPlayer#MEDIA_ERROR_UNKNOWN} or
         *             {@link MediaPlayer#MEDIA_ERROR_SERVER_DIED}
         * @param extra An extra code received from the backing {@link MediaPlayer}
         * @return {@code true} if the error was handled, {@code false} otherwise
         */
        boolean onError(int what, int extra);

        /**
         * Invoked when an {@link IOException} was thrown while calling
         * {@link MediaPlayer#setDataSource(Context, Uri)}. Implementors should handle this
         * exception by displaying a brief notification and optionally attempt to resume playback
         * in a logical manner.
         * @param exception The exception thrown during the call to {@code setDataSource(...)}
         */
        void onSetDataSourceException(IOException exception);
    }

    /**
     * Sets the volume of both the left and right audio channels
     * @param volume Volume multiplier. Must be between {@code 0.0f} and {@code 1.0f}
     */
    public void setVolume(float volume) {
        mCurrentPlayer.setVolume(volume);
        mNextPlayer.setVolume(volume);
    }

    /**
     * Sets the AudioSessionId of the backing {@link MediaPlayer} Objects.
     * @param sessionId The audio session ID
     * @throws IllegalStateException If one of the {@link MediaPlayer}s is in an invalid state
     */
    public void setAudioSessionId(int sessionId) throws IllegalStateException {
        mCurrentPlayer.setAudioSessionId(sessionId);
        mNextPlayer.setAudioSessionId(sessionId);
    }

    /**
     * @return The Audio Session ID of the backing {@link MediaPlayer}s. Both MediaPlayers will
     *         always share this value
     */
    public int getAudioSessionId() {
        return mCurrentPlayer.getAudioSessionId();
    }

    /**
     * @return {@code true} if the current song is being played, {@code false} otherwise
     */
    public boolean isPlaying() {
        return mCurrentPlayer.isPlaying();
    }

    /**
     * Sets an optional wake mode on the backing {@link MediaPlayer}s which may be used to allow
     * playback while the device is locked
     * @param mode The new wake mode to set on the backing MediaPlayers
     * @see android.os.PowerManager
     */
    public void setWakeMode(int mode) {
        mCurrentPlayer.setWakeMode(mode);
        mNextPlayer.setWakeMode(mode);
    }

    /**
     * Resets both MediaPlayers, and clears the queue. This makes the current instance act like a
     * new one, retaining any attached callbacks and other properties unrelated to the queue.
     */
    public void reset() {
        Timber.i("resetting QueuedMediaPlayer...");

        mCurrentPlayer.reset();
        mNextPlayer.reset();

        mQueue = Collections.emptyList();
        mQueueIndex = 0;

        mRequestedSeekPosition = 0;
        mPlayWhenPrepared = false;
    }

    /**
     * Releases both MediaPlayers and the Context used to create this Object. Once this method is
     * called, this instance will no longer be able to play music
     */
    public void release() {
        Timber.i("Releasing QueuedMediaPlayer...");

        mCurrentPlayer.release();
        mNextPlayer.release();
        mCurrentPlayer = null;
        mNextPlayer = null;
    }
}
