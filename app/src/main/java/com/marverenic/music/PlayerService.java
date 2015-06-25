package com.marverenic.music;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.marverenic.music.activity.LibraryActivity;
import com.marverenic.music.instances.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerService extends Service {

    private static final String TAG = "PlayerService";
    private static final boolean debug = BuildConfig.DEBUG;

    public static final int NOTIFICATION_ID = 1;

    // Intent Action & Extra names
    /**
     * Toggle between play and pause
     */
    public static final String ACTION_TOGGLE_PLAY = "com.marverenic.music.action.TOGGLE_PLAY";
    /**
     * Starts playback of current song in the queue (See {@link Player#begin()})
     */
    public static final String ACTION_BEGIN = "com.marverenic.music.action.BEGIN";
    /**
     * Pause playback
     */
    public static final String ACTION_PAUSE = "com.marverenic.music.action.PAUSE";
    /**
     * Resume playback
     */
    public static final String ACTION_PLAY = "com.marverenic.music.action.PLAY";
    /**
     * Skip to the previous song
     */
    public static final String ACTION_PREV = "com.marverenic.music.action.PREVIOUS";
    /**
     * Skip to the next song
     */
    public static final String ACTION_NEXT = "com.marverenic.music.action.NEXT";
    /**
     * Stop playback and kill service
     */
    public static final String ACTION_STOP = "com.marverenic.music.action.STOP";
    /**
     * Seek within the current song
     */
    public static final String ACTION_SEEK = "com.marverenic.music.action.SEEK";
    /**
     * Begin playback a different song in the queue
     * Make sure to pass {@link PlayerService#EXTRA_QUEUE_LIST_POSITION}
     */
    public static final String ACTION_CHANGE_SONG = "com.marverenic.music.action.CHANGE_SONG";
    /**
     * Replace the songs in the queue
     */
    public static final String ACTION_SET_QUEUE = "com.marverenic.music.action.CHANGE_QUEUE";
    /**
     * Edit the contents and current position of the queue without changing the current song
     */
    public static final String ACTION_EDIT_QUEUE = "com.marverenic.music.action.EDIT_QUEUE";
    /**
     * Add an item or list of items to the queue
     */
    public static final String ACTION_QUEUE = "com.marverenic.music.action.ADD_QUEUE";
    /**
     * Sync shuffle and repeat preferences with SharedPreferences. Pass {@link PlayerService#EXTRA_PREF_SHUFFLE}
     * and/or {@link PlayerService#EXTRA_PREF_REPEAT} as applicable
     */
    public static final String ACTION_SET_PREFS = "com.marverenic.music.action.PREF_EDIT";
    /**
     * Request an update broadcast to be sent
     * Used to keep the processes in sync with each other
     */
    public static final String ACTION_REQUEST_SYNC = "com.marverenic.music.action.SYNC_PROCESSES";
    /**
     * The position to seek to. Pass as an int extra with {@link PlayerService#ACTION_SEEK}
     */
    public static final String EXTRA_POSITION = "com.marverenic.music.extra.SEEK_POSITION";
    /**
     * The {@link Song} to queue. Pass as a parcelable extra with {@link PlayerService#ACTION_QUEUE}
     */
    public static final String EXTRA_QUEUE_SONG = "com.marverenic.music.extra.QUEUE_ITEM";
    /**
     * The {@link ArrayList<Song>} to queue. Pass as a parcelable extra with
     * {@link PlayerService#ACTION_QUEUE} or {@link PlayerService#ACTION_EDIT_QUEUE}
     */
    public static final String EXTRA_QUEUE_LIST = "com.marverenic.music.extra.QUEUE_LIST";
    /**
     * Whether or not to queue the song(s) next or not. Pass as a boolean extra with
     * {@link PlayerService#ACTION_QUEUE}. If a value isn't passed, then false is assumed
     */
    public static final String EXTRA_QUEUE_NEXT = "com.marverenic.music.extra.QUEUE_LAST";
    /**
     * The starting position of the new queue.
     * Pass as an int extra with {@link PlayerService#ACTION_SET_QUEUE}, {@link PlayerService#ACTION_EDIT_QUEUE},
     * or {@link PlayerService#ACTION_QUEUE} with {@link PlayerService#EXTRA_QUEUE_LIST} specified
     */
    public static final String EXTRA_QUEUE_LIST_POSITION = "com.marverenic.music.extra.QUEUE_POSITION";
    /**
     * Pass as an extra with {@link PlayerService#ACTION_SET_PREFS} to change the shuffle setting
     */
    public static final String EXTRA_PREF_SHUFFLE = "com.marverenic.music.extra.SHUFFLE_PREF";
    /**
     * Pass as an extra with {@link PlayerService#ACTION_SET_PREFS} to change the repeat setting
     */
    public static final String EXTRA_PREF_REPEAT = "com.marverenic.music.extra.REPEAT_PREF";

    /**
     * The service instance in use (singleton)
     */
    private static PlayerService instance;

    // Instance variables
    /**
     * The media player for the service instance
     */
    private Player player;
    private boolean finished = false; // Don't attempt to release resources more than once

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (debug) Log.i(TAG, "onCreate() called");

        if (instance == null){
            instance = this;
        }
        else{
            if (debug) Log.w(TAG, "Attempted to create a second PlayerService");
            stopSelf();
            return;
        }

        if (player == null){
            player = new Player(this);
        }

        player.reload();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId ){
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if (debug) Log.i(TAG, "Called onDestroy()");
        try{
            player.saveState("");
        }
        catch (Exception ignored){}
        finish();
        super.onDestroy();
    }

    public static PlayerService getInstance(){
        return instance;
    }

    /**
     * Generate and post a notification for the current player status
     * Posts the notification by starting the service in the foreground
     */
    public void notifyNowPlaying() {
        if (debug) Log.i(TAG, "notifyNowPlaying() called");

        // The intent to use for the notification buttons
        Intent intent = new Intent(this, Listener.class);

        // Create the compact view
        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.notification);
        // Create the expanded view
        RemoteViews notificationViewExpanded = new RemoteViews(getPackageName(), R.layout.notification_expanded);

        // Set the artwork for the notification
        if (player.getArt() != null) {
            notificationView.setImageViewBitmap(R.id.notificationIcon, player.getArt());
            notificationViewExpanded.setImageViewBitmap(R.id.notificationIcon, player.getArt());
        }
        else {
            notificationView.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
            notificationViewExpanded.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
        }

        // If the player is playing music, set the track info and the button intents
        if (player.getNowPlaying() != null) {
            // Update the info for the compact view
            notificationView.setTextViewText(R.id.notificationContentTitle, player.getNowPlaying().songName);
            notificationView.setTextViewText(R.id.notificationContentText, player.getNowPlaying().albumName);
            notificationView.setTextViewText(R.id.notificationSubText, player.getNowPlaying().artistName);

            // Update the info for the expanded view
            notificationViewExpanded.setTextViewText(R.id.notificationContentTitle, player.getNowPlaying().songName);
            notificationViewExpanded.setTextViewText(R.id.notificationContentText, player.getNowPlaying().albumName);
            notificationViewExpanded.setTextViewText(R.id.notificationSubText, player.getNowPlaying().artistName);
        }
        else{
            // If the player isn't playing music, set the notification text to a hardcoded set of strings
            notificationView.setTextViewText(R.id.notificationContentTitle, "Nothing is playing");
            notificationView.setTextViewText(R.id.notificationContentText, "");
            notificationView.setTextViewText(R.id.notificationSubText, "");

            notificationViewExpanded.setTextViewText(R.id.notificationContentTitle, "Nothing is playing");
            notificationViewExpanded.setTextViewText(R.id.notificationContentText, "");
            notificationViewExpanded.setTextViewText(R.id.notificationSubText, "");
        }

        // Set the button intents for the compact view
        notificationView.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_PREV), 0));
        notificationView.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_NEXT), 0));
        notificationView.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));
        notificationView.setOnClickPendingIntent(R.id.notificationStop, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_STOP), 0));

        // Set the button intents for the expanded view
        notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_PREV), 0));
        notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_NEXT), 0));
        notificationViewExpanded.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));
        notificationViewExpanded.setOnClickPendingIntent(R.id.notificationStop, PendingIntent.getBroadcast(this, 1, intent.setAction(ACTION_STOP), 0));

        // Update the play/pause button icon to reflect the player status
        if (!(player.isPlaying() || player.isPreparing())) {
            notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_play_arrow_48dp);
            notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_play_arrow_48dp);
        } else{
            notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause_48dp);
            notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause_48dp);
        }

        // Build the notification
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(
                        (player.isPlaying() || player.isPreparing())
                                ? R.drawable.ic_play_arrow_24dp
                                : R.drawable.ic_pause_24dp)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, LibraryActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        Notification notification = builder.build();

        // Manually set the expanded and compact views
        notification.contentView = notificationView;
        notification.bigContentView = notificationViewExpanded;

        startForeground(NOTIFICATION_ID, notification);
    }

    public void stop(){
        if (debug) Log.i(TAG, "stop() called");

        // If the UI process is still running, don't kill the process -- only remove its notification
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++){
            if(procInfos.get(i).processName.equals(BuildConfig.APPLICATION_ID)){
                player.pause();
                stopForeground(true);
                return;
            }
        }

        // If the UI process has already ended, kill the service and close the player
        finish();
    }

    public void finish() {
        if (debug) Log.i(TAG, "finish() called");
        if (!finished) {
            player.finish();
            player = null;
            stopForeground(true);
            instance = null;
            stopSelf();
            finished = true;
        }
    }

    public static class Listener extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null){
                if (debug) Log.i(TAG, "Intent received (action = null)");
                return;
            }

            if (debug) Log.i(TAG, "Intent received (action = \"" + intent.getAction() + "\")");

            if (instance == null){
                if (debug) Log.i(TAG, "Service not initialized");
                return;
            }

            if (instance.player.getNowPlaying() != null){
                try {
                    instance.player.saveState(intent.getAction());
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }

            switch (intent.getAction()){
                case (ACTION_TOGGLE_PLAY):
                    instance.player.togglePlay();
                    break;
                case (ACTION_BEGIN):
                    instance.player.begin();
                    break;
                case (ACTION_PLAY):
                    instance.player.play();
                    break;
                case (ACTION_PAUSE):
                    instance.player.pause();
                    break;
                case (ACTION_PREV):
                    instance.player.previous();
                    break;
                case (ACTION_NEXT):
                    instance.player.skip();
                    break;
                case (ACTION_STOP):
                    instance.stop();
                    break;
                case (ACTION_SEEK):
                    instance.player.seek(intent.getIntExtra(EXTRA_POSITION, instance.player.getCurrentPosition()));
                    instance.player.updateNowPlaying();
                    break;
                case (ACTION_CHANGE_SONG):
                    instance.player.changeSong(intent.getIntExtra(EXTRA_QUEUE_LIST_POSITION, instance.player.getQueuePosition()));
                    break;
                case (ACTION_SET_QUEUE):
                    if (!intent.hasExtra(EXTRA_QUEUE_LIST)){
                        if (debug) Log.w(TAG, "intent with action ACTION_SET_QUEUE was sent, but no EXTRA_QUEUE_LIST was provided");
                        break;
                    }
                    ArrayList<Song> queue = intent.getParcelableArrayListExtra(EXTRA_QUEUE_LIST);
                    instance.player.setQueue(queue, intent.getIntExtra(EXTRA_QUEUE_LIST_POSITION, 0));
                    break;
                case (ACTION_EDIT_QUEUE):
                    if (!intent.hasExtra(EXTRA_QUEUE_LIST)){
                        if (debug) Log.w(TAG, "intent with action ACTION_EDIT_QUEUE was sent, but no EXTRA_QUEUE_LIST was provided");
                        break;
                    }
                    ArrayList<Song> editedQueue = intent.getParcelableArrayListExtra(EXTRA_QUEUE_LIST);
                    instance.player.editQueue(editedQueue, intent.getIntExtra(EXTRA_QUEUE_LIST_POSITION, 0));
                    break;
                case (ACTION_QUEUE):
                    if (intent.hasExtra(EXTRA_QUEUE_SONG)){
                        if (intent.getBooleanExtra(EXTRA_QUEUE_NEXT, false))
                            instance.player.queueNext((Song) intent.getParcelableExtra(EXTRA_QUEUE_SONG));
                        else
                            instance.player.queueLast((Song) intent.getParcelableExtra(EXTRA_QUEUE_SONG));
                    }
                    else if (intent.hasExtra(EXTRA_QUEUE_LIST)){
                        ArrayList<Song> songList = intent.getParcelableArrayListExtra(EXTRA_QUEUE_LIST);
                        if (intent.getBooleanExtra(EXTRA_QUEUE_NEXT, false))
                            instance.player.queueNext(songList);
                        else
                            instance.player.queueLast(songList);
                    }
                    else{
                        if (debug) Log.w(TAG, "intent with action ACTION_QUEUE was sent, but no EXTRA_QUEUE_LIST or EXTRA_QUEUE_SONG was provided");
                    }
                    break;
                case (ACTION_SET_PREFS):
                    instance.player.setPrefs(
                            intent.getBooleanExtra(EXTRA_PREF_SHUFFLE, instance.player.isShuffle()),
                            intent.getShortExtra(EXTRA_PREF_REPEAT, instance.player.getRepeat()));
                    break;
                case (ACTION_REQUEST_SYNC):
                    instance.player.updateNowPlaying();
                    break;
            }
        }
    }
}

