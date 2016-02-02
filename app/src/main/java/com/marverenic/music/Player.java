package com.marverenic.music;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.ManagedMediaPlayer;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

public class Player implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener {

    // Sent to refresh views that use up-to-date player information
    public static final String UPDATE_BROADCAST = "marverenic.jockey.player.REFRESH";
    public static final String ERROR_BROADCAST = "marverenic.jockey.player.ERROR";
    public static final String ERROR_EXTRA_MSG = "marverenic.jockey.player.ERROR:MSG";
    private static final String TAG = "Player";
    private static final String QUEUE_FILE = ".queue";

    public static final String PREFERENCE_SHUFFLE = "prefShuffle";
    public static final String PREFERENCE_REPEAT = "prefRepeat";

    // Instance variables
    private ManagedMediaPlayer mediaPlayer;
    private Equalizer equalizer;
    private Context context;
    private MediaSessionCompat mediaSession;
    private HeadsetListener headphoneListener;

    // Queue information
    private List<Song> queue;
    private List<Song> queueShuffled = new ArrayList<>();
    private int queuePosition;
    private int queuePositionShuffled;

    // MediaFocus variables
    // If we currently have audio focus
    private boolean active = false;
    // If we should play music when focus is returned
    private boolean shouldResumeOnFocusGained = false;

    // Shufle & Repeat options
    private boolean shuffle; // Shuffle status
    public static final short REPEAT_NONE = 0;
    public static final short REPEAT_ALL = 1;
    public static final short REPEAT_ONE = 2;
    private short repeat; // Repeat status

    private Bitmap art; // The art for the current song

    /**
     * A {@link Properties} object used as a hashtable for saving play and skip counts.
     * See {@link Player#logPlayCount(long, boolean)}
     *
     * Keys are stored as strings in the form "song_id"
     * Values are stored as strings in the form "play,skip,lastPlayDateAsUtcTimeStamp"
     */
    private Properties playCountHashtable;

    /**
     * Create a new Player Object, which manages a {@link MediaPlayer}
     * @param context A {@link Context} that will be held for the lifetime of the Player
     */
    public Player(Context context) {
        this.context = context;

        // Initialize the media player
        mediaPlayer = new ManagedMediaPlayer();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);

        // Initialize the queue
        queue = new ArrayList<>();
        queuePosition = 0;

        // Load preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        shuffle = prefs.getBoolean(PREFERENCE_SHUFFLE, false);
        repeat = (short) prefs.getInt(PREFERENCE_REPEAT, REPEAT_NONE);

        initMediaSession();
        if (Util.hasEqualizer()) {
            initEqualizer();
        }

        // Attach a HeadsetListener to respond to headphone events
        headphoneListener = new HeadsetListener(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BUTTON);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(headphoneListener, filter);
    }

    /**
     * Reload the last queue saved by the Player
     */
    public void reload() {
        try {
            File save = new File(context.getExternalFilesDir(null), QUEUE_FILE);
            Scanner scanner = new Scanner(save);

            final int currentPosition = scanner.nextInt();

            if (shuffle) {
                queuePositionShuffled = scanner.nextInt();
            } else {
                queuePosition = scanner.nextInt();
            }

            int queueLength = scanner.nextInt();
            int[] queueIDs = new int[queueLength];
            for (int i = 0; i < queueLength; i++) {
                queueIDs[i] = scanner.nextInt();
            }
            queue = Library.buildSongListFromIds(queueIDs, context);

            int[] shuffleQueueIDs;
            if (scanner.hasNextInt()) {
                shuffleQueueIDs = new int[queueLength];
                for (int i = 0; i < queueLength; i++) {
                    shuffleQueueIDs[i] = scanner.nextInt();
                }
                queueShuffled = Library.buildSongListFromIds(shuffleQueueIDs, context);
            } else if (shuffle) {
                queuePosition = queuePositionShuffled;
                shuffleQueue();
            }

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.seekTo(currentPosition);
                    updateNowPlaying();
                    mp.setOnPreparedListener(Player.this);
                }
            });

            art = Util.fetchFullArt(getNowPlaying());
            mediaPlayer.setDataSource(getNowPlaying().getLocation());
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            queuePosition = 0;
            queuePositionShuffled = 0;
            queue.clear();
            queueShuffled.clear();
        }
    }

    /**
     * Reload all equalizer settings from SharedPreferences
     */
    private void initEqualizer() {
        SharedPreferences prefs = Prefs.getPrefs(context);
        String eqSettings = prefs.getString(Prefs.EQ_SETTINGS, null);
        boolean enabled = Prefs.getPrefs(context).getBoolean(Prefs.EQ_ENABLED, false);

        equalizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
        if (eqSettings != null) {
            try {
                equalizer.setProperties(new Equalizer.Settings(eqSettings));
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                Crashlytics.logException(new RuntimeException(
                        "Failed to load equalizer settings: " + eqSettings, e));
            }
        }
        equalizer.setEnabled(enabled);

        // If the built in equalizer is off, bind to the system equalizer if one is available
        if (!enabled) {
            final Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.sendBroadcast(intent);
        }
    }

    /**
     * Writes a player state to disk. Contains information about the queue (both unshuffled
     * and shuffled), current queuePosition within this list, and the current queuePosition
     * of the song
     * @throws IOException
     */
    public void saveState(@NonNull final String nextCommand) throws IOException {
        // Anticipate the outcome of a command so that if we're killed right after it executes,
        // we can restore to the proper state
        int reloadSeekPosition;
        int reloadQueuePosition = (shuffle) ? queuePositionShuffled : queuePosition;

        switch (nextCommand) {
            case PlayerService.ACTION_NEXT:
                if (reloadQueuePosition + 1 < queue.size()) {
                    reloadSeekPosition = 0;
                    reloadQueuePosition++;
                } else {
                    reloadSeekPosition = mediaPlayer.getDuration();
                }
                break;
            case PlayerService.ACTION_PREV:
                if (mediaPlayer.getDuration() < 5000 && reloadQueuePosition - 1 > 0) {
                    reloadQueuePosition--;
                }
                reloadSeekPosition = 0;
                break;
            default:
                reloadSeekPosition = mediaPlayer.getCurrentPosition();
                break;
        }

        final String currentPosition = Integer.toString(reloadSeekPosition);
        final String queuePosition = Integer.toString(reloadQueuePosition);
        final String queueLength = Integer.toString(queue.size());

        String queue = "";
        for (Song s : this.queue) {
            queue += " " + s.getSongId();
        }

        String queueShuffled = "";
        for (Song s : this.queueShuffled) {
            queueShuffled += " " + s.getSongId();
        }

        String output = currentPosition + " " + queuePosition + " "
                + queueLength + queue + queueShuffled;

        File save = new File(context.getExternalFilesDir(null), QUEUE_FILE);
        FileOutputStream stream = new FileOutputStream(save);
        stream.write(output.getBytes());
        stream.close();
    }

    /**
     * Release the player. Call when finished with an instance
     */
    public void finish() {
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
        context.unregisterReceiver(headphoneListener);

        // Unbind from the system audio effects
        final Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);

        if (equalizer != null) {
            equalizer.release();
        }
        active = false;
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaSession.release();
        mediaPlayer = null;
        context = null;
    }

    /**
     * Initiate a MediaSession to allow the Android system to interact with the player
     */
    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(context, TAG, null, null);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
                updateUi();
            }

            @Override
            public void onSkipToQueueItem(long id) {
                changeSong((int) id);
                updateUi();
            }

            @Override
            public void onPause() {
                pause();
                updateUi();
            }

            @Override
            public void onSkipToNext() {
                skip();
                updateUi();
            }

            @Override
            public void onSkipToPrevious() {
                previous();
                updateUi();
            }

            @Override
            public void onStop() {
                stop();
                updateUi();
            }

            @Override
            public void onSeekTo(long pos) {
                seek((int) pos);
                updateUi();
            }
        });
        mediaSession.setSessionActivity(
                PendingIntent.getActivity(
                        context, 0,
                        new Intent(context, NowPlayingActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_CANCEL_CURRENT));

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO
                        | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f);

        mediaSession.setPlaybackState(state.build());
        mediaSession.setActive(true);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        shouldResumeOnFocusGained = isPlaying() || shouldResumeOnFocusGained;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaPlayer.setVolume(0.5f, 0.5f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                mediaPlayer.setVolume(1f, 1f);
                if (shouldResumeOnFocusGained) play();
                shouldResumeOnFocusGained = false;
                break;
            default:
                break;
        }
        updateNowPlaying();
        updateUi();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        logPlayCount(getNowPlaying().getSongId(), false);
        if (repeat == REPEAT_ONE) {
            mediaPlayer.seekTo(0);
            play();
        } else if (hasNextInQueue(1)) {
            skip();
        }
        updateUi();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!isPreparing()) {
            mediaPlayer.start();
            updateUi();
            updateNowPlaying();
        }
    }

    private boolean hasNextInQueue(int by) {
        if (shuffle) {
            return queuePositionShuffled + by < queueShuffled.size();
        } else {
            return queuePosition + by < queue.size();
        }
    }

    private void setQueuePosition(int position) {
        if (shuffle) {
            queuePositionShuffled = position;
        } else {
            queuePosition = position;
        }
    }

    private void incrementQueuePosition(int by) {
        if (shuffle) {
            queuePositionShuffled += by;
        } else {
            queuePosition += by;
        }
    }

    /**
     * Change the queue and shuffle it if necessary
     * @param newQueue A {@link List} of {@link Song}s to become the new queue
     * @param newPosition The queuePosition in the list to start playback from
     */
    public void setQueue(final List<Song> newQueue, final int newPosition) {
        queue = newQueue;
        queuePosition = newPosition;
        if (shuffle) shuffleQueue();
        updateNowPlaying();
    }

    /**
     * Replace the contents of the queue without affecting playback
     * @param newQueue A {@link List} of {@link Song}s to become the new queue
     * @param newPosition The queuePosition in the list to start playback from
     */
    public void editQueue(final List<Song> newQueue, final int newPosition) {
        if (shuffle) {
            queueShuffled = newQueue;
            queuePositionShuffled = newPosition;
        } else {
            queue = newQueue;
            queuePosition = newPosition;
        }
        updateNowPlaying();
    }

    /**
     * Begin playback of a new song. Call this method after changing the queue or now playing track
     */
    public void begin() {
        if (getNowPlaying() != null && getFocus()) {
            mediaPlayer.stop();
            mediaPlayer.reset();

            art = Util.fetchFullArt(getNowPlaying());

            try {
                File source = new File(getNowPlaying().getLocation());
                mediaPlayer.setDataSource(context, Uri.fromFile(source));
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                Crashlytics.logException(
                        new IOException("Failed to play song " + getNowPlaying().getLocation(), e));

                postError(context.getString(
                        (e instanceof FileNotFoundException)
                                ? R.string.message_play_error_not_found
                                : R.string.message_play_error_io_exception,
                        getNowPlaying().getSongName()));
            }
        }
    }

    /**
     * Update the main thread and Android System about this player instance. This method also calls
     * {@link PlayerService#notifyNowPlaying()}.
     */
    public void updateNowPlaying() {
        PlayerService.getInstance().notifyNowPlaying();
        updateMediaSession();
    }

    /**
     * Update the MediaSession to keep the Android system up to date with player information
     */
    public void updateMediaSession() {
        if (getNowPlaying() != null) {
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
            Song nowPlaying = getNowPlaying();
            metadataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                            nowPlaying.getSongName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                            nowPlaying.getSongName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                            nowPlaying.getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                            nowPlaying.getArtistName())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art);
            mediaSession.setMetadata(metadataBuilder.build());

            PlaybackStateCompat.Builder state = new PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SEEK_TO
                            | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

            switch (mediaPlayer.getState()) {
                case STARTED:
                    state.setState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition(), 1f);
                    break;
                case PAUSED:
                    state.setState(PlaybackStateCompat.STATE_PAUSED, getCurrentPosition(), 1f);
                    break;
                case STOPPED:
                    state.setState(PlaybackStateCompat.STATE_STOPPED, getCurrentPosition(), 1f);
                    break;
                default:
                    state.setState(PlaybackStateCompat.STATE_NONE, getCurrentPosition(), 1f);
            }
            mediaSession.setPlaybackState(state.build());
            mediaSession.setActive(active);
        }
    }

    /**
     * Called to notify the UI thread to refresh any player data when the player changes states
     * on its own (Like when a song finishes)
     */
    public void updateUi() {
        context.sendBroadcast(new Intent(UPDATE_BROADCAST), null);
    }

    /**
     * Called to notify the UI thread that an error has occurred. The typical listener will show the
     * message passed in to the user.
     * @param message A user-friendly message associated with this error that may be shown in the UI
     */
    public void postError(String message) {
        context.sendBroadcast(new Intent(ERROR_BROADCAST).putExtra(ERROR_EXTRA_MSG, message), null);
    }

    /**
     * Get the song at the current queuePosition in the queue or shuffled queue
     * @return The now playing {@link Song} (null if nothing is playing)
     */
    public Song getNowPlaying() {
        if (shuffle) {
            if (queueShuffled.size() == 0 || queuePositionShuffled >= queueShuffled.size()
                    || queuePositionShuffled < 0) {
                return null;
            }
            return queueShuffled.get(queuePositionShuffled);
        }
        if (queue.size() == 0 || queuePosition >= queue.size() || queuePosition < 0) {
            return null;
        }
        return queue.get(queuePosition);
    }

    //
    //      MEDIA CONTROL METHODS
    //

    /**
     * Toggle between playing and pausing music
     */
    public void togglePlay() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    /**
     * Resume playback. Starts playback over if at the end of the last song in the queue
     */
    public void play() {
        if (!isPlaying() && getFocus()) {
            if (shuffle) {
                if (queuePositionShuffled + 1 == queueShuffled.size() && mediaPlayer.isComplete()) {
                    queuePositionShuffled = 0;
                    begin();
                } else {
                    mediaPlayer.start();
                    updateNowPlaying();
                }
            } else {
                if (queuePosition + 1 == queue.size() && mediaPlayer.isComplete()) {
                    queuePosition = 0;
                    begin();
                } else {
                    mediaPlayer.start();
                    updateNowPlaying();
                }
            }
        }
    }

    /**
     * Pauses playback. The same as calling {@link MediaPlayer#pause()}
     * and {@link Player#updateNowPlaying()}
     */
    public void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
            updateNowPlaying();
        }
        shouldResumeOnFocusGained = false;
    }

    /**
     * Pauses playback and releases audio focus from the system
     */
    public void stop() {
        if (isPlaying()) {
            pause();
        }
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
        active = false;
        updateMediaSession();
    }

    /**
     * Gain Audio focus from the system if we don't already have it
     * @return whether we have gained focus (or already had it)
     */
    private boolean getFocus() {
        if (!active) {
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            int response = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            active = response == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return active;
    }

    /**
     * Skip to the previous track if less than 5 seconds in, otherwise restart this song from
     * the beginning
     */
    public void previous() {
        if (!isPreparing()) {
            if (mediaPlayer.getCurrentPosition() > 5000 || !hasNextInQueue(-1)) {
                mediaPlayer.seekTo(0);
                updateNowPlaying();
            } else {
                incrementQueuePosition(-1);
                begin();
            }
        }
    }

    /**
     * Skip to the next track in the queue
     */
    public void skip() {
        if (!isPreparing()) {
            if (!mediaPlayer.isComplete() && getNowPlaying() != null) {
                if (mediaPlayer.getCurrentPosition() > 24000
                        || mediaPlayer.getCurrentPosition() > mediaPlayer.getDuration() / 2) {
                    logPlayCount(getNowPlaying().getSongId(), false);
                } else if (getCurrentPosition() < 20000) {
                    logPlayCount(getNowPlaying().getSongId(), true);
                }
            }

            if (hasNextInQueue(1)) {
                incrementQueuePosition(1);
                begin();
            } else if (repeat == REPEAT_ALL) {
                setQueuePosition(0);
                begin();
            } else {
                mediaPlayer.complete();
                updateNowPlaying();
            }
        }
    }

    /**
     * Seek to a different queuePosition in the current track
     * @param position The queuePosition to seek to (in milliseconds)
     */
    public void seek(int position) {
        if (position <= mediaPlayer.getDuration() && getNowPlaying() != null) {
            mediaPlayer.seekTo(position);

            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(
                            isPlaying()
                                    ? PlaybackStateCompat.STATE_PLAYING
                                    : PlaybackStateCompat.STATE_PAUSED,
                            (long) mediaPlayer.getCurrentPosition(),
                            isPlaying() ? 1f : 0f)
                    .build());
        }
    }

    /**
     * Change the current song to a different song in the queue
     * @param newPosition The index of the song to start playing
     */
    public void changeSong(int newPosition) {
        if (!mediaPlayer.isComplete()) {
            if (mediaPlayer.getCurrentPosition() > 24000
                    || mediaPlayer.getCurrentPosition() > mediaPlayer.getDuration() / 2) {
                logPlayCount(getNowPlaying().getSongId(), false);
            } else if (getCurrentPosition() < 20000) {
                logPlayCount(getNowPlaying().getSongId(), true);
            }
        }

        if (shuffle) {
            if (newPosition < queueShuffled.size() && newPosition != queuePositionShuffled) {
                queuePositionShuffled = newPosition;
                begin();
            }
        } else {
            if (newPosition < queue.size() && queuePosition != newPosition) {
                queuePosition = newPosition;
                begin();
            }
        }
    }

    //
    //      QUEUE METHODS
    //

    /**
     * Add a song to the queue so it plays after the current track. If shuffle is enabled, then the
     * song will also be added to the end of the unshuffled queue.
     * @param song the {@link Song} to add
     */
    public void queueNext(final Song song) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.add(queuePositionShuffled + 1, song);
                queue.add(song);
            } else {
                queue.add(queuePosition + 1, song);
            }
        } else {
            List<Song> newQueue = new ArrayList<>();
            newQueue.add(song);
            setQueue(newQueue, 0);
            begin();
        }
    }

    /**
     * Add a song to the end of the queue. If shuffle is enabled, then the song will also be added
     * to the end of the unshuffled queue.
     * @param song the {@link Song} to add
     */
    public void queueLast(final Song song) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.add(queueShuffled.size(), song);
            }
            queue.add(queue.size(), song);
        } else {
            List<Song> newQueue = new ArrayList<>();
            newQueue.add(song);
            setQueue(newQueue, 0);
            begin();
        }
    }

    /**
     * Add songs to the queue so they play after the current track. If shuffle is enabled, then the
     * songs will also be added to the end of the unshuffled queue.
     * @param songs a {@link List} of {@link Song}s to add
     */
    public void queueNext(final List<Song> songs) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.addAll(queuePositionShuffled + 1, songs);
                queue.addAll(songs);
            } else {
                queue.addAll(queuePosition + 1, songs);
            }
        } else {
            List<Song> newQueue = new ArrayList<>();
            newQueue.addAll(songs);
            setQueue(newQueue, 0);
            begin();
        }

    }

    /**
     * Add songs to the end of the queue. If shuffle is enabled, then the songs will also be added
     * to the end of the unshuffled queue.
     * @param songs an {@link List} of {@link Song}s to add
     */
    public void queueLast(final List<Song> songs) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.addAll(queueShuffled.size(), songs);
                queue.addAll(queue.size(), songs);
            } else {
                queue.addAll(queue.size(), songs);
            }
        } else {
            List<Song> newQueue = new ArrayList<>();
            newQueue.addAll(songs);
            setQueue(newQueue, 0);
            begin();
        }
    }

    //
    //      SHUFFLE & REPEAT METHODS
    //

    /**
     * Shuffle the queue and put it into {@link Player#queueShuffled}. The current song will always
     * be placed first in the list
     */
    private void shuffleQueue() {
        queueShuffled.clear();

        if (queue.size() > 0) {
            queuePositionShuffled = 0;
            queueShuffled.add(queue.get(queuePosition));

            List<Song> randomHolder = new ArrayList<>();

            for (int i = 0; i < queuePosition; i++) {
                randomHolder.add(queue.get(i));
            }
            for (int i = queuePosition + 1; i < queue.size(); i++) {
                randomHolder.add(queue.get(i));
            }

            Collections.shuffle(randomHolder, new Random(System.nanoTime()));

            queueShuffled.addAll(randomHolder);
        }
    }

    public void setPrefs(boolean shuffleSetting, short repeatSetting) {
        // Because SharedPreferences doesn't work with multiple processes (thanks Google...)
        // we actually have to be told what the new settings are in order to avoid the service
        // and UI doing the opposite of what they should be doing and to prevent the universe
        // from exploding. It's fine to initialize the SharedPreferences by reading them like we
        // do in the constructor since they haven't been modified, but if something gets written
        // in one process the SharedPreferences in the other process won't be updated.

        // I really wish someone had told me this earlier.

        if (shuffle != shuffleSetting) {
            shuffle = shuffleSetting;

            if (shuffle) {
                shuffleQueue();
            } else if (queueShuffled.size() > 0) {
                queuePosition = queue.indexOf(queueShuffled.get(queuePositionShuffled));
                queueShuffled = new ArrayList<>();
            }
        }

        repeat = repeatSetting;

        updateNowPlaying();
    }

    //
    //      PLAY & SKIP COUNT LOGGING
    //

    /**
     * Initializes {@link Player#playCountHashtable}
     * @throws IOException
     */
    private void openPlayCountFile() throws IOException {
        File file = new File(context.getExternalFilesDir(null) + "/" + Library.PLAY_COUNT_FILENAME);

        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
        }

        InputStream is = new FileInputStream(file);

        try {
            playCountHashtable = new Properties();
            playCountHashtable.load(is);
        } finally {
            is.close();
        }
    }

    /**
     * Writes {@link Player#playCountHashtable} to disk
     * @throws IOException
     */
    private void savePlayCountFile() throws IOException {
        OutputStream os = new FileOutputStream(context.getExternalFilesDir(null) + "/"
                + Library.PLAY_COUNT_FILENAME);

        try {
            playCountHashtable.store(os, Library.PLAY_COUNT_FILE_COMMENT);
        } finally {
            os.close();
        }
    }

    /**
     * Record a play or skip for a certain song
     * @param songId the ID of the song written in the {@link android.provider.MediaStore}
     * @param skip Whether the song was skipped (true if skipped, false if played)
     */
    public void logPlayCount(long songId, boolean skip) {
        try {
            if (playCountHashtable == null) {
                openPlayCountFile();
            }

            final String originalValue = playCountHashtable.getProperty(Long.toString(songId));
            int playCount = 0;
            int skipCount = 0;
            int playDate = 0;

            if (originalValue != null && !originalValue.equals("")) {
                final String[] originalValues = originalValue.split(",");

                playCount = Integer.parseInt(originalValues[0]);
                skipCount = Integer.parseInt(originalValues[1]);

                // Preserve backwards compatibility with play count files written with older
                // versions of Jockey that didn't save this data
                if (originalValues.length > 2) {
                    playDate = Integer.parseInt(originalValues[2]);
                }
            }

            if (skip) {
                skipCount++;
            } else {
                playDate = (int) (System.currentTimeMillis() / 1000);
                playCount++;
            }

            playCountHashtable.setProperty(
                    Long.toString(songId),
                    playCount + "," + skipCount + "," + playDate);

            savePlayCountFile();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }

    //
    //      ACCESSOR METHODS
    //

    public Bitmap getArt() {
        return art;
    }

    public boolean isPlaying() {
        return mediaPlayer.getState() == ManagedMediaPlayer.Status.STARTED;
    }

    public boolean isPreparing() {
        return mediaPlayer.getState() == ManagedMediaPlayer.Status.PREPARING;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public List<Song> getQueue() {
        if (shuffle) {
            return queueShuffled;
        }
        return queue;
    }

    public int getQueuePosition() {
        if (shuffle) {
            return queuePositionShuffled;
        }
        return queuePosition;
    }

    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    /**
     * Receives headphone connect and disconnect intents so that music may be paused when headphones
     * are disconnected
     */
    public static class HeadsetListener extends BroadcastReceiver {

        Player instance;

        public HeadsetListener(Player instance) {
            this.instance = instance;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)
                    && intent.getIntExtra("state", -1) == 0 && instance.isPlaying()) {

                instance.pause();
                instance.updateUi();
            }
        }
    }
}

