package com.marverenic.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.preference.PreferenceManager;

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
     *                THIS {@link Context} IS NEVER RELEASED and may cause a leak
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

    private static Intent getBaseIntent(String action){
        Intent i = new Intent(applicationContext, PlayerService.Listener.class);
        if (action != null) i.setAction(action);

        return i;
    }

    public static void requestSync() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_REQUEST_SYNC));
    }

    public static void stop() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_STOP));
    }

    public static void skip() {
        if (playerState != null && playerState.queuePosition < playerState.queue.size()) ++playerState.queuePosition;
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_NEXT));
    }

    public static void previous() {
        if (playerState != null && playerState.queuePosition > 0) --playerState.queuePosition;
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PREV));
    }

    public static void begin(){
        if (playerState != null){
            playerState.position = 0;
            playerState.isPlaying = true;
        }
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_BEGIN));
    }

    public static void togglePlay() {
        if (playerState != null){
            playerState.isPlaying = !playerState.isPlaying;
            playerState.isPaused = !playerState.isPaused;
        }
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_TOGGLE_PLAY));
    }

    public static void play() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PLAY));
    }

    public static void pause() {
        applicationContext.sendBroadcast(getBaseIntent(PlayerService.ACTION_PAUSE));
    }

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

    public static void toggleShuffle() {
        boolean shuffleOption = !PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean(Player.PREFERENCE_SHUFFLE, false);
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putBoolean(Player.PREFERENCE_SHUFFLE, shuffleOption).apply();

        Intent prefIntent = getBaseIntent(PlayerService.ACTION_SET_PREFS);
        prefIntent.putExtra(PlayerService.EXTRA_PREF_SHUFFLE, shuffleOption);
        applicationContext.sendBroadcast(prefIntent);
    }

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

    public static void changeSong(int queuePosition) {
        playerState.queuePosition = queuePosition;

        Intent changeSongIntent = getBaseIntent(PlayerService.ACTION_CHANGE_SONG);
        changeSongIntent.putExtra(PlayerService.EXTRA_QUEUE_LIST_POSITION, queuePosition);

        applicationContext.sendBroadcast(changeSongIntent);
    }

    public static void editQueue(ArrayList<Song> queue, int queuePosition){
        playerState.queue = queue;
        playerState.queuePosition = queuePosition;

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_EDIT_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, queue);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_LIST_POSITION, queuePosition);

        applicationContext.sendBroadcast(queueIntent);
    }

    public static void queueNext(final Song song) {
        if (playerState != null) {
            playerState.queue.add(playerState.queuePosition + 1, song);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_SONG, song);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, true);

        applicationContext.sendBroadcast(queueIntent);
    }

    public static void queueNext(final ArrayList<Song> songs) {
        if (playerState != null) {
            playerState.queue.addAll(playerState.queuePosition + 1, songs);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, songs);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, true);

        applicationContext.sendBroadcast(queueIntent);
    }

    public static void queueLast(final Song song) {
        if (playerState != null) {
            playerState.queue.add(song);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_SONG, song);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, false);

        applicationContext.sendBroadcast(queueIntent);
    }

    public static void queueLast(final ArrayList<Song> songs) {
        if (playerState != null) {
            playerState.queue.addAll(songs);
        }

        Intent queueIntent = getBaseIntent(PlayerService.ACTION_QUEUE);
        queueIntent.putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE_LIST, songs);
        queueIntent.putExtra(PlayerService.EXTRA_QUEUE_NEXT, false);

        applicationContext.sendBroadcast(queueIntent);
    }

    public static void seek(final int position) {
        if (playerState != null) {
            playerState.position = position;
            playerState.bundleTime = System.currentTimeMillis();
        }

        Intent seekIntent = getBaseIntent(PlayerService.ACTION_SEEK);
        seekIntent.putExtra(PlayerService.EXTRA_POSITION, position);

        applicationContext.sendBroadcast(seekIntent);
    }

    public static boolean isPlaying() {
        return playerState != null && playerState.isPlaying;
    }

    public static boolean isPreparing() {
        return playerState != null && playerState.isPreparing;
    }

    public static boolean isShuffle() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getBoolean(Player.PREFERENCE_SHUFFLE, false);
    }

    public static boolean isRepeat() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ALL;
    }

    public static boolean isRepeatOne() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext).getInt(Player.PREFERENCE_REPEAT, Player.REPEAT_NONE) == Player.REPEAT_ONE;
    }

    public static Song getNowPlaying() {
        if (playerState != null && playerState.queuePosition < playerState.queue.size())
            return playerState.queue.get(playerState.queuePosition);
        return null;
    }

    public static ArrayList<Song> getQueue() {
        if (playerState != null) return playerState.queue;
        return new ArrayList<>();
    }

    public static int getQueuePosition() {
        if (playerState != null) return playerState.queuePosition;
        return 0;
    }

    public static int getCurrentPosition() {
        if (playerState == null) return 0;
        if (!isPlaying()) return playerState.position;

        long dT = System.currentTimeMillis() - playerState.bundleTime;
        return playerState.position + (int) dT;
    }

    public static int getDuration() {
        if (playerState != null) return playerState.duration;
        return Integer.MAX_VALUE;
    }

    public static Bitmap getArtwork() {
        if (artwork == null) artwork = Fetch.fetchFullArt(getNowPlaying());
        return artwork;
    }

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
