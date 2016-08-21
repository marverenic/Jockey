package com.marverenic.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;

import com.marverenic.music.IPlayerService;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.ReadOnlyPreferencesStore;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public final class PlayerController {

    private static Context applicationContext;
    private static IPlayerService playerService;
    private static Set<UpdateListener> updateListeners;
    private static Set<InfoListener> infoListeners;
    private static Set<ErrorListener> errorListeners;
    private static Bitmap artwork;

    static {
        updateListeners = new HashSet<>();
        errorListeners = new HashSet<>();
        infoListeners = new HashSet<>();
    }

    // This class is never instantiated
    private PlayerController() {

    }

    /**
     * Start the player service in the background
     * @param context The {@link Context} to start the service and send commands with
     *                THIS {@link Context} IS KEPT FOREVER unless {@link PlayerController#stop()}
     *                is called and may cause a leak
     */
    public static void startService(Context context) {
        if (applicationContext == null) {
            if (!MediaStoreUtil.hasPermission(context)) {
                Timber.i("Storage permission is not been granted. Aborting service initialization");
                return;
            }

            applicationContext = context;

            Intent serviceIntent = new Intent(context, PlayerService.class);

            // Manually start the service to ensure that it is associated with this task and can
            // appropriately set its dismiss behavior
            context.startService(serviceIntent);

            context.bindService(serviceIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    playerService = IPlayerService.Stub.asInterface(service);
                    updateUi();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    playerService = null;
                    applicationContext = null;
                }
            }, Context.BIND_WAIVE_PRIORITY);
        }
    }

    /**
     * @return Whether or not the service has been bound to the UI process
     */
    public static boolean isServiceStarted() {
        return playerService != null;
    }

    /**
     * Register a callback for when the Player Service changes its state and the UI needs to be
     * updated. Don't forget to unregister this listener when you're done, otherwise you'll probably
     * leak an Activity or something horrific.
     * @param l The UpdateListener to be registered
     * @see #unregisterUpdateListener(UpdateListener)
     */
    public static void registerUpdateListener(UpdateListener l) {
        updateListeners.add(l);
    }

    /**
     * Unregister an Update Listener callback set in {@link #registerUpdateListener(UpdateListener)}
     * @param l The Listener to be removed. If it's not currently bound than nothing interesting
     *          happens.
     */
    public static void unregisterUpdateListener(UpdateListener l) {
        updateListeners.remove(l);
    }

    /**
     * Register a callback for when the Player Service sends a message to the UI thread containing
     * information that should be directly presented to the user. Don't forget to unregister this
     * listener when you're done, otherwise you'll probably leak an Activity or Context.
     * @param l The InfoListener to be registered
     * @see #unregisterInfoListener(InfoListener)
     */
    public static void registerInfoListener(InfoListener l) {
        infoListeners.add(l);
    }

    /**
     * Unregister an InfoListener callback added in {@link #registerInfoListener(InfoListener)}
     * @param l The listener to be removed. If it's not currently registered, then nothing happens
     */
    public static void unregisterInfoListener(InfoListener l) {
        infoListeners.remove(l);
    }

    /**
     * Register a callback for when the Player Service encounters an exception that affects
     * music playback that the user should be alerted of. Don't forget to unregister this listener
     * when you're done, otherwise you'll probably leak an Activity or something bad.
     * @param l The ErrorListener to be registered
     * @see #unregisterErrorListener(ErrorListener)
     */
    public static void registerErrorListener(ErrorListener l) {
        errorListeners.add(l);
    }

    /**
     * Unregister an Error Listener callback set in {@link #registerErrorListener(ErrorListener)}
     * @param l The Listener to be removed. If it's not currently registered, then nothing
     *          interesting happens.
     */
    public static void unregisterErrorListener(ErrorListener l) {
        errorListeners.remove(l);
    }

    /**
     * Called to alert all Update Listeners that the Player's state has changed
     */
    private static void updateUi() {
        for (UpdateListener l : updateListeners) {
            l.onUpdate();
        }
    }

    /**
     * Called to alert all Info Listeners that a significant event has occurred that the service
     * would like to inform the user of
     * @param message The user-friendly String sent by the player service
     */
    private static void alertInfo(String message) {
        for (InfoListener l : infoListeners) {
            l.onInfo(message);
        }
    }

    /**
     * Called to alert all Error Listeners that an error has occurred
     * @param message The detailed message of the error that the Player Service sent
     */
    private static void alertError(String message) {
        for (ErrorListener l : errorListeners) {
            l.onError(message);
        }
    }

    /**
     * Stop playback completely and end the player service process. If you call this from the UI
     * thread make sure that you don't want to play music for the rest of the lifetime of the
     * process, otherwise you're going to have a bad time.
     * See {@link MusicPlayer#stop()}
     */
    public static void stop() {
        if (playerService != null) {
            try {
                playerService.stop();
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to stop playback");
            }
        }
    }

    /**
     * Skip to the next song in the queue.
     * See {@link MusicPlayer#skip()}
     */
    public static void skip() {
        if (playerService != null) {
            try {
                playerService.skip();
                artwork = null;
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip current track");
            }
        }
    }

    /**
     * Skip to the previous song in the queue
     * See {@link MusicPlayer#skipPrevious()}
     */
    public static void previous() {
        if (playerService != null) {
            try {
                playerService.previous();
                artwork = null;
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip backward");
            }
        }
    }

    /**
     * Begin playback of a new song
     * See {@link MusicPlayer#prepare(boolean)}
     */
    public static void begin() {
        if (playerService != null) {
            try {
                playerService.begin();
                artwork = null;
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to begin playback");
            }
        }
    }

    /**
     * Toggle between play and pause states.
     * See {@link MusicPlayer#togglePlay()}
     */
    public static void togglePlay() {
        if (playerService != null) {
            try {
                playerService.togglePlay();
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to toggle playback");
            }
        }
    }

    /**
     * Start music playback of the current song
     * See {@link MusicPlayer#play()}
     */
    public static void play() {
        if (playerService != null) {
            try {
                playerService.play();
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to resume playback");
            }
        }
    }

    /**
     * Pause music playback
     * See {@link MusicPlayer#pause()}
     */
    public static void pause() {
        if (playerService != null) {
            try {
                playerService.pause();
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to pause playback");
            }
        }
    }

    public static void updatePlayerPreferences(ReadOnlyPreferencesStore preferencesStore) {
        if (playerService != null) {
            try {
                playerService.setPreferences(new ImmutablePreferenceStore(preferencesStore));
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to update remote player preferences");
            }
        }
    }

    /**
     * Replace the contents of the queue with a new list of songs
     * @param newQueue An {@link List<Song>} to become the new queue
     * @param newPosition The index of the list to start playback from
     * See {@link MusicPlayer#setQueue(List, int)}
     */
    public static void setQueue(final List<Song> newQueue, final int newPosition) {
        if (playerService != null) {
            try {
                playerService.setQueue(newQueue, newPosition);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to set queue");
            }
        }
    }

    /**
     * Removes all songs from the queue and stops playback
     */
    public static void clearQueue() {
        if (playerService != null) {
            try {
                playerService.setQueue(new ArrayList<>(), 0);
                artwork = null;
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to clear queue");
            }
        }
    }

    /**
     * Change the current index of the queue to a new position
     * @param queuePosition The new index of the queue
     * See {@link MusicPlayer#changeSong(int)}
     */
    public static void changeSong(int queuePosition) {
        if (playerService != null) {
            try {
                playerService.changeSong(queuePosition);
                artwork = null;
                updateUi();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to change song");
            }
        }
    }

    /**
     * Edit the contents of the queue without interrupting playback
     * @param queue An {@link List<Song>} to become the new queue
     * @param queuePosition The index of the currently playing song in the new queue
     * See {@link MusicPlayer#editQueue(List, int)}
     */
    public static void editQueue(List<Song> queue, int queuePosition) {
        if (playerService != null) {
            try {
                playerService.editQueue(queue, queuePosition);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to edit queue");
            }
        }
    }

    /**
     * Enqueue a song so that it plays after the current song
     * @param song The {@link Song} to play next
     * See {@link MusicPlayer#queueNext(Song)}
     */
    public static void queueNext(final Song song) {
        if (playerService != null) {
            try {
                playerService.queueNext(song);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue next song");
            }
        }
    }

    /**
     * Enqueue a list of songs so that they play after the current songs
     * @param songs A {@link List<Song>} to play next
     * See {@link MusicPlayer#queueNext(List)}
     */
    public static void queueNext(final List<Song> songs) {
        if (playerService != null) {
            try {
                playerService.queueNextList(songs);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue next songs");
            }
        }
    }

    /**
     * Add a song to the end of the queue
     * @param song The {@link Song} to queue
     * See {@link MusicPlayer#queueLast(Song)}
     */
    public static void queueLast(final Song song) {
        if (playerService != null) {
            try {
                playerService.queueLast(song);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue last song");
            }
        }
    }

    /**
     * Add a list of songs to the end of the queue
     * @param songs A {@link List<Song>} to place at the end of the queue
     * See {@link MusicPlayer#queueLast(List)}
     */
    public static void queueLast(final List<Song> songs) {
        if (playerService != null) {
            try {
                playerService.queueLastList(songs);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue last songs");
            }
        }
    }

    /**
     * Seek to a different time in the current song
     * @param position The new seek position in milliseconds
     */
    public static void seek(final int position) {
        if (playerService != null) {
            try {
                playerService.seekTo(position);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to seek");
            }
        }
    }

    /**
     * @return if the player service is currently playing music
     */
    public static boolean isPlaying() {
        if (playerService == null) {
            return false;
        }

        try {
            return playerService.isPlaying();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to check playing state");
            return false;
        }
    }

    /**
     * @return The song currently being played by the player service (null if nothing is playing)
     */
    public static Song getNowPlaying() {
        if (playerService == null) {
            return null;
        }

        try {
            return playerService.getNowPlaying();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get current song");
            return null;
        }
    }

    /**
     * @return The current queue of the player service
     */
    public static List<Song> getQueue() {
        if (playerService == null) {
            return Collections.emptyList();
        }

        try {
            return playerService.getQueue();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get current queue");
            return Collections.emptyList();
        }
    }

    /**
     * @return The index of the currently playing song in the player service's queue
     */
    public static int getQueuePosition() {
        if (playerService == null) {
            return 0;
        }

        try {
            return playerService.getQueuePosition();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get current queue position");
            return 0;
        }
    }

    /**
     * @return The number of items in the current queue
     */
    public static int getQueueSize() {
        if (playerService == null) {
            return 0;
        }

        try {
            return playerService.getQueueSize();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get queue size");
            return 0;
        }
    }

    /**
     * @return The current seek position of the now playing song in milliseconds
     */
    public static int getCurrentPosition() {
        if (playerService == null) {
            return 0;
        }

        try {
            return playerService.getCurrentPosition();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get current position");
            return 0;
        }
    }

    /**
     * @return The total duration of the currently playing song
     */
    public static int getDuration() {
        if (playerService == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return playerService.getDuration();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get song duration");
            return Integer.MAX_VALUE;
        }
    }

    public static int getMultiRepeatCount() {
        if (playerService == null) {
            return 0;
        }

        try {
            return playerService.getMultiRepeatCount();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get Multi-Repeat count");
            return 0;
        }
    }

    public static void setMultiRepeatCount(int count) {
        if (playerService == null) {
            return;
        }

        try {
            playerService.setMultiRepeatCount(count);
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to set Multi-Repeat count");
        }
    }

    public static long getSleepTimerEndTime() {
        if (playerService == null) {
            return 0;
        }

        try {
            return playerService.getSleepTimerEndTime();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get sleep timer value");
            return 0;
        }
    }

    public static void setSleepTimerEndTime(long timestampInMillis) {
        if (playerService == null) {
            return;
        }

        try {
            playerService.setSleepTimerEndTime(timestampInMillis);
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to set sleep timer value");
        }
    }

    public static void disableSleepTimer() {
        setSleepTimerEndTime(0);
    }

    /**
     * @return The album artwork for the current song
     */
    public static Bitmap getArtwork() {
        if (artwork == null) {
            artwork = Util.fetchFullArt(getNowPlaying());
        }
        return artwork;
    }

    public static RemoteEqualizer getEqualizer() {
        if (playerService == null) {
            return null;
        }

        try {
            return playerService.getEqualizer();
        } catch (RemoteException exception) {
            Timber.e(exception, "Failed to get remote equalizer");
            return null;
        }
    }

    /**
     * A {@link BroadcastReceiver} class listening for intents with an
     * {@link MusicPlayer#UPDATE_BROADCAST} action. This broadcast must be sent ordered with this
     * receiver being the highest priority so that the UI can access this class for accurate
     * information from the player service
     */
    public static class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicPlayer.UPDATE_BROADCAST)) {
                artwork = null;
                updateUi();
            } else if (intent.getAction().equals(MusicPlayer.INFO_BROADCAST)) {
                alertInfo(intent.getExtras().getString(MusicPlayer.INFO_EXTRA_MESSAGE));
            } else if (intent.getAction().equals(MusicPlayer.ERROR_BROADCAST)) {
                alertError(intent.getExtras().getString(MusicPlayer.ERROR_EXTRA_MSG));
            }
        }

    }

    public interface UpdateListener {
        void onUpdate();
    }

    public interface InfoListener {
        void onInfo(String message);
    }

    public interface ErrorListener {
        void onError(String message);
    }
}
