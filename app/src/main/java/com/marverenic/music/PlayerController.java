package com.marverenic.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Fetch;

import java.util.ArrayList;

public class PlayerController {

    private static Context applicationContext;
    private static Player.State playerState;
    private static Bitmap artwork;

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
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Since we communicate to the {@link PlayerService} using {@link Intent}s, we have to build a
     * lot of Intents that look really similar. This method makes template Intents that will be
     * sent to the service and optionally have an action already set
     * @param action Optional; set the action of the intent. If null, no action will be set
     * @return A template {@link Intent} that can be sent with {@link Context#sendBroadcast(Intent)},
     *         or have more information added first.
     */
    private static Intent getBaseIntent(@Nullable String action){
        Intent i = new Intent(applicationContext, PlayerService.Listener.class);
        if (action != null) i.setAction(action);

        return i;
    }

    /**
     * Request the player service to send information about the {@link Player}'s status to the
     * UI process with an {@link Intent} with the {@link Player#UPDATE_BROADCAST} action
     */
    public static void requestSync() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_REQUEST_SYNC));
    }

    /**
     * Stop playback completely and end the player service process. If you call this from the UI
     * thread make sure that you don't want to play music for the rest of the lifetime of the
     * process, otherwise you're going to have a bad time.
     * See {@link PlayerService#stop()}
     */
    public static void stop() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_STOP));
        applicationContext = null;
        playerState = null;
        artwork = null;
    }

    /**
     * Skip to the next song in the queue.
     * See {@link Player#skip()}
     */
    public static void skip() {
        if (playerState != null && playerState.queuePosition < playerState.queue.size()) ++playerState.queuePosition;
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_NEXT));
    }

    /**
     * Skip to the previous song in the queue
     * See {@link Player#previous()}
     */
    public static void previous() {
        if (playerState != null){
            playerState.position = 0;
            playerState.bundleTime = System.currentTimeMillis();

            if (playerState.queuePosition > 0)
                --playerState.queuePosition;
        }
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PREV));
    }

    /**
     * Begin playback of a new song
     * See {@link Player#begin()}
     */
    public static void begin(){
        if (playerState != null){
            playerState.position = 0;
            playerState.bundleTime = System.currentTimeMillis();
            playerState.isPlaying = true;
        }
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_BEGIN));
    }

    /**
     * Toggle between play and pause states.
     * See {@link Player#togglePlay()}
     */
    public static void togglePlay() {
        if (playerState != null){
            playerState.position = getCurrentPosition();
            playerState.bundleTime = System.currentTimeMillis();
            playerState.isPlaying = !playerState.isPlaying;
            playerState.isPaused = !playerState.isPaused;
        }
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_TOGGLE_PLAY));
    }

    /**
     * Start music playback of the current song
     * See {@link Player#play()}
     */
    public static void play() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PLAY));
    }

    /**
     * Pause music playback
     * See {@link Player#pause()}
     */
    public static void pause() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PAUSE));
    }

    /**
     * Toggle repeat from {@link Player#REPEAT_NONE} to {@link Player#REPEAT_ALL},
     * from {@link Player#REPEAT_ALL} to {@link Player#REPEAT_ONE}
     * and from {@link Player#REPEAT_ONE} to {@link Player#REPEAT_NONE}
     * in {@link android.content.SharedPreferences} and notify the service about the preference change
     * See {@link Player#setPrefs(boolean, short)}
     */
    public static void toggleRepeat() {
        short repeatOption;
        switch ((short) PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE)){
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

        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putInt(Player.PREFERENCE_REPEAT, repeatOption).apply();

        Intent prefIntent = getBaseIntent(PlayerService.ACTION_SET_PREFS);
        prefIntent.putExtra(PlayerService.EXTRA_PREF_REPEAT, repeatOption);
        applicationContext.sendBroadcast(prefIntent);
    }

    /**
     * Toggle shuffle on or off in {@link android.content.SharedPreferences} and notify the service
     * about the preference change
     * See {@link Player#setPrefs(boolean, short)}
     */
    public static void toggleShuffle() {
        boolean shuffleOption = !PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean(Player.PREFERENCE_SHUFFLE, false);
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean(Player.PREFERENCE_SHUFFLE, shuffleOption).apply();

        Intent prefIntent = getBaseIntent(PlayerService.ACTION_SET_PREFS);
        prefIntent.putExtra(PlayerService.EXTRA_PREF_SHUFFLE, shuffleOption);
        applicationContext.sendBroadcast(prefIntent);
    }

    /**
     * Replace the contents of the queue with a new list of songs
     * @param newQueue An {@link ArrayList<Song>} to become the new queue
     * @param newPosition The index of the list to start playback from
     * See {@link Player#setQueue(ArrayList, int)}
     */
    public static void setQueue(final ArrayList<Song> newQueue, final int newPosition) {
        if (playerState != null) {
            playerState.queue = newQueue;
            playerState.queuePosition = newPosition;
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_SET_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, newQueue);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_LIST_POSITION, newPosition);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Change the current index of the queue to a new position
     * @param queuePosition The new index of the queue
     * See {@link Player#changeSong(int)}
     */
    public static void changeSong(int queuePosition) {
        playerState.queuePosition = queuePosition;

        Intent changeSongIntent = getBaseIntent(PlayerService.ACTION_CHANGE_SONG);
        changeSongIntent.putExtra(PlayerService.EXTRA_QUEUE_LIST_POSITION, queuePosition);

        applicationContext.sendBroadcast(changeSongIntent);
    }

    /**
     * Edit the contents of the queue without interrupting playback
     * @param queue An {@link ArrayList<Song>} to become the new queue
     * @param queuePosition The index of the currently playing song in the new queue
     * See {@link Player#editQueue(ArrayList, int)}
     */
    public static void editQueue(ArrayList<Song> queue, int queuePosition){
        playerState.queue = queue;
        playerState.queuePosition = queuePosition;

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_EDIT_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, queue);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_LIST_POSITION, queuePosition);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Enqueue a song so that it plays after the current song
     * @param song The {@link Song} to play next
     * See {@link Player#queueNext(Song)}
     */
    public static void queueNext(final Song song) {
        if (playerState != null) {
            playerState.queue.add(playerState.queuePosition + 1, song);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_SONG, song);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, true);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Enqueue a list of songs so that they play after the current songs
     * @param songs A {@link ArrayList<Song>} to play next
     * See {@link Player#queueNext(ArrayList)}
     */
    public static void queueNext(final ArrayList<Song> songs) {
        if (playerState != null) {
            playerState.queue.addAll(playerState.queuePosition + 1, songs);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, songs);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, true);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Add a song to the end of the queue
     * @param song The {@link Song} to queue
     * See {@link Player#queueLast(Song)}
     */
    public static void queueLast(final Song song) {
        if (playerState != null) {
            playerState.queue.add(song);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_SONG, song);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, false);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Add a list of songs to the end of the queue
     * @param songs A {@link ArrayList<Song>} to place at the end of the queue
     * See {@link Player#queueLast(ArrayList)}
     */
    public static void queueLast(final ArrayList<Song> songs) {
        if (playerState != null) {
            playerState.queue.addAll(songs);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, songs);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, false);

        applicationContext.sendBroadcast(queueIntent);
    }

    /**
     * Seek to a different time in the current song
     * @param position The new seek position in milliseconds
     */
    public static void seek(final int position) {
        if (playerState != null) {
            playerState.position = position;
            playerState.bundleTime = System.currentTimeMillis();
        }

        Intent seekIntent = getBaseIntent(PlayerService.ACTION_SEEK);
        seekIntent.putExtra(PlayerService.EXTRA_POSITION, position);

        applicationContext.sendBroadcast(seekIntent);
    }

    /**
     * @return if the player service is currently playing music
     */
    public static boolean isPlaying() {
        return playerState != null && playerState.isPlaying;
    }

    /**
     * @return if the player service is currently preparing to play a song
     */
    public static boolean isPreparing() {
        return playerState != null && playerState.isPreparing;
    }

    /**
     * @return if shuffle is enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isShuffle() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean(Player.PREFERENCE_SHUFFLE, false);
    }

    /**
     * @return whether repeat all is currently enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isRepeat() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ALL;
    }

    /**
     * @return whether repeat one is currently enabled in {@link android.content.SharedPreferences}
     */
    public static boolean isRepeatOne() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ONE;
    }

    /**
     * @return The song currently being played by the player service (null if nothing is playing)
     */
    public static Song getNowPlaying() {
        if (playerState != null && playerState.queuePosition < playerState.queue.size())
            return playerState.queue.get(playerState.queuePosition);
        return null;
    }

    /**
     * @return The current queue of the player service
     */
    public static ArrayList<Song> getQueue() {
        if (playerState != null) return playerState.queue;
        return new ArrayList<>();
    }

    /**
     * @return The index of the currently playing song in the player service's queue
     */
    public static int getQueuePosition() {
        if (playerState != null) return playerState.queuePosition;
        return 0;
    }

    /**
     * @return The current seek position of the now playing song in milliseconds
     */
    public static int getCurrentPosition() {
        if (playerState == null) return 0;
        if (!isPlaying()) return playerState.position;

        long dT = System.currentTimeMillis() - playerState.bundleTime;
        return playerState.position + (int) dT;
    }

    /**
     * @return The total duration of the currently playing song
     */
    public static int getDuration() {
        if (playerState != null) return playerState.duration;
        return Integer.MAX_VALUE;
    }

    /**
     * @return The album artwork for the current song
     */
    public static Bitmap getArtwork() {
        if (artwork == null) artwork = Fetch.fetchFullArt(getNowPlaying());
        return artwork;
    }

    /**
     * A {@link BroadcastReceiver} class listening for intents with an {@link Player#UPDATE_BROADCAST}
     * action. This broadcast must be sent ordered with this receiver being the highest priority
     * so that the UI can access this class for accurate information from the player service
     */
    public static class Listener extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Player.UPDATE_BROADCAST)){
                playerState = intent.getParcelableExtra(Player.UPDATE_EXTRA);
                artwork = null;
            }
        }

    }
}
