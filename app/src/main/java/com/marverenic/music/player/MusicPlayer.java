package com.marverenic.music.player;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.R;
import com.marverenic.music.data.store.RemotePreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.browser.MediaBrowserRoot;
import com.marverenic.music.player.browser.MediaList;
import com.marverenic.music.player.extensions.MusicPlayerExtension;
import com.marverenic.music.ui.library.LibraryActivity;
import com.marverenic.music.utils.ArtworkUtils;
import com.marverenic.music.utils.Internal;
import com.marverenic.music.utils.MusicUtils;
import com.marverenic.music.utils.compat.AudioManagerCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.Intent.ACTION_HEADSET_PLUG;
import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;

/**
 * High level implementation for a MediaPlayer. MusicPlayer is backed by a {@link QueuedMediaPlayer}
 * and provides high-level behavior definitions (for actions like {@link #skip()},
 * {@link #skipPrevious()} and {@link #togglePlay()}) as well as system integration.
 *
 * MediaPlayer provides shuffle and repeat with {@link #setShuffle(boolean, long)} and
 * {@link #setRepeat(int)}, respectively.
 *
 * System integration is implemented by handling Audio Focus through {@link AudioManager}, attaching
 * a {@link MediaSessionCompat}, and with a {@link HeadsetListener} -- an implementation of
 * {@link BroadcastReceiver} that pauses playback when headphones are disconnected.
 */
public class MusicPlayer implements AudioManager.OnAudioFocusChangeListener,
        QueuedMediaPlayer.PlaybackEventListener {

    private static final String TAG = "MusicPlayer";

    /**
     * Package permission that is required to receive broadcasts
     */
    private static final String BROADCAST_PERMISSION = BuildConfig.APPLICATION_ID + ".MUSIC_BROADCAST_PERMISSION";

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has changed its state automatically
     */
    public static final String UPDATE_BROADCAST = "marverenic.jockey.player.REFRESH";

    /**
     * An {@link Intent} extra sent with {@link #UPDATE_BROADCAST} intents which maps to a boolean
     * representing whether or not the update is a minor update (i.e. an update that was triggered
     * by the user).
     */
    public static final String UPDATE_EXTRA_MINOR = "marverenic.jockey.player.REFRESH:minor";

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has information that should be
     * presented to the user
     * @see #INFO_EXTRA_MESSAGE
     */
    public static final String INFO_BROADCAST = "marverenic.jockey.player.INFO";

    /**
     * An {@link Intent} extra sent with {@link #INFO_BROADCAST} intents which maps to a
     * user-friendly information message
     */
    public static final String INFO_EXTRA_MESSAGE = "marverenic.jockey.player.INFO:MSG";

    /**
     * An {@link Intent} action broadcasted when a MusicPlayer has encountered an error when
     * setting the current playback source
     * @see #ERROR_EXTRA_MSG
     */
    public static final String ERROR_BROADCAST = "marverenic.jockey.player.ERROR";

    /**
     * An {@link Intent} extra sent with {@link #ERROR_BROADCAST} intents which maps to a
     * user-friendly error message
     */
    public static final String ERROR_EXTRA_MSG = "marverenic.jockey.player.ERROR:MSG";

    /**
     * Repeat value that corresponds to repeat none. Playback will continue as normal until and will
     * end after the last song finishes
     * @see #setRepeat(int)
     */
    public static final int REPEAT_NONE = 0;

    /**
     * Repeat value that corresponds to repeat all. Playback will continue as normal, but the queue
     * will restart from the beginning once the last song finishes
     * @see #setRepeat(int)
     */
    public static final int REPEAT_ALL = -1;

    /**
     * Repeat value that corresponds to repeat one. When the current song is finished, it will be
     * repeated. The MusicPlayer will never progress to the next track until the user manually
     * changes the song.
     * @see #setRepeat(int)
     */
    public static final int REPEAT_ONE = -2;

    /**
     * Defines the threshold for skip previous behavior. If the current seek position in the song is
     * greater than this value, {@link #skipPrevious()} will seek to the beginning of the song.
     * If the current seek position is less than this threshold, then the queue index will be
     * decremented and the previous song in the queue will be played.
     * This value is measured in milliseconds and is currently set to 5 seconds
     * @see #skipPrevious()
     */
    private static final int SKIP_PREVIOUS_THRESHOLD = 5000;

    /**
     * The volume scalar to set when {@link AudioManager} causes a MusicPlayer instance to duck
     */
    private static final float DUCK_VOLUME = 0.5f;

    /**
     * Controls the maximum number of songs in the sliding window when setting
     * {@link MediaSessionCompat#setQueue(List)}.
     * @see #buildQueueWindow() For the usage of this value
     */
    private static final int MEDIA_SESSION_QUEUE_MAX_SIZE = 250;

    private QueuedMediaPlayer mMediaPlayer;
    private Context mContext;
    private Handler mHandler;
    private MediaSessionCompat mMediaSession;
    private HeadsetListener mHeadphoneListener;
    private OnPlaybackChangeListener mCallback;
    private final List<MusicPlayerExtension> mExtensions;

    @NonNull
    private List<Song> mQueue;
    @NonNull
    private List<Song> mQueueShuffled;

    private boolean mShuffle;
    private int mRepeat;
    private int mMultiRepeat;

    /**
     * Whether this MusicPlayer has focus from {@link AudioManager} to play audio
     * @see #getFocus()
     */
    private boolean mFocused = false;
    /**
     * Whether playback should continue once {@link AudioManager} returns focus to this MusicPlayer
     * @see #onAudioFocusChange(int)
     */
    private boolean mResumeOnFocusGain = false;

    private boolean mResumeOnHeadphonesConnect;

    /**
     * The album artwork of the current song
     */
    private Bitmap mArtwork;

    @Inject MediaBrowserRoot mMediaBrowserRoot;
    private RemotePreferenceStore mRemotePreferenceStore;

    private final Runnable mSleepTimerRunnable = this::onSleepTimerEnd;

    /**
     * Creates a new MusicPlayer with an empty queue. The backing {@link android.media.MediaPlayer}
     * will create a wakelock (specified by {@link PowerManager#PARTIAL_WAKE_LOCK}), and all
     * system integration will be initialized
     * @param context A Context used to interact with other components of the OS and used to
     *                load songs. This Context will be kept for the lifetime of this Object.
     * @param extensions Additional extensions that can be used to augment behavior in MusicPlayer.
     *                   Pass an empty list if no additional behavior is required.
     */
    public MusicPlayer(Context context, PlayerOptions options, List<MusicPlayerExtension> extensions) {
        mContext = context;
        mHandler = new Handler();
        mRemotePreferenceStore = new RemotePreferenceStore(mContext);

        // Initialize the media player
        mMediaPlayer = new QueuedExoPlayer(context);
        mMediaPlayer.setPlaybackEventListener(this);

        mQueue = new ArrayList<>();
        mQueueShuffled = new ArrayList<>();

        // Attach a HeadsetListener to respond to headphone events
        mHeadphoneListener = new HeadsetListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_HEADSET_PLUG);
        filter.addAction(ACTION_AUDIO_BECOMING_NOISY);
        context.registerReceiver(mHeadphoneListener, filter);

        loadPrefs(options);
        initMediaSession();

        mExtensions = Collections.unmodifiableList(new ArrayList<>(extensions));
        for (MusicPlayerExtension ext : mExtensions) {
            ext.onCreateMusicPlayer(this);
        }
    }

    /**
     * Reloads shuffle and repeat preferences from {@link SharedPreferences}
     */
    private void loadPrefs(PlayerOptions playerOptions) {
        Timber.i("Loading SharedPreferences...");

        mShuffle = playerOptions.isShuffleEnabled();
        setRepeat(playerOptions.getRepeatMode());
        setMultiRepeat(mRemotePreferenceStore.getMultiRepeatCount());

        initEqualizer(playerOptions);
        startSleepTimer(mRemotePreferenceStore.getSleepTimerEndTime());

        mResumeOnHeadphonesConnect = playerOptions.shouldResumeOnHeadphonesConnected();
    }

    /**
     * Updates shuffle and repeat preferences from a Preference Store
     * @param options The preference store to read values from
     */
    public void updatePreferences(PlayerOptions options, long seed) {
        requireNotReleased();
        Timber.i("Updating preferences...");
        if (options.isShuffleEnabled() != mShuffle) {
            setShuffle(options.isShuffleEnabled(), seed);
        }

        mResumeOnHeadphonesConnect = options.shouldResumeOnHeadphonesConnected();
        setRepeat(options.getRepeatMode());
        initEqualizer(options);
    }

    /**
     * Initiate a MediaSession to allow the Android system to interact with the player
     */
    private void initMediaSession() {
        Timber.i("Initializing MediaSession");
        ComponentName mbrComponent = new ComponentName(mContext, MediaButtonReceiver.class.getName());
        mMediaSession = new MediaSessionCompat(mContext, TAG, mbrComponent, null);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setCallback(new MediaSessionCallback(this, mMediaBrowserRoot));
        mMediaSession.setSessionActivity(
                PendingIntent.getActivity(
                        mContext, 0,
                        LibraryActivity.newNowPlayingIntent(mContext)
                                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_CANCEL_CURRENT));

        updateMediaSession();

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(mContext, MediaButtonReceiver.class);
        PendingIntent mbrIntent = PendingIntent.getBroadcast(mContext, 0, mediaButtonIntent, 0);
        mMediaSession.setMediaButtonReceiver(mbrIntent);

        mMediaSession.setActive(true);
    }

    /**
     * Reload all equalizer settings from SharedPreferences
     */
    private void initEqualizer(PlayerOptions playerOptions) {
        Timber.i("Initializing equalizer");
        mMediaPlayer.setEqualizer(playerOptions.isEqualizerEnabled(),
            playerOptions.getEqualizerSettings());
    }

    /**
     * Sets a callback for when the current song changes (no matter the source of the change)
     * @param listener The callback to be registered. {@code null} may be passed in to remove a
     *                 previously registered listener. Only one callback may be registered at
     *                 a time.
     */
    public void setPlaybackChangeListener(OnPlaybackChangeListener listener) {
        mCallback = listener;
    }

    /**
     * Updates the metadata in the attached {@link MediaSessionCompat}
     */
    private void updateMediaSession() {
        requireNotReleased();
        if (mMediaSession == null) {
            return;
        }

        Timber.i("Updating MediaSession");

        if (getNowPlaying() != null) {
            Song nowPlaying = getNowPlaying();
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                            nowPlaying.getSongName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            nowPlaying.getSongName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                            nowPlaying.getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                            nowPlaying.getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                            nowPlaying.getArtistName())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                            nowPlaying.getArtistName())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, mArtwork);
            mMediaSession.setMetadata(metadataBuilder.build());
        }

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);

        if (!mMediaPlayer.getQueue().isEmpty()) {
            state.setActiveQueueItemId(mMediaPlayer.getQueueIndex());
        }

        mMediaSession.setQueueTitle(mContext.getString(R.string.header_now_playing));
        mMediaSession.setQueue(buildQueueWindow());

        if (mMediaPlayer.isPlaying()) {
            state.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), 1f);
        } else if (mMediaPlayer.isPaused()) {
            state.setState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), 1f);
        } else if (mMediaPlayer.isStopped()) {
            state.setState(PlaybackStateCompat.STATE_STOPPED, getCurrentPosition(), 1f);
        } else {
            state.setState(PlaybackStateCompat.STATE_NONE, getCurrentPosition(), 1f);
        }

        if (isShuffled()) {
            mMediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            state.addCustomAction(
                    MediaSessionCallback.ACTION_TOGGLE_SHUFFLE,
                    mContext.getString(R.string.action_disable_shuffle),
                    R.drawable.ic_shuffle_enable_auto
            );
        } else {
            mMediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
            state.addCustomAction(
                    MediaSessionCallback.ACTION_TOGGLE_SHUFFLE,
                    mContext.getString(R.string.action_enable_shuffle),
                    R.drawable.ic_shuffle_disabled_auto
            );
        }

        switch (mRepeat) {
            case REPEAT_ALL:
                mMediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
                break;
            case REPEAT_ONE:
                mMediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
                break;
            case REPEAT_NONE:
            default:
                mMediaSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
        }

        mMediaSession.setPlaybackState(state.build());

        Timber.i("Sending minor broadcast to update UI process");
        Intent broadcast = new Intent(UPDATE_BROADCAST)
                .putExtra(UPDATE_EXTRA_MINOR, true);

        mContext.sendBroadcast(broadcast, BROADCAST_PERMISSION);
    }

    private List<QueueItem> buildQueueWindow() {
        List<Song> queue = mMediaPlayer.getQueue();
        int queueIndex = mMediaPlayer.getQueueIndex();
        int windowSize = Math.min(queue.size(), MEDIA_SESSION_QUEUE_MAX_SIZE);

        int prefixLength = Math.min(queueIndex, MEDIA_SESSION_QUEUE_MAX_SIZE / 2 - 1);
        int startIndex = queueIndex - prefixLength;

        List<QueueItem> window = new ArrayList<>();
        for (int i = startIndex; i < startIndex + windowSize; i++) {
            Song song = queue.get(i);

            window.add(new QueueItem(
                    new MediaDescriptionCompat.Builder()
                            .setTitle(song.getSongName())
                            .setSubtitle(song.getArtistName())
                            .build(),
                    i
            ));
        }
        return window;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Timber.i("AudioFocus changed (%d)", focusChange);

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                Timber.i("Focus lost. Pausing music.");
                mFocused = false;
                mResumeOnFocusGain = false;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Timber.i("Focus lost transiently. Pausing music.");
                boolean resume = isPlaying() || mResumeOnFocusGain;
                mFocused = false;
                pause();
                mResumeOnFocusGain = resume;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Timber.i("Focus lost transiently. Letting system duck.");
                } else {
                    Timber.i("Focus lost transiently. Ducking.");
                    mMediaPlayer.setVolume(DUCK_VOLUME);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Timber.i("Regained AudioFocus");
                mMediaPlayer.setVolume(1f);
                if (mResumeOnFocusGain) play();
                mResumeOnFocusGain = false;
                break;
            default:
                Timber.i("Ignoring AudioFocus state change");
                break;
        }
        updateNowPlaying();
        updateUi();
    }

    @Internal boolean shouldResumeOnHeadphonesConnect() {
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return mResumeOnHeadphonesConnect && (mFocused || !manager.isMusicActive());
    }

    /**
     * Notifies the attached {@link MusicPlayer.OnPlaybackChangeListener} that a playback change
     * has occurred, and updates the attached {@link MediaSessionCompat}
     */
    private void updateNowPlaying() {
        Timber.i("updateNowPlaying() called");
        updateMediaSession();
        if (mCallback != null) {
            mCallback.onPlaybackChange();
        }
    }

    /**
     * Called to notify the UI thread to refresh any player data when the player changes states
     * on its own (Like when a song finishes)
     */
    protected void updateUi() {
        Timber.i("Sending broadcast to update UI process");
        Intent broadcast = new Intent(UPDATE_BROADCAST)
                .putExtra(UPDATE_EXTRA_MINOR, false);

        mContext.sendBroadcast(broadcast, BROADCAST_PERMISSION);
    }

    /**
     * Called to notify the UI thread that an error has occurred. The typical listener will show the
     * message passed in to the user.
     * @param message A user-friendly message associated with this error that may be shown in the UI
     */
    protected void postError(String message) {
        Timber.i("Posting error to UI process: %s", message);
        mContext.sendBroadcast(new Intent(ERROR_BROADCAST).putExtra(ERROR_EXTRA_MSG, message),
                BROADCAST_PERMISSION);
    }

    /**
     * Called to notify the UI thread of a non-critical event. The typical listener will show the
     * message passed in to the user
     * @param message A user-friendly message associated with this event that may be shown in the UI
     */
    protected void postInfo(String message) {
        Timber.i("Posting info to UI process: %s", message);
        mContext.sendBroadcast(new Intent(INFO_BROADCAST).putExtra(INFO_EXTRA_MESSAGE, message),
                BROADCAST_PERMISSION);
    }

    /**
     * Gain Audio focus from the system if we don't already have it
     * @return whether we have gained focus (or already had it)
     */
    private boolean getFocus() {
        if (!mFocused) {
            Timber.i("Requesting AudioFocus...");
            AudioManagerCompat audioManager = AudioManagerCompat.getInstance(mContext);
            mFocused = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return mFocused;
    }

    /**
     * Generates a new random permutation of the queue, and sets it as the backing
     * {@link QueuedMediaPlayer}'s queue
     * @param currentIndex The index of the current song which will be moved to the top of the
     *                     shuffled queue
     * @param seed The seed to use when shuffling the queue
     */
    private void shuffleQueue(int currentIndex, long seed) {
        Timber.i("Shuffling queue...");
        List<Song> shuffled = MusicUtils.generateShuffledQueue(mQueue, currentIndex, seed);
        mQueueShuffled = Collections.unmodifiableList(shuffled);
    }

    /**
     * Called when disabling shuffle to ensure that any modifications made to the shuffled queue
     * are applied to the unshuffled queue. Currently, the only such modification is song deletions
     * since they are implemented on the client side.
     */
    private void unshuffleQueue() {
        List<Song> unshuffled = new ArrayList<>(mQueue);
        List<Song> songs = new ArrayList<>(mQueueShuffled);

        Iterator<Song> unshuffledIterator = unshuffled.iterator();
        while (unshuffledIterator.hasNext()) {
            Song song = unshuffledIterator.next();

            if (!songs.remove(song)) {
                unshuffledIterator.remove();
            }
        }

        mQueue = Collections.unmodifiableList(unshuffled);
    }

    /**
     * Toggles playback between playing and paused states
     * @see #play()
     * @see #pause()
     */
    public void togglePlay() {
        requireNotReleased();
        Timber.i("Toggling playback");
        if (isPlaying()) {
            pause();
        } else if (mMediaPlayer.isComplete()) {
            mMediaPlayer.setQueueIndex(0);
            play();
        } else {
            play();
        }
    }

    /**
     * Pauses music playback
     */
    public void pause() {
        requireNotReleased();
        Timber.i("Pausing playback");
        if (isPlaying()) {
            mMediaPlayer.pause();
            updateNowPlaying();
            for (MusicPlayerExtension ext : mExtensions) {
                ext.onSongPaused(this);
            }
        }
        mResumeOnFocusGain = false;
    }

    /**
     * Starts or resumes music playback
     */
    public void play() {
        requireNotReleased();
        Timber.i("Resuming playback");
        if (!isPlaying() && getFocus()) {
            mMediaPlayer.play();
            updateNowPlaying();
            for (MusicPlayerExtension ext : mExtensions) {
                ext.onSongResumed(this);
            }
        }
    }

    /**
     * Skips to the next song in the queue and logs a play count or skip count
     * If repeat all is enabled, the queue will loop from the beginning when it it is finished.
     * Otherwise, calling this method when the last item in the queue is being played will stop
     * playback
     * @see #setRepeat(int) to set the current repeat mode
     */
    public void skip() {
        requireNotReleased();
        Timber.i("Skipping song");
        boolean skippedByUser = !mMediaPlayer.isComplete() && !mMediaPlayer.hasError();
        for (MusicPlayerExtension ext : mExtensions) {
            ext.onSongSkipped(this, skippedByUser);
        }

        setMultiRepeat(0);

        if (mMediaPlayer.getQueueIndex() < mQueue.size() - 1
                || mRepeat == REPEAT_ALL) {
            // If we're in the middle of the queue, or repeat all is on, start the next song
            mMediaPlayer.skip();
        } else {
            mMediaPlayer.setQueueIndex(0);
            mMediaPlayer.pause();
        }
    }

    /**
     * Skips to the previous song in the queue
     * If the song's current position is more than 5 seconds or 50% of the song (whichever is
     * smaller), then the song will be restarted from the beginning instead.
     * If this is called when the first item in the queue is being played, it will loop to the last
     * song if repeat all is enabled, otherwise the current song will always be restarted
     * @see #setRepeat(int) to set the current repeat mode
     */
    public void skipPrevious() {
        requireNotReleased();
        Timber.i("skipPrevious() called");
        if ((getQueuePosition() == 0 && mRepeat != REPEAT_ALL)
                || getCurrentPosition() > SKIP_PREVIOUS_THRESHOLD
                || getCurrentPosition() > getDuration() / 2) {
            Timber.i("Restarting current song...");
            mMediaPlayer.seekTo(0);
            mMediaPlayer.play();
            updateNowPlaying();
        } else {
            Timber.i("Starting previous song...");
            mMediaPlayer.skipPrevious();
        }
    }

    /**
     * Stops music playback
     */
    public void stop() {
        requireNotReleased();
        Timber.i("stop() called");
        pause();
        if (mCallback != null) {
            mCallback.onPlaybackStop();
        }
    }

    /**
     * Seek to a specified position in the current song
     * @param mSec The time (in milliseconds) to seek to
     * @see MediaPlayer#seekTo(int)
     */
    public void seekTo(int mSec) {
        Timber.i("Seeking to %d", mSec);
        mMediaPlayer.seekTo(mSec);

        for (MusicPlayerExtension ext : mExtensions) {
            ext.onSeekPositionChanged(this);
        }
    }

    /**
     * @return The {@link Song} that is currently being played
     */
    public Song getNowPlaying() {
        requireNotReleased();
        return mMediaPlayer.getNowPlaying();
    }

    /**
     * @return Whether music is being played or not
     * @see MediaPlayer#isPlaying()
     */
    public boolean isPlaying() {
        requireNotReleased();
        return mMediaPlayer.isPlaying();
    }

    /**
     * @return The current queue. If shuffle is enabled, then the shuffled queue will be returned,
     *         otherwise the regular queue will be returned
     */
    public List<Song> getQueue() {
        requireNotReleased();
        // If you're using this method on the UI thread, consider replacing this method with
        // return new ArrayList<>(mMediaPlayer.getQueue());
        // to prevent components from accidentally changing the backing queue
        return mMediaPlayer.getQueue();
    }

    /**
     * @return The current index in the queue that is being played
     */
    public int getQueuePosition() {
        requireNotReleased();
        return mMediaPlayer.getQueueIndex();
    }

    /**
     * @return The number of items in the current queue
     */
    public int getQueueSize() {
        requireNotReleased();
        return mMediaPlayer.getQueueSize();
    }

    /**
     * @return The current seek position of the song that is playing
     * @see MediaPlayer#getCurrentPosition()
     */
    public int getCurrentPosition() {
        requireNotReleased();
        return mMediaPlayer.getCurrentPosition();
    }

    /**
     * @return The length of the current song in milliseconds
     * @see MediaPlayer#getDuration()
     */
    public int getDuration() {
        requireNotReleased();
        return mMediaPlayer.getDuration();
    }

    /**
     * Changes the current index of the queue and starts playback from this new position
     * @param position The index in the queue to skip to
     * @throws IllegalArgumentException if {@code position} is not between 0 and the queue length
     */
    public void changeSong(int position) {
        requireNotReleased();
        Timber.i("changeSong called (position = %d)", position);
        mMediaPlayer.setQueueIndex(position);
        play();
    }

    /**
     * Changes the current queue and starts playback from the specified index
     * @param queue The replacement queue
     * @param index The index to start playback from
     * @param seed A seed to use when shuffling the queue (only used if shuffle is enabled)
     * @throws IllegalArgumentException if {@code index} is not between 0 and the queue length
     */
    public void setQueue(@NonNull List<Song> queue, int index, long seed) {
        requireNotReleased();
        Timber.i("setQueue called (%d songs)", queue.size());
        // If you're using this method on the UI thread, consider replacing the first line in this
        // method with "mQueue = new ArrayList<>(queue);"
        // to prevent components from accidentally changing the backing queue
        mQueue = Collections.unmodifiableList(queue);
        if (mShuffle) {
            Timber.i("Shuffling new queue and starting from beginning");
            shuffleQueue(index, seed);
            setBackingQueue(0, true);
        } else {
            Timber.i("Setting new backing queue (starting at index %d)", index);
            setBackingQueue(index, true);
        }
    }

    /**
     * Changes the order of the current queue without interrupting playback
     * @param queue The modified queue. This List should contain all of the songs currently in the
     *              queue, but in a different order to prevent discrepancies between the shuffle
     *              and non-shuffled queue.
     * @param index The index of the song that is currently playing in the modified queue
     */
    public void editQueue(@NonNull List<Song> queue, int index) {
        requireNotReleased();
        Timber.i("editQueue called (index = %d)", index);
        if (mShuffle) {
            mQueueShuffled = Collections.unmodifiableList(queue);
        } else {
            mQueue = Collections.unmodifiableList(queue);
        }
        setBackingQueue(index, false);
    }

    /**
     * Helper method to push changes in the queue to the backing {@link QueuedMediaPlayer}
     * @see #setBackingQueue(int, boolean)
     */
    private void setBackingQueue(boolean resetSeekPosition) {
        Timber.i("setBackingQueue() called");
        setBackingQueue(mMediaPlayer.getQueueIndex(), resetSeekPosition);
    }

    /**
     * Helper method to push changes in the queue to the backing {@link QueuedMediaPlayer}. This
     * method will set the queue to the appropriate shuffled or ordered list and apply the
     * specified index as the replacement queue position
     * @param index The new queue index to send to the backing {@link QueuedMediaPlayer}.
     */
    private void setBackingQueue(int index, boolean resetSeekPosition) {
        Timber.i("setBackingQueue() called (index = %d)", index);
        if (mShuffle) {
            mMediaPlayer.setQueue(mQueueShuffled, index, resetSeekPosition);
        } else {
            mMediaPlayer.setQueue(mQueue, index, resetSeekPosition);
        }

        for (MusicPlayerExtension ext : mExtensions) {
            ext.onQueueChanged(this);
        }
    }

    /**
     * Sets the repeat option to control what happens when a track finishes.
     * @param repeat An integer representation of the repeat option. May be one of either
     *               {@link #REPEAT_NONE}, {@link #REPEAT_ALL}, {@link #REPEAT_ONE}.
     */
    public void setRepeat(int repeat) {
        requireNotReleased();
        if (repeat == mRepeat) {
            return;
        }

        Timber.i("Changing repeat setting to %d", repeat);
        mRepeat = repeat;
        switch (repeat) {
            case REPEAT_ALL:
                mMediaPlayer.enableRepeatAll();
                break;
            case REPEAT_ONE:
                mMediaPlayer.enableRepeatOne();
                break;
            case REPEAT_NONE:
            default:
                mMediaPlayer.enableRepeatNone();
        }
        updateMediaSession();
        updateUi();
    }

    /**
     * Gets the current repeat option.
     * @return An integer representation of the repeat option. May be one of either
     *         {@link #REPEAT_NONE}, {@link #REPEAT_ALL}, or {@link #REPEAT_ONE}.
     */
    public int getRepeatMode() {
        requireNotReleased();
        return mRepeat;
    }

    /**
     * Sets the Multi-Repeat counter to repeat a song {@code count} times before proceeding to the
     * next song
     * @param count The number of times to repeat the song. When multi-repeat is enabled, the
     *              current song will be played back-to-back for the specified number of loops.
     *              Once this counter decrements to 0, playback will resume as it was before and the
     *              previous repeat option will be restored unless it was previously Repeat All. If
     *              Repeat All was enabled before Multi-Repeat, the repeat setting will be reset to
     *              Repeat none.
     */
    public void setMultiRepeat(int count) {
        requireNotReleased();
        Timber.i("Changing Multi-Repeat counter to %d", count);
        mMultiRepeat = count;
        mRemotePreferenceStore.setMultiRepeatCount(count);
        if (count > 1) {
            mMediaPlayer.enableRepeatOne();
        } else {
            setRepeat(mRepeat);
        }
    }

    /**
     * Gets the current Multi-Repeat status
     * @return The number of times that the current song will be played back-to-back. This is
     *         decremented when the song finishes. If Multi-Repeat is disabled, this method
     *         will return {@code 0}.
     */
    public int getMultiRepeatCount() {
        requireNotReleased();
        return mMultiRepeat;
    }

    /**
     * Enables or updates the sleep timer to pause music at a specified timestamp
     * @param endTimestampInMillis The timestamp to pause music. This is in milliseconds since the
     *                             Unix epoch as returned by {@link System#currentTimeMillis()}.
     */
    public void setSleepTimer(long endTimestampInMillis) {
        requireNotReleased();
        Timber.i("Changing sleep timer end time to %d", endTimestampInMillis);
        startSleepTimer(endTimestampInMillis);
        mRemotePreferenceStore.setSleepTimerEndTime(endTimestampInMillis);
    }

    /**
     * Internal method for setting up the system timer to pause music.
     * @param endTimestampInMillis The timestamp to pause music in milliseconds since the Unix epoch
     * @see #setSleepTimer(long)
     */
    private void startSleepTimer(long endTimestampInMillis) {
        if (endTimestampInMillis <= System.currentTimeMillis()) {
            Timber.i("Sleep timer end time (%1$d) is in the past (currently %2$d). Stopping timer",
                    endTimestampInMillis, System.currentTimeMillis());
            mHandler.removeCallbacks(mSleepTimerRunnable);
        } else {
            long delay = endTimestampInMillis - System.currentTimeMillis();
            Timber.i("Setting sleep timer for %d ms", delay);
            mHandler.postDelayed(mSleepTimerRunnable, delay);
        }
    }

    /**
     * Internal method called once the sleep timer is triggered.
     * @see #setSleepTimer(long) to set the sleep timer
     * @see #startSleepTimer(long) for the sleep timer setup
     */
    private void onSleepTimerEnd() {
        Timber.i("Sleep timer ended.");
        pause();
        updateUi();

        postInfo(mContext.getString(R.string.confirm_sleep_timer_end));
    }

    /**
     * Gets the current end time of the sleep timer.
     * @return The current end time of the sleep timer in milliseconds since the Unix epoch. This
     *         method returns {@code 0} if the sleep timer is disabled.
     */
    public long getSleepTimerEndTime() {
        requireNotReleased();
        return mRemotePreferenceStore.getSleepTimerEndTime();
    }

    /**
     * Overload for {@link #setShuffle(boolean, long)} that uses a default seed generator if
     * enabling shuffle.
     */
    public void setShuffle(boolean shuffle) {
        setShuffle(shuffle, System.currentTimeMillis());
    }

    /**
     * Sets the shuffle option and immediately applies it to the queue
     * @param shuffle The new shuffle option. {@code true} will switch the current playback to a
     *                copy of the current queue in a randomized order. {@code false} will restore
     *                the queue to its original order.
     * @param seed A seed to use when shuffling (only used if shuffle is {@code true})
     */
    public void setShuffle(boolean shuffle, long seed) {
        requireNotReleased();
        if (shuffle == mShuffle) {
            return;
        }

        mShuffle = shuffle;
        if (shuffle) {
            Timber.i("Enabling shuffle...");
            shuffleQueue(getQueuePosition(), seed);
            setBackingQueue(0, false);
        } else {
            Timber.i("Disabling shuffle...");
            unshuffleQueue();
            setBackingQueue(mQueue.indexOf(getNowPlaying()), false);
        }
        updateUi();
        updateMediaSession();
        updateNowPlaying();
    }

    public boolean isShuffled() {
        return mShuffle;
    }

    /**
     * Adds a {@link Song} to the queue to be played after the current song
     * @param song the song to enqueue
     */
    public void queueNext(Song song) {
        requireNotReleased();
        Timber.i("queueNext(Song) called");
        int index = mQueue.isEmpty() ? 0 : mMediaPlayer.getQueueIndex() + 1;

        List<Song> shuffledQueue = new ArrayList<>(mQueueShuffled);
        List<Song> queue = new ArrayList<>(mQueue);

        if (mShuffle) {
            shuffledQueue.add(index, song);
            queue.add(song);
        } else {
            queue.add(index, song);
        }

        mQueueShuffled = Collections.unmodifiableList(shuffledQueue);
        mQueue = Collections.unmodifiableList(queue);

        setBackingQueue(false);
    }

    /**
     * Adds a {@link List} of {@link Song}s to the queue to be played after the current song
     * @param songs The songs to enqueue
     */
    public void queueNext(List<Song> songs) {
        requireNotReleased();
        Timber.i("queueNext(List<Song>) called");
        int index = mQueue.isEmpty() ? 0 : mMediaPlayer.getQueueIndex() + 1;

        List<Song> shuffledQueue = new ArrayList<>(mQueueShuffled);
        List<Song> queue = new ArrayList<>(mQueue);

        if (mShuffle) {
            shuffledQueue.addAll(index, songs);
            queue.addAll(songs);
        } else {
            queue.addAll(index, songs);
        }

        mQueueShuffled = Collections.unmodifiableList(shuffledQueue);
        mQueue = Collections.unmodifiableList(queue);

        setBackingQueue(false);
    }

    /**
     * Adds a {@link Song} to the end of the queue
     * @param song The song to enqueue
     */
    public void queueLast(Song song) {
        requireNotReleased();
        Timber.i("queueLast(Song) called");

        List<Song> shuffledQueue = new ArrayList<>(mQueueShuffled);
        List<Song> queue = new ArrayList<>(mQueue);

        if (mShuffle) {
            shuffledQueue.add(song);
            queue.add(song);
        } else {
            queue.add(song);
        }

        mQueueShuffled = Collections.unmodifiableList(shuffledQueue);
        mQueue = Collections.unmodifiableList(queue);

        setBackingQueue(false);
    }

    /**
     * Adds a {@link List} of {@link Song}s to the end of the queue
     * @param songs The songs to enqueue
     */
    public void queueLast(List<Song> songs) {
        requireNotReleased();
        Timber.i("queueLast(List<Song>)");

        List<Song> shuffledQueue = new ArrayList<>(mQueueShuffled);
        List<Song> queue = new ArrayList<>(mQueue);

        if (mShuffle) {
            shuffledQueue.addAll(songs);
            queue.addAll(songs);
        } else {
            queue.addAll(songs);
        }

        mQueueShuffled = Collections.unmodifiableList(shuffledQueue);
        mQueue = Collections.unmodifiableList(queue);

        setBackingQueue(false);
    }

    /**
     * Releases all resources and bindings associated with this MusicPlayer.
     * Once this is called, this MusicPlayer can no longer be used.
     */
    public void release() {
        requireNotReleased();
        Timber.i("release() called");
        AudioManagerCompat.getInstance(mContext).abandonAudioFocus(this);
        mContext.unregisterReceiver(mHeadphoneListener);

        // Make sure to disable the sleep timer to purge any delayed runnables in the message queue
        startSleepTimer(0);

        mFocused = false;
        mCallback = null;
        mMediaPlayer.release();
        mMediaSession.release();
        mMediaPlayer = null;
        mContext = null;
    }

    public boolean isReleased() {
        return mMediaPlayer == null;
    }

    private void requireNotReleased() {
        if (isReleased()) {
            throw new IllegalStateException("MusicPlayer has been released");
        }
    }

    protected MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    /**
     * Updates the options for all connected {@link MusicPlayerExtension MusicPlayerExtensions}.
     * Every extension will receive this bundle.
     * @param options The new options to be sent to each player extension.
     */
    public void updateExtensionOptions(Bundle options) {
        for (MusicPlayerExtension ext : mExtensions) {
            ext.onOptionsChange(options);
        }
    }

    @Override
    public void onCompletion(Song completed) {
        Timber.i("onCompletion called");
        for (MusicPlayerExtension ext : mExtensions) {
            ext.onSongCompleted(this, completed);
        }

        if (mMultiRepeat > 1) {
            Timber.i("Multi-Repeat (%d) is enabled. Restarting current song and decrementing.",
                    mMultiRepeat);

            setMultiRepeat(mMultiRepeat - 1);
            updateNowPlaying();
            updateUi();
        } else if (mMediaPlayer.isComplete()) {
            updateNowPlaying();
            updateUi();
        }
    }

    @Override
    public void onSongStart() {
        Timber.i("Started new song");
        for (MusicPlayerExtension ext : mExtensions) {
            ext.onSongStarted(this);
        }
        ArtworkUtils.fetchArtwork(mContext, getNowPlaying().getLocation())
                .subscribeOn(Schedulers.io())
                .subscribe(artwork -> {
                    mArtwork = artwork;
                    updateNowPlaying();
                }, throwable -> {
                    Timber.e(throwable, "Failed to load artwork");
                    mArtwork = null;
                    if (!isReleased()) {
                        updateNowPlaying();
                    }
                });

        updateUi();
    }

    @Override
    public boolean onError(Throwable error) {
        Timber.i(error, "Sending error message to UI...");
        postError(mContext.getString(
                R.string.message_play_error_io_exception,
                getNowPlaying().getSongName()));
        if (mQueue.size() > 1) {
            skip();
        } else {
            stop();
        }
        return true;
    }

    /**
     * Creates a snapshot of the current player state including the state of the queue,
     * seek position, playing status, etc. This is useful for undoing modifications to the state.
     * @return A {@link PlayerState} object with the current status of this MusicPlayer instance.
     * @see #restorePlayerState(PlayerState) To restore this state
     */
    public PlayerState getState() {
        return new PlayerState.Builder()
                .setPlaying(isPlaying())
                .setQueuePosition(getQueuePosition())
                .setQueue(mQueue)
                .setShuffledQueue(mQueueShuffled)
                .setSeekPosition(getCurrentPosition())
                .build();
    }

    /**
     * Restores a player state created from {@link #getState()}.
     * @param state The state to be restored. All properties including seek position and playing
     *              status will immediately be applied.
     */
    public void restorePlayerState(PlayerState state) {
        mQueue = Collections.unmodifiableList(state.getQueue());
        mQueueShuffled = Collections.unmodifiableList(state.getShuffledQueue());

        setBackingQueue(state.getQueuePosition(), false);
        seekTo(state.getSeekPosition());

        if (state.isPlaying()) {
            play();
        } else {
            pause();
        }

        updateNowPlaying();
        updateUi();
    }

    /**
     * A callback for receiving information about song changes -- useful for integrating
     * {@link MusicPlayer} with other components
     */
    public interface OnPlaybackChangeListener {
        /**
         * Called when a MusicPlayer changes songs. This method will always be called, even if the
         * event was caused by an external source. Implement and attach this callback to provide
         * more integration with external sources which requires up-to-date song information
         * (i.e. to post a notification)
         *
         * This method will only be called after the current song changes -- not when the
         * {@link MediaPlayer} changes states.
         */
        void onPlaybackChange();

        /**
         * Called when a MusicPlayer stops playback. This method will always be called, even if the
         * event was caused by an external source. This method should be implemented to handle
         * any side effects of a MusicPlayer being stopped, which may happen due to user interaction
         * from other components of the system.
         */
        void onPlaybackStop();
    }

    private static class MediaSessionCallback extends MediaSessionCompat.Callback {

        private static final String ACTION_TOGGLE_SHUFFLE = "shuffle";

        /**
         * A period of time added after a remote button press to delay handling the event. This
         * delay allows the user to press the remote button multiple times to execute different
         * actions
         */
        private static final int REMOTE_CLICK_SLEEP_TIME_MS = 300;

        private int mClickCount;

        private MusicPlayer mMusicPlayer;
        private MediaBrowserRoot mBrowserRoot;
        private Handler mHandler;

        MediaSessionCallback(MusicPlayer musicPlayer, MediaBrowserRoot browserRoot) {
            mHandler = new Handler();
            mMusicPlayer = musicPlayer;
            mBrowserRoot = browserRoot;
        }

        private final Runnable mButtonHandler = () -> {
            if (mMusicPlayer.isReleased()) return;

            if (mClickCount == 1) {
                mMusicPlayer.togglePlay();
                mMusicPlayer.updateUi();
            } else if (mClickCount == 2) {
                onSkipToNext();
            } else {
                onSkipToPrevious();
            }
            mClickCount = 0;
        };

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                if (keyEvent.getAction() == KeyEvent.ACTION_UP && !keyEvent.isLongPress()) {
                    onRemoteClick();
                }
                return true;
            } else {
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        }

        private void onRemoteClick() {
            mClickCount++;
            mHandler.removeCallbacks(mButtonHandler);
            mHandler.postDelayed(mButtonHandler, REMOTE_CLICK_SLEEP_TIME_MS);
        }

        @Override
        public void onPlay() {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.play();
            mMusicPlayer.updateUi();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.changeSong((int) id);
            mMusicPlayer.updateUi();
        }

        @Override
        public void onPause() {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.pause();
            mMusicPlayer.updateUi();
        }

        @Override
        public void onSkipToNext() {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.skip();
            mMusicPlayer.updateUi();
        }

        @Override
        public void onSkipToPrevious() {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.skipPrevious();
            mMusicPlayer.updateUi();
        }

        @Override
        public void onStop() {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.stop();
            // Don't update the UI if this object has been released
            if (mMusicPlayer.mContext != null) {
                mMusicPlayer.updateUi();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            if (mMusicPlayer.isReleased()) return;

            mMusicPlayer.seekTo((int) pos);
            mMusicPlayer.updateUi();
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            if (mMusicPlayer.isReleased()) return;

            switch (repeatMode) {
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    mMusicPlayer.setRepeat(REPEAT_ALL);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    mMusicPlayer.setRepeat(REPEAT_NONE);
                    break;
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    mMusicPlayer.setRepeat(REPEAT_ONE);
                    break;
            }
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            if (mMusicPlayer.isReleased()) return;

            switch (shuffleMode) {
                case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                    mMusicPlayer.setShuffle(true, System.currentTimeMillis());
                    break;
                case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                    mMusicPlayer.setShuffle(false, 0);
                    break;
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            mBrowserRoot.getQueue(mediaId)
                    .subscribe(
                            this::applyMediaList,
                            e -> Timber.e(e, "Failed to play media from ID %s", mediaId));
        }

        private void applyMediaList(MediaList tracks) {
            if (tracks != null) {
                if (!tracks.keepCurrentQueue && tracks.songs != null) {
                    mMusicPlayer.setQueue(tracks.songs, tracks.startIndex, System.currentTimeMillis());
                } else {
                    mMusicPlayer.changeSong(tracks.startIndex);
                }

                if (!mMusicPlayer.isShuffled() && tracks.enableShuffle) {
                    mMusicPlayer.setShuffle(true, System.currentTimeMillis());
                }

                mMusicPlayer.play();
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            switch (action) {
                case ACTION_TOGGLE_SHUFFLE:
                    mMusicPlayer.setShuffle(!mMusicPlayer.isShuffled(), System.currentTimeMillis());
                    break;
            }
        }
    }

    /**
     * Receives headphone connect and disconnect intents so that music may be paused when headphones
     * are disconnected
     */
    public static class HeadsetListener extends BroadcastReceiver {

        private MusicPlayer mInstance;

        public HeadsetListener(MusicPlayer instance) {
            mInstance = instance;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInitialStickyBroadcast()) {
                return;
            }

            Timber.i("onReceive: %s", intent);

            boolean plugged, unplugged;

            if (ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                unplugged = intent.getIntExtra("state", -1) == 0;
                plugged = intent.getIntExtra("state", -1) == 1;
            } else {
                unplugged = plugged = false;
            }

            boolean becomingNoisy = ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction());

            if (unplugged || becomingNoisy) {
                mInstance.pause();
                mInstance.updateUi();
            } else if (plugged && mInstance.shouldResumeOnHeadphonesConnect()) {
                mInstance.play();
                mInstance.updateUi();
            }
        }
    }

}
