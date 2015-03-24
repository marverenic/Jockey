package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class PlayerService extends Service {

    // Constants
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_TOGGLE_PLAY = "toggle";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PREV = "previous";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_STOP = "stop";
    private static final String TAG = "PlayerService";

    // Global instance values
    private static PlayerService instance; // The service instance in use
    private static Player player; // The player for the service
    private static Context context;
    private NotificationManager notificationManager;

    public PlayerService() {
        // Only make a new service if there isn't one already
        if (instance == null || this.equals(instance)) {
            instance = this;

            if (player == null) {
                player = new Player(this);
            }
        }
        else {
            // If the service has already started, kill this instance.
            Debug.log(Debug.LogLevel.WARNING, TAG, "Attempted to create a second player service", getApplicationContext());
            stopSelf();
        }
    }

    @Override
    public void onCreate() {
        context = getApplicationContext();
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // When the service is started, move it to the foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startForeground(NOTIFICATION_ID, getNotification());
        } else {
            startForeground(NOTIFICATION_ID, getNotificationCompat());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.equals(instance)) {
            LibraryScanner.saveLibrary(context);
            player.finish();
            player = null;
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    // Stop the service when Jockey is removed from the recent apps overview
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    // Listens for commands from the notification
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_NEXT:
                    player.skip();
                    break;
                case ACTION_TOGGLE_PLAY:
                    player.togglePlay();
                    break;
                case ACTION_PREV:
                    player.previous();
                    break;
                case ACTION_STOP:
                    player.stop();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    // Generate a notification, choosing the proper API level notification builder
    public void notifyNowPlaying() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationManager.notify(NOTIFICATION_ID, getNotification());
        } else {
            notificationManager.notify(NOTIFICATION_ID, getNotificationCompat());
        }
    }

    // Build a notification on API <21 devices
    @TargetApi(16)
    public static Notification getNotificationCompat() {
        // Creates a notification on pre-Lollipop devices
        // The notification that will be returned
        Notification notification;

        // The intent for the button actions
        Intent intent = new Intent(context, PlayerService.class);

        // Create the compact view
        RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.notification);
        // Create the expanded view
        RemoteViews notificationViewExpanded = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);

        // Set the artwork for the notification
        if (getArt() != null) {
            notificationView.setImageViewBitmap(R.id.notificationIcon, getArt());
            notificationViewExpanded.setImageViewBitmap(R.id.notificationIcon, getArt());
        }
        else {
            notificationView.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
            notificationViewExpanded.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
        }

        // If the player is playing music, set the track info and the button intents
        if (getNowPlaying() != null) {
            // Update the info for the compact view
            notificationView.setTextViewText(R.id.notificationContentTitle, getNowPlaying().songName);
            notificationView.setTextViewText(R.id.notificationContentText, getNowPlaying().albumName);
            notificationView.setTextViewText(R.id.notificationSubText, getNowPlaying().artistName);

            // Update the info for the expanded view
            notificationViewExpanded.setTextViewText(R.id.notificationContentTitle, getNowPlaying().songName);
            notificationViewExpanded.setTextViewText(R.id.notificationContentText, getNowPlaying().albumName);
            notificationViewExpanded.setTextViewText(R.id.notificationSubText, getNowPlaying().artistName);

            // Set the button intents for the compact view
            notificationView.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));

            // Set the button intents for the expanded view
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));

        }
        else{
            // If the player isn't playing music, set the notification text to a hardcoded set of strings
            notificationView.setTextViewText(R.id.notificationContentTitle, "Nothing is playing");
            notificationView.setTextViewText(R.id.notificationContentText, "To exit Jockey, remove it from your recent apps");
            notificationView.setTextViewText(R.id.notificationSubText, "");

            notificationViewExpanded.setTextViewText(R.id.notificationContentTitle, "Nothing is playing");
            notificationViewExpanded.setTextViewText(R.id.notificationContentText, "To exit Jockey, remove it from your recent apps");
            notificationViewExpanded.setTextViewText(R.id.notificationSubText, "");
        }

        // Update the play/pause button icon to reflect the player status
        if (!(PlayerService.isPlaying() || PlayerService.isPreparing())) {
            notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_play);
            notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_play);
        } else{
            notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause);
            notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause);
        }

        // Build the notification
        notification = new Notification.Builder(context)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_play_notification)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(PendingIntent.getActivity(
                        getInstance(),
                        0,
                        new Intent(context, LibraryActivity.class).putExtra(LibraryActivity.START_NOW_PLAYING, true),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        // Manually set the expanded and compact views
        notification.contentView = notificationView;
        notification.bigContentView = notificationViewExpanded;

        return notification;
    }

    // Build a notification on API >= 21 devices
    @TargetApi(21)
    public static Notification getNotification() {
        // Builds a notification on Lollipop and higher devices

        // Get a notification builder
        Notification.Builder notification = new Notification.Builder(context);

        // The intent for buttons
        Intent intent = new Intent(getInstance(), PlayerService.class);

        notification
                .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(1, 2))
                .setColor(context.getResources().getColor(R.color.player_control_background))
                .setShowWhen(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(PendingIntent.getActivity(
                        getInstance(),
                        0,
                        new Intent(context, LibraryActivity.class).putExtra(LibraryActivity.START_NOW_PLAYING, true),
                        PendingIntent.FLAG_UPDATE_CURRENT));

        // Set the album artwork
        if (getArt() == null) {
            notification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.art_default));
        } else {
            notification.setLargeIcon(getArt());
        }

        // Add the media control buttons
        // Always add the previous button first
        notification.addAction(R.drawable.ic_vector_skip_previous_notification, context.getResources().getString(R.string.action_previous), PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
        // Add the play/pause button next
        // Also set the notification's icon to reflect the player's status
        if (PlayerService.isPlaying() || PlayerService.isPreparing()) {
            notification
                    .addAction(R.drawable.ic_vector_pause, context.getResources().getString(R.string.action_pause), PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0))
                    .setSmallIcon(R.drawable.ic_vector_play_notification);
        } else {
            notification
                    .setDeleteIntent(PendingIntent.getService(context, 1, intent.setAction(ACTION_STOP), 0))
                    .addAction(R.drawable.ic_vector_play, context.getResources().getString(R.string.action_play), PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0))
                    .setSmallIcon(R.drawable.ic_vector_pause_notification);
        }
        // Add the skip button last
        notification.addAction(R.drawable.ic_vector_skip_next_notification, context.getResources().getString(R.string.action_skip), PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));

        // Update the now playing information
        if (getNowPlaying() != null) {
            notification
                    .setContentTitle(getNowPlaying().songName)
                    .setContentText(getNowPlaying().albumName)
                    .setSubText(getNowPlaying().artistName);
        }
        else{
            // If nothing is playing, set the text to a hardcoded string instead
            notification
                    .setContentTitle("Nothing is playing")
                    .setContentText("To exit Jockey, remove it from your recent apps")
                    .setSubText("");
        }
        return notification.build();
    }

    // Get the active instance of the player service
    public static PlayerService getInstance() {
        return instance;
    }

    // A simple check if the service has started yet
    public static boolean isInitialized() {
        return !(player == null);
    }

    // Get the player instance for the service
    // Don't worry about creating it, since it should have been started already
    public Player getPlayer() { return player; }

    // Get the player instance for the service
    // The service may not have been started already, so start it if necessary
    public static Player getPlayer(Context mContext) {
        if (!isInitialized()) {
            context = mContext;
            player = new Player(mContext);
            context.startService(new Intent(context, PlayerService.class));
        }
        return player;
    }

    // Make these methods easier to call
    // They will pass the commands to the Player, creating the service if necessary
    // These methods CAN (and should) be used to initialize the service
    public static void setQueue(Context context, ArrayList<Song> newQueue, int newPosition) { getPlayer(context).setQueue(newQueue, newPosition); }
    public static void changeQueue(Context context, ArrayList<Song> newQueue, int newPosition) { getPlayer(context).changeQueue(newQueue, newPosition); }
    public static void queueNext(Context context, Song song) { getPlayer(context).queueNext(song); }
    public static void queueLast(Context context, Song song) { getPlayer(context).queueLast(song); }
    public static void queueNext(Context context, ArrayList<Song> songs) {getPlayer(context).queueNext(songs); }
    public static void queueLast(Context context, ArrayList<Song> songs) { getPlayer(context).queueLast(songs); }

    // Same as above, but these should be "safe" (the service should already be started)
    // Calling these methods before creating the service will cause problems and make you sad.
    public static void begin() { player.begin(); }
    public static Song getNowPlaying() { return player.getNowPlaying(); }
    public static void togglePlay() { player.togglePlay(); }
    public static void play() { player.play(); }
    public static void pause() { player.pause(); }
    public static void stop() { player.stop(); }
    public static void previous() { player.previous(); }
    public static void skip() { player.skip(); }
    public static void seek(int position) { player.seek(position); }
    public static void changeSong(int newPosition) { player.changeSong(newPosition); }
    public static void toggleShuffle() { player.toggleShuffle(); }
    public static void toggleRepeat() { player.toggleRepeat(); }
    public static Bitmap getArt() { return player.getArt(); }
    public static Bitmap getFullArt() { return player.getFullArt(); }
    public static boolean isPlaying() { return player.isPlaying(); }
    public static boolean isPreparing() {
        return player.isPreparing();
    }
    public static int getCurrentPosition() { return player.getCurrentPosition(); }
    public static boolean isShuffle() {return player.isShuffle();}
    public static boolean isRepeat() { return player.isRepeat();}
    public static boolean isRepeatOne() { return player.isRepeatOne(); }
    public static ArrayList<Song> getQueue() { return player.getQueue(); }
    public static int getPosition() { return player.getPosition(); }
}

