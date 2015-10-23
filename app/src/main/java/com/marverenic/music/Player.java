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
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.ManagedMediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

public class Player implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    public static final String UPDATE_BROADCAST = "marverenic.jockey.player.REFRESH"; // Sent to refresh views that use up-to-date player information
    private static final String TAG = "Player";
    private static final String QUEUE_FILE = ".queue";

    public static final String PREFERENCE_SHUFFLE = "prefShuffle";
    public static final String PREFERENCE_REPEAT = "prefRepeat";

    // Instance variables
    private ManagedMediaPlayer mediaPlayer;
    private Context context;
    private MediaSessionCompat mediaSession;
    private HeadsetListener headphoneListener;

    // Queue information
    private ArrayList<Song> queue;
    private ArrayList<Song> queueShuffled = new ArrayList<>();
    private int queuePosition;
    private int queuePositionShuffled;

    // MediaFocus variables
    private boolean active = false; // If we currently have audio focus
    private boolean shouldResumeOnFocusGained = false; // If we should play music when focus is returned

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

            art = Fetch.fetchFullArt(getNowPlaying());
            mediaPlayer.setDataSource(getNowPlaying().location);
            mediaPlayer.prepareAsync();
        }
        catch (Exception e){
            queuePosition = 0;
            queuePositionShuffled = 0;
            queue.clear();
            queueShuffled.clear();
        }
    }

    /**
     * Writes a player state to disk. Contains information about the queue (both unshuffled and shuffled),
     * current queuePosition within this list, and the current queuePosition of the song
     * @throws IOException
     */
    public void saveState(@NonNull final String nextCommand) throws IOException {
        // Anticipate the outcome of a command so that if we're killed right after it executes,
        // we can restore to the proper state
        int reloadSeekPosition;
        int reloadQueuePosition = (shuffle)? queuePositionShuffled : queuePosition;

        switch (nextCommand) {
            case PlayerService.ACTION_NEXT:
                if (reloadQueuePosition + 1 < queue.size()) {
                    reloadSeekPosition = 0;
                    reloadQueuePosition++;
                }
                else{
                    reloadSeekPosition = mediaPlayer.getDuration();
                }
                break;
            case PlayerService.ACTION_PREV:
                if (mediaPlayer.getDuration() < 5000 && reloadQueuePosition - 1 > 0){
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
        for (Song s : this.queue){
            queue += " " + s.songId;
        }

        String queueShuffled = "";
        for (Song s : this.queueShuffled){
            queueShuffled += " " + s.songId;
        }

        String output = currentPosition + " " + queuePosition + " " + queueLength + queue + queueShuffled;

        File save = new File(context.getExternalFilesDir(null), QUEUE_FILE);
        FileOutputStream stream = new FileOutputStream(save);
        stream.write(output.getBytes());
        stream.close();
    }

    /**
     * Release the player. Call when finished with an instance
     */
    public void finish (){
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
        context.unregisterReceiver(headphoneListener);

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
            }

            @Override
            public void onSkipToQueueItem(long id) {
                changeSong((int) id);
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                skip();
            }

            @Override
            public void onSkipToPrevious() {
                previous();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onSeekTo(long pos) {
                seek((int) pos);
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
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(repeat == REPEAT_ONE){
            mediaPlayer.seekTo(0);
            play();
        }
        else {
            skip();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if(!isPreparing()) {
            mediaPlayer.start();
            updateNowPlaying();
        }
    }

    /**
     * Change the queue and shuffle it if necessary
     * @param newQueue An {@link ArrayList} of {@link Song}s to become the new queue
     * @param newPosition The queuePosition in the list to start playback from
     */
    public void setQueue(final ArrayList<Song> newQueue, final int newPosition) {
        queue = newQueue;
        queuePosition = newPosition;
        if (shuffle) shuffleQueue();
        updateNowPlaying();
    }

    /**
     * Replace the contents of the queue without affecting playback
     * @param newQueue An {@link ArrayList} of {@link Song}s to become the new queue
     * @param newPosition The queuePosition in the list to start playback from
     */
    public void editQueue(final ArrayList<Song> newQueue, final int newPosition) {
        if (shuffle){
            queueShuffled = newQueue;
            queuePositionShuffled = newPosition;
        }
        else {
            queue = newQueue;
            queuePosition = newPosition;
        }
        updateNowPlaying();
    }

    /**
     * Begin playback of a new song. Call this method after changing the queue or now playing track
     */
    public void begin() {
        if (getFocus()) {
            mediaPlayer.stop();
            mediaPlayer.reset();

            art = Fetch.fetchFullArt(getNowPlaying());

            try {
                mediaPlayer.setDataSource((getNowPlaying()).location);
            } catch (Exception e) {
                Crashlytics.logException(e);
                Log.e("MUSIC SERVICE", "Error setting data source", e);
                Toast.makeText(context, "There was an error playing this song", Toast.LENGTH_SHORT).show();
                return;
            }
            mediaPlayer.prepareAsync();
        }
    }

    /**
     * Update the main thread and Android System about this player instance. This method also calls
     * {@link PlayerService#notifyNowPlaying()}.
     */
    public void updateNowPlaying() {
        PlayerService.getInstance().notifyNowPlaying();
        context.sendOrderedBroadcast(new Intent(UPDATE_BROADCAST), null);
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
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, nowPlaying.songName)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, nowPlaying.songName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, nowPlaying.albumName)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, nowPlaying.artistName)
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
            mediaSession.setActive(true);
        }
    }

    /**
     * Get the song at the current queuePosition in the queue or shuffled queue
     * @return The now playing {@link Song} (null if nothing is playing)
     */
    public Song getNowPlaying() {
        if (shuffle) {
            if (queueShuffled.size() == 0 || queuePositionShuffled >= queueShuffled.size() || queuePositionShuffled < 0) {
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
        }
        else {
            play();
        }
    }

    /**
     * Resume playback. Starts playback over if at the end of the last song in the queue
     */
    public void play() {
        if (!isPlaying() && getFocus()) {
            if (shuffle) {
                if (queuePositionShuffled + 1 == queueShuffled.size()
                        && mediaPlayer.getState() == ManagedMediaPlayer.status.COMPLETED) {

                    queuePositionShuffled = 0;
                    begin();
                } else {
                    mediaPlayer.start();
                    updateNowPlaying();
                }
            } else {
                if (queuePosition + 1 == queue.size()
                        && mediaPlayer.getState() == ManagedMediaPlayer.status.COMPLETED) {

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
     * Pauses playback. The same as calling {@link MediaPlayer#pause()} and {@link Player#updateNowPlaying()}
     */
    public void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
            updateNowPlaying();
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
    }

    /**
     * Gain Audio focus from the system if we don't already have it
     * @return whether we have gained focus (or already had it)
     */
    private boolean getFocus() {
        if (!active) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            active = (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return active;
    }

    /**
     * Skip to the previous track if less than 5 seconds in, otherwise restart this song from the beginning
     */
    public void previous() {
        if (!isPreparing()) {
            if (shuffle) {
                if (mediaPlayer.getCurrentPosition() > 5000 || queuePositionShuffled < 1) {
                    mediaPlayer.seekTo(0);
                    updateNowPlaying();
                } else {
                    queuePositionShuffled--;
                    begin();
                }
            } else {
                if (mediaPlayer.getCurrentPosition() > 5000 || queuePosition < 1) {
                    mediaPlayer.seekTo(0);
                    updateNowPlaying();
                } else {
                    queuePosition--;
                    begin();
                }
            }
        }
    }

    /**
     * Skip to the next track in the queue
     */
    public void skip() {
        if (!isPreparing()) {
            if (mediaPlayer.getState() == ManagedMediaPlayer.status.COMPLETED
                    || mediaPlayer.getCurrentPosition() > 24000
                    || mediaPlayer.getCurrentPosition() > mediaPlayer.getDuration() / 2) {
                logPlayCount(getNowPlaying().songId, false);
            }
            else if (getCurrentPosition() < 20000) {
                logPlayCount(getNowPlaying().songId, true);
            }

            // Change the media source
            if (shuffle) {
                if (queuePositionShuffled + 1 < queueShuffled.size()) {
                    queuePositionShuffled++;
                    begin();
                } else {
                    if (repeat == REPEAT_ALL) {
                        queuePositionShuffled = 0;
                        begin();
                    } else {
                        mediaPlayer.pause();
                        mediaPlayer.seekTo(mediaPlayer.getDuration());
                        updateNowPlaying();
                    }
                }
            } else {
                if (queuePosition + 1 < queue.size()) {
                    queuePosition++;
                    begin();
                } else {
                    if (repeat == REPEAT_ALL) {
                        queuePosition = 0;
                        begin();
                    } else {
                        mediaPlayer.pause();
                        mediaPlayer.seekTo(mediaPlayer.getDuration());
                        updateNowPlaying();
                    }

                }
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
        if (mediaPlayer.getState() == ManagedMediaPlayer.status.COMPLETED
                || mediaPlayer.getCurrentPosition() > 24000
                || mediaPlayer.getCurrentPosition() > mediaPlayer.getDuration() / 2) {
            logPlayCount(getNowPlaying().songId, false);
        }
        else if (getCurrentPosition() < 20000) {
            logPlayCount(getNowPlaying().songId, true);
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
            ArrayList<Song> newQueue = new ArrayList<>();
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
            ArrayList<Song> newQueue = new ArrayList<>();
            newQueue.add(song);
            setQueue(newQueue, 0);
            begin();
        }
    }

    /**
     * Add songs to the queue so they play after the current track. If shuffle is enabled, then the
     * songs will also be added to the end of the unshuffled queue.
     * @param songs an {@link ArrayList} of {@link Song}s to add
     */
    public void queueNext(final ArrayList<Song> songs) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.addAll(queuePositionShuffled + 1, songs);
                queue.addAll(songs);
            } else {
                queue.addAll(queuePosition + 1, songs);
            }
        } else {
            ArrayList<Song> newQueue = new ArrayList<>();
            newQueue.addAll(songs);
            setQueue(newQueue, 0);
            begin();
        }

    }

    /**
     * Add songs to the end of the queue. If shuffle is enabled, then the songs will also be added
     * to the end of the unshuffled queue.
     * @param songs an {@link ArrayList} of {@link Song}s to add
     */
    public void queueLast(final ArrayList<Song> songs) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.addAll(queueShuffled.size(), songs);
                queue.addAll(queue.size(), songs);
            } else {
                queue.addAll(queue.size(), songs);
            }
        } else {
            ArrayList<Song> newQueue = new ArrayList<>();
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

            ArrayList<Song> randomHolder = new ArrayList<>();

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

    public void setPrefs(boolean shuffleSetting, short repeatSetting){
        // Because SharedPreferences doesn't work with multiple processes (thanks Google...)
        // we actually have to be told what the new settings are in order to avoid the service
        // and UI doing the opposite of what they should be doing and to prevent the universe
        // from exploding. It's fine to initialize the SharedPreferences by reading them like we
        // do in the constructor since they haven't been modified, but if something gets written
        // in one process the SharedPreferences in the other process won't be updated.

        // I really wish someone had told me this earlier.

        if (shuffle != shuffleSetting){
            shuffle = shuffleSetting;

            if (shuffle) {
                shuffleQueue();
            } else if (queueShuffled.size() > 0){
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
    private void openPlayCountFile() throws IOException{
        File file = new File(context.getExternalFilesDir(null) + "/" + Library.PLAY_COUNT_FILENAME);

        if (!file.exists()) //noinspection ResultOfMethodCallIgnored
            file.createNewFile();

        InputStream is = new FileInputStream(file);

        try {
            playCountHashtable = new Properties();
            playCountHashtable.load(is);
        }
        finally {
            is.close();
        }
    }

    /**
     * Writes {@link Player#playCountHashtable} to disk
     * @throws IOException
     */
    private void savePlayCountFile() throws IOException{
        OutputStream os = new FileOutputStream(context.getExternalFilesDir(null) + "/" + Library.PLAY_COUNT_FILENAME);

        try {
            playCountHashtable.store(os, Library.PLAY_COUNT_FILE_COMMENT);
        }
        finally {
            os.close();
        }
    }

    /**
     * Record a play or skip for a certain song
     * @param songId the ID of the song written in the {@link android.provider.MediaStore}
     * @param skip Whether the song was skipped (true if skipped, false if played)
     */
    public void logPlayCount(long songId, boolean skip){
        try {
            if (playCountHashtable == null) openPlayCountFile();

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
        }
        catch (Exception e){
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
        return mediaPlayer.getState() == ManagedMediaPlayer.status.STARTED;
    }

    public boolean isPreparing() {
        return mediaPlayer.getState() == ManagedMediaPlayer.status.PREPARING;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public ArrayList<Song> getQueue() {
        if (shuffle) return new ArrayList<>(queueShuffled);
        return new ArrayList<>(queue);
    }

    public int getQueuePosition() {
        if (shuffle) return queuePositionShuffled;
        return queuePosition;
    }

    /**
     * Receives headphone connect and disconnect intents so that music may be paused when headphones
     * are disconnected
     */
    public static class HeadsetListener extends BroadcastReceiver {

        Player instance;

        public HeadsetListener(Player instance){
            this.instance = instance;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)
                    && intent.getIntExtra("state", -1) == 0 && instance.isPlaying()){

                instance.pause();
            }
        }
    }
}

