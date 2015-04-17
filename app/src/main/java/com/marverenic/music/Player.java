package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.annotations.SerializedName;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.ManagedMediaPlayer;
import com.marverenic.music.utils.MediaReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@SuppressWarnings("deprecation")
public class Player implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    public static final String UPDATE_BROADCAST = "marverenic.jockey.player.REFRESH"; // Sent to refresh views that use up-to-date player information
    public static final String TEMP_COUNT_LOG_FILE = ".count-tmp";
    private static final String TAG = "Player";

    // Instance variables
    private ManagedMediaPlayer mediaPlayer;
    private Context context;
    private MediaSession mediaSession;
    private RemoteControlClient remoteControlClient;

    // Queue information
    private ArrayList<Song> queue;
    private ArrayList<Song> queueShuffled = new ArrayList<>();
    private int position;
    private int positionShuffled;

    // MediaFocus variables
    private boolean active = false; // If we currently have audio focus
    private boolean shouldResumeOnFocusGained = false; // If we should play music when focus is returned

    // Shufle & Repeat options
    private boolean shuffle; // Shuffle status
    public enum repeatOption {NONE, ONE, ALL}
    private repeatOption repeat; // Repeat status

    private Bitmap art; // The art for the current song
    private Bitmap artFullRes; // The full resolution artwork for the current song

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
        position = 0;

        // Update shuffle and repeat settings
        shuffle = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefShuffle", false);
        boolean repeatAll = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefRepeat", false);
        boolean repeatOne = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefRepeatOne", false);

        if (repeatAll && repeatOne) {
            repeat = repeatOption.NONE;

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("prefRepeat", false);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("prefRepeatOne", false);
        } else {
            if (repeatAll) repeat = repeatOption.ALL;
            else if (repeatOne) repeat = repeatOption.ONE;
            else repeat = repeatOption.NONE;
        }

        // Initialize the relevant media controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initMediaSession();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            initRemoteController();
        }
    }

    public void finish (){
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);

        active = false;
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
        context = null;
    }

    @TargetApi(21)
    private void initMediaSession() {
        mediaSession = new MediaSession(context, TAG);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() { play(); }

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
        mediaSession.setSessionActivity(PendingIntent.getActivity(context, 0, new Intent(context, NowPlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_CANCEL_CURRENT));
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        PlaybackState.Builder state = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_NONE, 0, 0f);
        mediaSession.setPlaybackState(state.build());
        mediaSession.setActive(true);
    }

    @TargetApi(18)
    private void initRemoteController() {
        getFocus();

        ComponentName eventReceiver = new ComponentName(context.getPackageName(), MediaReceiver.class.getName());
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerMediaButtonEventReceiver(eventReceiver);

        // build the PendingIntent for the remote control client
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, mediaButtonIntent, 0);

        // create and register the remote control client
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        // Flags for the media transport control that this client supports.
        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

        remoteControlClient.setTransportControlFlags(flags);
        audioManager.registerRemoteControlClient(remoteControlClient);
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
        skip();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if(!isPreparing()) {
            mediaPlayer.start();
        }
    }

    public void setQueue(final ArrayList<Song> newQueue, final int newPosition) {
        queue = newQueue;
        position = newPosition;
        if (shuffle) shuffleQueue();
    }

    public void changeQueue(ArrayList<Song> newQueue, int newPosition) {
        if (newPosition < 0) throw new NegativeArraySizeException("newPosition cannot be negative");

        if (shuffle){
            queueShuffled = newQueue;
            positionShuffled = newPosition;
        }
        else{
            queue = newQueue;
            position = newPosition;
        }
    }

    // Start playing a new song
    public void begin() {
        if (getFocus()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            // Fetch a low resolution art bitmap initially...
            art = Fetch.fetchAlbumArtLocal(context, getNowPlaying().albumId);
            if (artFullRes != null) artFullRes.recycle();
            artFullRes = null;
            // ... And a high resolution version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Fetch.fetchFullResolutionArt(getNowPlaying(), context, new Fetch.fullResolutionArtCallback() {
                    @Override
                    public void onArtFetched(Bitmap art) {
                        artFullRes = art;
                        updateNowPlaying();
                    }
                });
            }
            else{
                // On ICS, calling Fetch.fetchFullResolutionArt(...) here throws a Remote Exception
                // As a result, ICS won't use full resolution album artwork
                artFullRes = art;
                updateNowPlaying();
            }

            try {
                mediaPlayer.setDataSource((getNowPlaying()).location);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
                Toast.makeText(context, "There was an error playing this song", Toast.LENGTH_SHORT).show();
                Debug.log(Debug.LogLevel.WARNING, TAG, "There was an error setting the data source", context);
                return;
            }
            try {
                mediaPlayer.prepareAsync();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    // Update external information for the current track
    public void updateNowPlaying() {
        PlayerService.getInstance().notifyNowPlaying();
        context.sendOrderedBroadcast(new Intent(UPDATE_BROADCAST), null);

        // Update the relevant media controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            updateRemoteController();
        }
    }

    @TargetApi(21)
    public void updateMediaSession() {
        if (getNowPlaying() != null) {
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
            metadataBuilder
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, getNowPlaying().songName)
                    .putString(MediaMetadata.METADATA_KEY_TITLE, getNowPlaying().songName)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, getNowPlaying().albumName)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, getNowPlaying().artistName)
                    .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art);
            mediaSession.setMetadata(metadataBuilder.build());

            PlaybackState.Builder state = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                    PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                    PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);

            switch (mediaPlayer.getState()) {
                case STARTED:
                    state.setState(PlaybackState.STATE_PLAYING, getPosition(), 1f);
                    break;
                case PAUSED:
                    state.setState(PlaybackState.STATE_PAUSED, getPosition(), 1f);
                    break;
                case STOPPED:
                    state.setState(PlaybackState.STATE_STOPPED, getPosition(), 1f);
                    break;
                default:
                    state.setState(PlaybackState.STATE_NONE, getPosition(), 1f);
            }
            mediaSession.setPlaybackState(state.build());
            mediaSession.setActive(true);
        }
    }

    @TargetApi(18)
    public void updateRemoteController (){
        if (isPlaying()) {
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        }
        else{
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }

        // Update the metadata
        remoteControlClient.editMetadata(true)
                .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getNowPlaying().artistName)
                .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getNowPlaying().albumName)
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getNowPlaying().songName)
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, getNowPlaying().songDuration)
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, getArt())
                .apply();
    }

    public Song getNowPlaying() {
        if (shuffle) {
            if (queueShuffled.size() == 0 || positionShuffled >= queueShuffled.size() || positionShuffled < 0) {
                return null;
            }
            return queueShuffled.get(positionShuffled);
        }
        if (queue.size() == 0 || position >= queue.size() || position < 0) {
            return null;
        }
        return queue.get(position);
    }

    //
    //      MEDIA CONTROL METHODS
    //

    public void togglePlay() {
        if (isPlaying()) {
            pause();
        }
        else {
            play();
        }
    }

    public void play() {
        if (!isPlaying() && getFocus()) {
            if (shuffle) {
                if (positionShuffled + 1 == queueShuffled.size() && mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() < 100) {
                    positionShuffled = 0;
                    begin();
                } else {
                    mediaPlayer.start();
                    updateNowPlaying();
                }
            } else {
                if (position + 1 == queue.size() && mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() < 100) {
                    position = 0;
                    begin();
                } else {
                    mediaPlayer.start();
                    updateNowPlaying();
                }
            }
        }
    }

    public void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
            updateNowPlaying();
        }
    }

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

    public boolean getFocus() {
        if (!active) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            active = (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return active;
    }

    public void previous() {
        if (!isPreparing()) {
            if (shuffle) {
                if (mediaPlayer.getCurrentPosition() > 5000 || positionShuffled < 1) {
                    mediaPlayer.seekTo(0);
                } else {
                    positionShuffled--;
                    begin();
                }
            } else {
                if (mediaPlayer.getCurrentPosition() > 5000 || position < 1) {
                    mediaPlayer.seekTo(0);
                } else {
                    position--;
                    begin();
                }
            }
        }
    }

    public void skip() {
        if (!isPreparing()) {
            if (getCurrentPosition() > 24000 || getCurrentPosition() > mediaPlayer.getDuration() / 2) {
                logPlayCount(getNowPlaying().songId, false);
            }
            else if (getCurrentPosition() < 20000) {
                logPlayCount(getNowPlaying().songId, true);
            }

            // Change the media source
            if (shuffle) {
                if (positionShuffled + 1 < queueShuffled.size()) {
                    positionShuffled++;
                    begin();
                } else {
                    if (repeat == repeatOption.ALL) {
                        positionShuffled = 0;
                        begin();
                    } else {
                        mediaPlayer.pause();
                        mediaPlayer.seekTo(mediaPlayer.getDuration());
                        updateNowPlaying();
                    }
                }
            } else {
                if (position + 1 < queue.size()) {
                    position++;
                    begin();
                } else {
                    if (repeat == repeatOption.ALL) {
                        position = 0;
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

    public void seek(int position) {
        if (position <= mediaPlayer.getDuration() && getNowPlaying() != null) {
            mediaPlayer.seekTo(position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isPlaying()) {
                    mediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, (long) mediaPlayer.getCurrentPosition(), 1f).build());
                } else {
                    mediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, (long) mediaPlayer.getCurrentPosition(), 1f).build());
                }
            }
        }
    }

    public void changeSong(int newPosition) {
        if (getCurrentPosition() > 24000 || getCurrentPosition() > mediaPlayer.getDuration() / 2) {
            logPlayCount(getNowPlaying().songId, false);
        }
        else if (getCurrentPosition() < 20000) {
            logPlayCount(getNowPlaying().songId, true);
        }

        if (shuffle) {
            if (newPosition < queueShuffled.size() && newPosition != positionShuffled) {
                positionShuffled = newPosition;
                begin();
            }
        } else {
            if (newPosition < queue.size() && position != newPosition) {
                position = newPosition;
                begin();
            }
        }
    }

    //
    //      QUEUE METHODS
    //

    public void queueNext(final Song song) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.add(positionShuffled + 1, song);
                queue.add(song);
            } else {
                queue.add(position + 1, song);
            }
        } else {
            ArrayList<Song> newQueue = new ArrayList<>();
            newQueue.add(song);
            setQueue(newQueue, 0);
            begin();
        }
    }

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

    public void queueNext(final ArrayList<Song> songs) {
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.addAll(positionShuffled + 1, songs);
                queue.addAll(songs);
            } else {
                queue.addAll(position + 1, songs);
            }
        } else {
            ArrayList<Song> newQueue = new ArrayList<>();
            newQueue.addAll(songs);
            setQueue(newQueue, 0);
            begin();
        }

    }

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

    public void toggleShuffle() {
        shuffle = !shuffle;
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putBoolean("prefShuffle", shuffle);
        prefs.apply();

        if (shuffle) {
            shuffleQueue();
        } else {
            position = queue.indexOf(queueShuffled.get(positionShuffled));
        }
    }

    private void shuffleQueue() {
        queueShuffled.clear();

        if (queue.size() > 0) {
            positionShuffled = 0;
            queueShuffled.add(queue.get(position));

            ArrayList<Song> randomHolder = new ArrayList<>();

            for (int i = 0; i < position; i++) {
                randomHolder.add(queue.get(i));
            }
            for (int i = position + 1; i < queue.size(); i++) {
                randomHolder.add(queue.get(i));
            }

            Collections.shuffle(randomHolder, new Random(System.nanoTime()));

            queueShuffled.addAll(randomHolder);

        }
    }

    public void toggleRepeat() {
        if (repeat == repeatOption.ALL) {
            repeat = repeatOption.ONE;
            mediaPlayer.setLooping(true);
        } else {
            if (repeat == repeatOption.ONE) {
                repeat = repeatOption.NONE;
            } else {
                repeat = repeatOption.ALL;
            }
            mediaPlayer.setLooping(false);
        }
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putBoolean("prefRepeat", repeat == repeatOption.ALL);
        prefs.putBoolean("prefRepeatOne", repeat == repeatOption.ONE);
        prefs.apply();
    }

    //
    //      PLAY & SKIP COUNT LOGGING
    //

    public void logPlayCount(long songId, boolean skip){
        /*
         * Because LibraryScanner is in another process, save play and skip counts temporarily
         * to a file that LibraryScanner can read when an UPDATE broadcast is sent or the
         * library is reloaded.
         *
         * This prevents duplicating data, keeping data in sync across processes, convoluted
         * intent broadcasts, or other interprocess communication that wouldn't log any counts
         * when the main process has stopped.
         */

        File countFile = new File(context.getExternalFilesDir(null), TEMP_COUNT_LOG_FILE);

        try {
            FileOutputStream outputStream = new FileOutputStream(countFile, true);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(songId + " " + skip + " ");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    //      ACCESSOR METHODS
    //

    public Bitmap getArt() {
        return art;
    }
    public Bitmap getFullArt() {
        return artFullRes;
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

    public boolean isShuffle() {
        return shuffle;
    }

    public boolean isRepeat() {
        return repeat == repeatOption.ALL;
    }

    public boolean isRepeatOne() {
        return repeat == repeatOption.ONE;
    }

    public ArrayList<Song> getQueue() {
        if (shuffle) return new ArrayList<>(queueShuffled);
        return new ArrayList<>(queue);
    }

    public int getPosition() {
        if (shuffle) return positionShuffled;
        return position;
    }

    //
    //      STATE SAVING & READING METHODS
    //

    public PlayerHolder getSaveState(){
        PlayerHolder playerHolder = new PlayerHolder();
        playerHolder.queue = queue;
        playerHolder.position = position;
        if (shuffle) {
            playerHolder.queueShuffled = queueShuffled;
            playerHolder.positionShuffled = positionShuffled;
        } else {
            playerHolder.queueShuffled = new ArrayList<>();
            playerHolder.positionShuffled = 0;
        }
        playerHolder.currentPosition = mediaPlayer.getCurrentPosition();
        return playerHolder;
    }

    public void restoreState(final PlayerHolder playerHolder) {
        this.queue = playerHolder.queue;
        this.position = playerHolder.position;
        if (shuffle) {
            this.queueShuffled = playerHolder.queueShuffled;
            this.positionShuffled = playerHolder.positionShuffled;
        } else {
            this.queueShuffled = new ArrayList<>();
            this.positionShuffled = 0;
        }

        //If there isn't any music in the queue, then don't proceed.
        if (queue.isEmpty()) return;

        final MediaPlayer.OnPreparedListener defaultListener = this;
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.seekTo(playerHolder.currentPosition);
                mp.setOnPreparedListener(defaultListener);
            }
        });

        // Perform most of the same tasks as begin(), but don't acquire the audio focus yet
        art = Fetch.fetchAlbumArtLocal(context, getNowPlaying().albumId);
        if (artFullRes != null) artFullRes.recycle();
        artFullRes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Fetch.fetchFullResolutionArt(getNowPlaying(), context, new Fetch.fullResolutionArtCallback() {
                @Override
                public void onArtFetched(Bitmap art) {
                    artFullRes = art;
                    updateNowPlaying();
                }
            });
        }
        else{
            artFullRes = art;
        }

        try {
            mediaPlayer.setDataSource((getNowPlaying()).location);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
            Toast.makeText(context, "There was an error playing this song", Toast.LENGTH_SHORT).show();
            Debug.log(Debug.LogLevel.WARNING, TAG, "There was an error setting the data source", context);
            return;
        }
        try {
            mediaPlayer.prepareAsync();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static final class PlayerHolder{
        // Holds info to save and restore the player's state

        // Queue information
        @SerializedName("queue")
        public ArrayList<Song> queue;
        @SerializedName("queueShuffled")
        public ArrayList<Song> queueShuffled;
        @SerializedName("position")
        public int position;
        @SerializedName("positionShuffled")
        public int positionShuffled;
        @SerializedName("currentPosition")
        public int currentPosition;
    }
}

