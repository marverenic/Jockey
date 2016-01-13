package com.marverenic.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlayerController {

    private static final String TAG = "PlayerController";

    private static Context applicationContext;
    private static IPlayerService playerService;
    private static Set<UpdateListener> updateListeners;
    private static Bitmap artwork;

    static {
        updateListeners = new HashSet<>();
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
            applicationContext = context;

            Intent serviceIntent = new Intent(context, PlayerService.class);
            context.bindService(serviceIntent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    playerService = IPlayerService.Stub.asInterface(service);
                    updateUi();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    playerService = null;
                }
            }, Context.BIND_AUTO_CREATE);
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
     * Called to alert all Update Listeners that the Player's state has changed
     */
    private static void updateUi() {
        for (UpdateListener l : updateListeners) {
            l.onUpdate();
        }
    }

    /**
     * Stop playback completely and end the player service process. If you call this from the UI
     * thread make sure that you don't want to play music for the rest of the lifetime of the
     * process, otherwise you're going to have a bad time.
     * See {@link PlayerService#stop()}
     */
    public static void stop() {
        if (playerService != null) {
            try {
                playerService.stop();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Skip to the next song in the queue.
     * See {@link Player#skip()}
     */
    public static void skip() {
        if (playerService != null) {
            try {
                playerService.skip();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Skip to the previous song in the queue
     * See {@link Player#previous()}
     */
    public static void previous() {
        if (playerService != null) {
            try {
                playerService.previous();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Begin playback of a new song
     * See {@link Player#begin()}
     */
    public static void begin() {
        if (playerService != null) {
            try {
                playerService.begin();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Toggle between play and pause states.
     * See {@link Player#togglePlay()}
     */
    public static void togglePlay() {
        if (playerService != null) {
            try {
                playerService.togglePlay();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Start music playback of the current song
     * See {@link Player#play()}
     */
    public static void play() {
        if (playerService != null) {
            try {
                playerService.play();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Pause music playback
     * See {@link Player#pause()}
     */
    public static void pause() {
        if (playerService != null) {
            try {
                playerService.pause();
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Toggle repeat from {@link Player#REPEAT_NONE} to {@link Player#REPEAT_ALL},
     * from {@link Player#REPEAT_ALL} to {@link Player#REPEAT_ONE}
     * and from {@link Player#REPEAT_ONE} to {@link Player#REPEAT_NONE}
     * in {@link android.content.SharedPreferences} and notify the service about the
     * preference change
     * See {@link Player#setPrefs(boolean, short)}
     */
    public static void toggleRepeat() {
        short repeatOption;
        switch ((short) PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE)) {
            case Player.REPEAT_ONE:
                repeatOption = Player.REPEAT_NONE;
                break;
            case Player.REPEAT_ALL:
                repeatOption = Player.REPEAT_ONE;
                break;
            case Player.REPEAT_NONE:
            default:
                repeatOption = Player.REPEAT_ALL;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        prefs.edit().putInt(Player.PREFERENCE_REPEAT, repeatOption).apply();

        if (playerService  != null) {
            try {
                playerService.setPrefs(
                        prefs.getBoolean(Player.PREFERENCE_SHUFFLE, false), repeatOption);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Toggle shuffle on or off in {@link android.content.SharedPreferences} and notify the service
     * about the preference change
     * See {@link Player#setPrefs(boolean, short)}
     */
    public static void toggleShuffle() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        boolean shuffleOption = !prefs.getBoolean(Player.PREFERENCE_SHUFFLE, false);
        prefs.edit().putBoolean(Player.PREFERENCE_SHUFFLE, shuffleOption).apply();

        if (playerService != null) {
            try {
                playerService.setPrefs(shuffleOption, prefs.getInt(Player.PREFERENCE_REPEAT, 0));
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Replace the contents of the queue with a new list of songs
     * @param newQueue An {@link List<Song>} to become the new queue
     * @param newPosition The index of the list to start playback from
     * See {@link Player#setQueue(List, int)}
     */
    public static void setQueue(final List<Song> newQueue, final int newPosition) {
        if (Prefs.allowAnalytics(applicationContext)) {
            Answers.getInstance().logCustom(
                    new CustomEvent("Changed queue")
                            .putCustomAttribute(
                                    "Queue size",
                                    newQueue.size())
                            .putCustomAttribute(
                                    "Queue index",
                                    newPosition)
                            .putCustomAttribute(
                                    "Song duration (sec)",
                                    newQueue.get(newPosition).getSongDuration() / 1000));
        }
        if (playerService != null) {
            try {
                playerService.setQueue(newQueue, newPosition);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Change the current index of the queue to a new position
     * @param queuePosition The new index of the queue
     * See {@link Player#changeSong(int)}
     */
    public static void changeSong(int queuePosition) {
        if (playerService != null) {
            try {
                playerService.changeSong(queuePosition);
                updateUi();
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Edit the contents of the queue without interrupting playback
     * @param queue An {@link List<Song>} to become the new queue
     * @param queuePosition The index of the currently playing song in the new queue
     * See {@link Player#editQueue(List, int)}
     */
    public static void editQueue(List<Song> queue, int queuePosition) {
        if (playerService != null) {
            try {
                playerService.editQueue(queue, queuePosition);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);

            }
        }
    }

    /**
     * Enqueue a song so that it plays after the current song
     * @param song The {@link Song} to play next
     * See {@link Player#queueNext(Song)}
     */
    public static void queueNext(final Song song) {
        if (playerService != null) {
            try {
                playerService.queueNext(song);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Enqueue a list of songs so that they play after the current songs
     * @param songs A {@link List<Song>} to play next
     * See {@link Player#queueNext(List)}
     */
    public static void queueNext(final List<Song> songs) {
        if (playerService != null) {
            try {
                playerService.queueNextList(songs);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Add a song to the end of the queue
     * @param song The {@link Song} to queue
     * See {@link Player#queueLast(Song)}
     */
    public static void queueLast(final Song song) {
        if (playerService != null) {
            try {
                playerService.queueLast(song);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
            }
        }
    }

    /**
     * Add a list of songs to the end of the queue
     * @param songs A {@link List<Song>} to place at the end of the queue
     * See {@link Player#queueLast(List)}
     */
    public static void queueLast(final List<Song> songs) {
        if (playerService != null) {
            try {
                playerService.queueLastList(songs);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
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
                playerService.seek(position);
            } catch (RemoteException e) {
                Crashlytics.logException(e);
                Log.w(TAG, e);
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
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return false;
        }
    }

    /**
     * @return if the player service is currently preparing to play a song
     */
    public static boolean isPreparing() {
        if (playerService == null) {
            return false;
        }

        try {
            return playerService.isPreparing();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return false;
        }
    }

    /**
     * @return if shuffle is enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isShuffle() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getBoolean(Player.PREFERENCE_SHUFFLE, false);
    }

    /**
     * @return whether repeat all is currently enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isRepeat() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ALL;
    }

    /**
     * @return whether repeat one is currently enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isRepeatOne() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ONE;
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
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return null;
        }
    }

    /**
     * @return The current queue of the player service
     */
    public static List<Song> getQueue() {
        if (playerService == null) {
            return new ArrayList<>();
        }

        try {
            return playerService.getQueue();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return new ArrayList<>();

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
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
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
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return 0;
        }
    }

    /**
     * @return The total duration of the currently playing song
     */
    public static int getDuration() {
        try {
            return playerService.getDuration();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return Integer.MAX_VALUE;
        }
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

    /**
     * @return The Audio Session Id from {@link MediaPlayer#getAudioSessionId()}. If an exception
     *         was raised, 0 is returned.
     */
    public static int getAudioSessionId() {
        try {
            return playerService.getAudioSessionId();
        } catch (RemoteException e) {
            Crashlytics.logException(e);
            Log.w(TAG, e);
            return 0;
        }
    }

    /**
     * A {@link BroadcastReceiver} class listening for intents with an
     * {@link Player#UPDATE_BROADCAST} action. This broadcast must be sent ordered with this
     * receiver being the highest priority so that the UI can access this class for accurate
     * information from the player service
     */
    public static class Listener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Player.UPDATE_BROADCAST)) {
                artwork = null;
                updateUi();
            }
        }

    }

    public interface UpdateListener {
        void onUpdate();
    }
}
