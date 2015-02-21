package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.MediaReceiver;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class PlayerService extends Service {

    // Constants
    public static final int NOTIFICATION_ID = 1;
    private static final String ACTION_TOGGLE_PLAY = "toggle";
    private static final String ACTION_PREV = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_STOP = "stop";
    private static final String TAG = "PlayerService";
    private static PlayerService instance;
    private static MediaReceiver mediaReceiver = new MediaReceiver();
    private static Context context;
    private static Player player;
    private NotificationManager notificationManager;

    public PlayerService() {
        if (instance == null || this.equals(instance)) {
            instance = this;

            notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

            if (player == null) {
                player = new Player(this);
            }

            IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

            context.registerReceiver(mediaReceiver, filter);
        } else {
            Debug.log(Debug.WARNING, TAG, "Attempted to create a second player service", getApplicationContext());
            stopSelf();
        }
    }

    @TargetApi(16)
    public static Notification getNotificationCompat() {
        Notification notification;

        if (getNowPlaying() != null) {
            Intent intent = new Intent(context, PlayerService.class);

            RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.notification);
            notificationView.setTextViewText(R.id.notificationContentTitle, getNowPlaying().songName);
            notificationView.setTextViewText(R.id.notificationContentText, getNowPlaying().albumName);
            notificationView.setTextViewText(R.id.notificationSubText, getNowPlaying().artistName);
            notificationView.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));
            if (getArt() != null) {
                notificationView.setImageViewBitmap(R.id.notificationIcon, getArt());
            } else {
                notificationView.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
            }

            RemoteViews notificationViewExpanded = new RemoteViews(context.getPackageName(), R.layout.notification_expanded);
            notificationViewExpanded.setTextViewText(R.id.notificationContentTitle, getNowPlaying().songName);
            notificationViewExpanded.setTextViewText(R.id.notificationContentText, getNowPlaying().albumName);
            notificationViewExpanded.setTextViewText(R.id.notificationSubText, getNowPlaying().artistName);
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0));
            if (getArt() != null) {
                notificationViewExpanded.setImageViewBitmap(R.id.notificationIcon, getArt());
            } else {
                notificationViewExpanded.setImageViewResource(R.id.notificationIcon, R.drawable.art_default);
            }

            if (isPlaying()) {
                notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause);
                notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_pause);

                notification = new Notification.Builder(context)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.ic_play_notification)
                        .setWhen(System.currentTimeMillis())
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, NowPlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                        .build();
            } else {
                notificationView.setImageViewResource(R.id.notificationPause, R.drawable.ic_play);
                notificationViewExpanded.setImageViewResource(R.id.notificationPause, R.drawable.ic_play);

                notification = new Notification.Builder(context)
                        .setOngoing(false)
                        .setDeleteIntent(PendingIntent.getService(context, 1, intent.setAction(ACTION_STOP), 0))
                                //.addAction(R.drawable.ic_play, context.getResources().getString(R.string.action_play), PendingIntent.getService(context, 1, intent.setAction(ACTION_PAUSE), 0))
                        .setSmallIcon(R.drawable.ic_pause_notification)
                        .setOnlyAlertOnce(true)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, NowPlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                        .build();
            }

            notification.contentView = notificationView;
            notification.bigContentView = notificationViewExpanded;

        } else {
            notification = new Notification.Builder(context)
                    .setContentTitle("Nothing is playing")
                    .setContentText("Jockey is currently running")
                    .setSubText("To exit Jockey, remove it from your recent apps")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LibraryActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .setSmallIcon(R.drawable.ic_pause_notification)
                    .setLargeIcon(Themes.getIcon(context))
                    .build();
        }

        return notification;
    }

    @TargetApi(21)
    public static Notification getNotification() {
        Notification.Builder notification = new Notification.Builder(context);
        if (getNowPlaying() != null) {
            Intent intent = new Intent(context, PlayerService.class);
            notification
                    .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(1, 2))
                    .setColor(context.getResources().getColor(R.color.player_control_background))
                    .setContentTitle(getNowPlaying().songName)
                    .setContentText(getNowPlaying().albumName)
                    .setSubText(getNowPlaying().artistName)
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, NowPlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .addAction(R.drawable.ic_vector_skip_previous_notification, context.getResources().getString(R.string.action_previous), PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            if (isPlaying()) {
                notification
                        .addAction(R.drawable.ic_vector_pause, context.getResources().getString(R.string.action_pause), PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0))
                        .setSmallIcon(R.drawable.ic_vector_play_notification);
            } else {
                notification
                        .setDeleteIntent(PendingIntent.getService(context, 1, intent.setAction(ACTION_STOP), 0))
                        .addAction(R.drawable.ic_vector_play, context.getResources().getString(R.string.action_play), PendingIntent.getService(context, 1, intent.setAction(ACTION_TOGGLE_PLAY), 0))
                        .setSmallIcon(R.drawable.ic_vector_pause_notification);
            }
            notification.addAction(R.drawable.ic_vector_skip_next_notification, context.getResources().getString(R.string.action_skip), PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            if (getArt() == null) {
                notification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.art_default));
            } else {
                notification.setLargeIcon(getArt());
            }
        } else {
            notification
                    .setContentTitle("Nothing is playing")
                    .setContentText("Jockey is currently running")
                    .setSubText("To exit Jockey, remove it from your recent apps")
                    .setColor(Themes.getPrimary())
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LibraryActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .setLargeIcon(Themes.getIcon(context))
                    .setSmallIcon(R.drawable.ic_vector_pause_notification);
        }
        return notification.build();
    }

    public static Player getPlayer(Context mContext) {
        if (!isInitialized()) {
            context = mContext;
            player = new Player(mContext);
            context.startService(new Intent(context, PlayerService.class));
        }
        return player;
    }

    public static PlayerService getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return !(player == null);
    }

    // Make these methods easier to call
    // All they do is pass them directly to the player instance
    public static void setQueue(Context context, ArrayList<Song> newQueue, int newPosition) {
        getPlayer(context).setQueue(newQueue, newPosition);
    }

    public static void begin(Context context) {
        getPlayer(context).begin();
    }

    public static Song getNowPlaying() {
        return player.getNowPlaying();
    }

    public static void togglePlay() {
        player.togglePlay();
    }

    public static void play() {
        player.play();
    }

    public static void pause() {
        player.pause();
    }

    public static void stop() {
        player.stop();
    }

    //public static PlayerService getPlayer(){return instance;}

    public static void previous() {
        player.previous();
    }

    public static void skip() {
        player.skip();
    }

    public static void seek(int position) {
        player.seek(position);
    }

    public static void changeSong(int newPosition) {
        player.changeSong(newPosition);
    }

    public static void queueNext(Context context, Song song) {
        getPlayer(context).queueNext(song);
    }

    public static void queueLast(Context context, Song song) {
        getPlayer(context).queueLast(song);
    }

    public static void queueNext(Context context, ArrayList<Song> songs) {
        getPlayer(context).queueNext(songs);
    }

    public static void queueLast(Context context, ArrayList<Song> songs) {
        getPlayer(context).queueLast(songs);
    }

    public static void toggleShuffle() {
        player.toggleShuffle();
    }

    public static void toggleRepeat() {
        player.toggleRepeat();
    }

    public static Bitmap getArt() {
        return player.getArt();
    }

    public static boolean isPlaying() {
        return player.isPlaying();
    }

    public static int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    public static boolean isShuffle() {
        return player.isShuffle();
    }

    public static boolean isRepeat() {
        return player.isRepeat();
    }

    public static boolean isRepeatOne() {
        return player.isRepeatOne();
    }

    public static ArrayList<Song> getQueue() {
        return player.getQueue();
    }

    public static int getPosition() {
        return player.getPosition();
    }

    @Override
    public void onCreate() {
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
            player.stop();
            notificationManager.cancel(NOTIFICATION_ID);
            try {
                unregisterReceiver(mediaReceiver);
            } catch (Exception e) {
                Log.d("mediaReceiver", "Unable to unregister mediaReceiver", e);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        onDestroy();
    }

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

    public void notifyNowPlaying() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationManager.notify(NOTIFICATION_ID, getNotification());
        } else {
            notificationManager.notify(NOTIFICATION_ID, getNotificationCompat());
        }
    }

    public Player getPlayer() {
        return player;
    }
}

