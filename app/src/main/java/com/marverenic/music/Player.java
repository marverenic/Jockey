package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.ManagedMediaPlayer;
import com.marverenic.music.utils.MediaReceiver;
import com.marverenic.music.utils.Themes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@SuppressWarnings("deprecation")
public class Player extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    // Constants
    public static final int NOTIFICATION_ID = 1;
    public static final String UPDATE_BROADCAST = "marverenic.jockey.player.REFRESH";
    private static final String SESSION_TAG = "PLAYER";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_PREV = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_STOP = "stop";
    private static final String TAG = "Player";
    private static Player instance;
    private static boolean shuffle;
    private static boolean repeat;
    private static boolean repeatOne;
    private static boolean active = false;
    private static Bitmap art;
    private static ArrayList<Song> queue;
    private static int position;
    private static ArrayList<Song> queueShuffled = new ArrayList<>();
    private static int positionShuffled;
    private static MediaReceiver mediaReceiver = new MediaReceiver();
    private static Context context;
    private static NotificationManager notificationManager;
    private static MediaSession mediaSession;
    private static RemoteControlClient remoteControlClient;
    private ManagedMediaPlayer mediaPlayer;

    //
    //                  INSTANCE METHODS
    //

    public Player() {
        super();
        mediaPlayer = new ManagedMediaPlayer(context);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        queue = new ArrayList<>();
        position = 0;
    }

    public static void setQueue(final ArrayList<Song> newQueue, final int newPosition) {
        queue = newQueue;
        position = newPosition;
        if (shuffle) {
            instance.shuffleQueue();
        }
        Debug.log(Debug.VERBOSE, TAG, "The queue has been updated", context);
    }

    // Start playing a new song
    public static void begin() {
        Debug.log(Debug.VERBOSE, TAG, "Begin method has been called", context);

        if (instance.getFocus()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                instance.updateMediaSession();
            }
            instance.mediaPlayer.stop();
            instance.mediaPlayer.reset();
            try {
                Debug.log(Debug.VERBOSE, TAG, "Setting data source for song " + getNowPlaying(), context);
                instance.mediaPlayer.setDataSource((getNowPlaying()).location);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
                Toast.makeText(context, "There was an error playing this song", Toast.LENGTH_SHORT).show();
                Debug.log(Debug.VERBOSE, TAG, "There was an error setting the data source", context);
                return;
            }
            //Toast.makeText(context, "Now playing " + getNowPlaying().songName + " by " + getNowPlaying().artistName, Toast.LENGTH_SHORT).show();
            //}
            instance.mediaPlayer.prepareAsync();
        }
        Debug.log(Debug.VERBOSE, TAG, "Begin method has finished", context);
    }

    //
    //                  MEDIA PLAYER METHOD OVERRIDES
    //

    // Return the currently playing song (May be null)
    public static Song getNowPlaying() {
        Debug.log(Debug.VERBOSE, TAG, "The current track has been requested", context);
        if (shuffle) {
            if (positionShuffled >= queueShuffled.size() || positionShuffled < 0 || queueShuffled.size() == 0) {
                return null;
            }
            return queueShuffled.get(positionShuffled);
        }
        if (position >= queue.size() || position < 0 || queue.size() == 0) {
            return null;
        }
        return queue.get(position);
    }

    public static Player getInstance() {
        return instance;
    }

    public static boolean isShuffle() {
        return shuffle;
    }

    public static boolean isRepeat() {
        return repeat;
    }

    public static boolean isRepeatOne() {
        return repeatOne;
    }

    public static boolean initialized() {
        return !(instance == null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        if (instance == null) {
            instance = new Player();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startForeground(NOTIFICATION_ID, getNotification());
        } else {
            startForeground(NOTIFICATION_ID, getNotificationCompat());
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(mediaReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // If Lollipop, use a MediaReciever
            mediaSession = new MediaSession(context, SESSION_TAG);
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public void onPlay() {
                    if (mediaPlayer.getState() != ManagedMediaPlayer.STATE_STARTED) {
                        Player.instance.pause();
                    }
                }

                @Override
                public void onSkipToQueueItem(long id) {
                    Player.instance.changeSong((int) id);
                }

                @Override
                public void onPause() {
                    if (mediaPlayer.getState() != ManagedMediaPlayer.STATE_PAUSED) {
                        Player.instance.pause();
                    }
                }

                @Override
                public void onSkipToNext() {
                    Player.instance.skip();
                }

                @Override
                public void onSkipToPrevious() {
                    Player.instance.previous();
                }

                @Override
                public void onStop() {
                    Player.instance.stop();
                }

                @Override
                public void onSeekTo(long pos) {
                    Player.instance.seek((int) pos);
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // For KitKat, use a RemoteController

            getFocus();

            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(new ComponentName(this.getPackageName(), this.getClass().toString()));
            remoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).registerRemoteControlClient(remoteControlClient);

            // Flags for the media transport control that this client supports.
            int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                    | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                    | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | RemoteControlClient.FLAG_KEY_MEDIA_STOP;

            flags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;

            remoteControlClient.setOnGetPlaybackPositionListener(
                    new RemoteControlClient.OnGetPlaybackPositionListener() {
                        @Override
                        public long onGetPlaybackPosition() {
                            return instance.getCurrentPosition();
                        }
                    });
            remoteControlClient.setPlaybackPositionUpdateListener(
                    new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                        @Override
                        public void onPlaybackPositionUpdate(long newPositionMs) {
                            seek((int) newPositionMs);
                        }
                    });


            remoteControlClient.setTransportControlFlags(flags);
        }

        shuffle = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefShuffle", false);
        repeat = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefRepeat", false);
        repeatOne = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("prefRepeatOne", false);

        if (repeat && repeatOne) {
            repeat = false;
            repeatOne = false;

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("prefRepeat", false);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("prefRepeatOne", false);
        }

        Debug.log(Debug.VERBOSE, TAG, "The player service has started", context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //mediaPlayer.stop();
        //mediaPlayer.release();
        //notificationManager.cancel(NOTIFICATION_ID);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        notificationManager.cancel(NOTIFICATION_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.release();
        }
        try {
            unregisterReceiver(mediaReceiver);
        } catch (Exception e) {
            Log.d("mediaReceiver", "Unable to unregister mediaReceiver", e);
        }
        Debug.log(Debug.VERBOSE, TAG, "The player service has been destroyed", context);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Debug.log(Debug.VERBOSE, TAG, "On Task Removed has been called", context);
        if (!isPlaying()) {
            stopSelf();
            onDestroy();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Debug.log(Debug.VERBOSE, TAG, "Responded to a command", context);
            switch (intent.getAction()) {
                case ACTION_NEXT:
                    Player.instance.skip();
                    break;
                case ACTION_PAUSE:
                    Player.instance.pause();
                    break;
                case ACTION_PREV:
                    Player.instance.previous();
                    break;
                case ACTION_STOP:
                    Player.instance.stop();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        skip();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Debug.log(Debug.VERBOSE, TAG, "On Prepared called", context);
        mp.start();

        art = Fetch.fetchAlbumArtLocal(context, getNowPlaying().albumId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, (long) Player.instance.mediaPlayer.getCurrentPosition(), 1f).build());
        }

        notifyNowPlaying();
        context.sendBroadcast(new Intent(UPDATE_BROADCAST));
        updateMediaSession();
        Debug.log(Debug.VERBOSE, TAG, "On Prepared finished", context);
    }

    //
    //              MEDIA CONTROL METHODS
    //

    @Override
    public void onAudioFocusChange(int focusChange) {
        Debug.log(Debug.VERBOSE, TAG, "On Audio Focus Change called", context);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Debug.log(Debug.VERBOSE, TAG, "Audiofocus_Loss_Transient: Pausing music", context);
                if (Player.instance.isPlaying()) {
                    Player.instance.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Debug.log(Debug.VERBOSE, TAG, "Audiofocus_Loss: Stopping player", context);
                Player.instance.stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Debug.log(Debug.VERBOSE, TAG, "On Audiofocus_Loss_Transient_Can_Duck: Lowering volume", context);
                Player.instance.mediaPlayer.setVolume(0.5f, 0.5f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Debug.log(Debug.VERBOSE, TAG, "On Audiofocus_Gained: Resetting volume", context);
                Player.instance.mediaPlayer.setVolume(1f, 1f);
                break;
            default:
                break;
        }
        Debug.log(Debug.VERBOSE, TAG, "On Audio Focus Change finished", context);
    }

    public void notifyNowPlaying() {
        Debug.log(Debug.VERBOSE, TAG, "Generating notification...", context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationManager.notify(NOTIFICATION_ID, getNotification());
        } else {
            notificationManager.notify(NOTIFICATION_ID, getNotificationCompat());
        }
    }

    @TargetApi(16)
    public Notification getNotificationCompat() {
        Notification notification;

        if (getNowPlaying() != null) {
            Intent intent = new Intent(context, Player.class);

            RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.notification);
            notificationView.setTextViewText(R.id.notificationContentTitle, getNowPlaying().songName);
            notificationView.setTextViewText(R.id.notificationContentText, getNowPlaying().albumName);
            notificationView.setTextViewText(R.id.notificationSubText, getNowPlaying().artistName);
            notificationView.setOnClickPendingIntent(R.id.notificationSkipPrevious, PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationSkipNext, PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            notificationView.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_PAUSE), 0));
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
            notificationViewExpanded.setOnClickPendingIntent(R.id.notificationPause, PendingIntent.getService(context, 1, intent.setAction(ACTION_PAUSE), 0));
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
                        .setWhen(System.currentTimeMillis())
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
                    .setWhen(System.currentTimeMillis())
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LibraryActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .setSmallIcon(R.drawable.ic_pause_notification)
                    .setLargeIcon(Themes.getIcon(context))
                    .build();
        }

        Debug.log(Debug.VERBOSE, TAG, "Notification built (API < 21)", context);
        return notification;
    }

    @TargetApi(21)
    public Notification getNotification() {
        Notification.Builder notification = new Notification.Builder(context);
        if (getNowPlaying() != null) {
            Intent intent = new Intent(context, Player.class);
            notification
                    .setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(1, 2))
                    .setColor(context.getResources().getColor(R.color.player_control_background))
                    .setContentTitle(getNowPlaying().songName)
                    .setContentText(getNowPlaying().albumName)
                    .setSubText(getNowPlaying().artistName)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, NowPlayingActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .addAction(R.drawable.ic_vector_skip_previous_notification, context.getResources().getString(R.string.action_previous), PendingIntent.getService(context, 1, intent.setAction(ACTION_PREV), 0));
            if (isPlaying()) {
                notification
                        .addAction(R.drawable.ic_vector_pause, context.getResources().getString(R.string.action_pause), PendingIntent.getService(context, 1, intent.setAction(ACTION_PAUSE), 0))
                        .setSmallIcon(R.drawable.ic_vector_play_notification);
            } else {
                notification
                        .setDeleteIntent(PendingIntent.getService(context, 1, intent.setAction(ACTION_STOP), 0))
                        .addAction(R.drawable.ic_vector_play, context.getResources().getString(R.string.action_play), PendingIntent.getService(context, 1, intent.setAction(ACTION_PAUSE), 0))
                        .setSmallIcon(R.drawable.ic_vector_pause_notification);
            }
            notification.addAction(R.drawable.ic_vector_skip_next_notification, context.getResources().getString(R.string.action_skip), PendingIntent.getService(context, 1, intent.setAction(ACTION_NEXT), 0));
            if (art == null) {
                notification.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.art_default));
            } else {
                notification.setLargeIcon(art);
            }
        } else {
            notification
                    .setContentTitle("Nothing is playing")
                    .setContentText("Jockey is currently running")
                    .setSubText("To exit Jockey, remove it from your recent apps")
                    .setColor(Themes.getPrimary())
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LibraryActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), PendingIntent.FLAG_CANCEL_CURRENT))
                    .setLargeIcon(Themes.getIcon(context))
                    .setSmallIcon(R.drawable.ic_vector_pause_notification);
        }
        Debug.log(Debug.VERBOSE, TAG, "Notification built (API >= 21)", context);
        return notification.build();
    }

    public void updateMediaSession() {
        Debug.log(Debug.VERBOSE, TAG, "Updating media session", context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getNowPlaying() != null) {
                MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
                metadataBuilder
                        .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, getNowPlaying().songName)
                        .putString(MediaMetadata.METADATA_KEY_TITLE, getNowPlaying().songName)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, getNowPlaying().albumName)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, getNowPlaying().artistName);
                mediaSession.setMetadata(metadataBuilder.build());

                PlaybackState.Builder state = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                        PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS);

                switch (mediaPlayer.getState()) {
                    case ManagedMediaPlayer.STATE_STARTED:
                        state.setState(PlaybackState.STATE_PLAYING, getPosition(), 1f);
                        break;
                    case ManagedMediaPlayer.STATE_PAUSED:
                        state.setState(PlaybackState.STATE_PAUSED, getPosition(), 1f);
                        break;
                    case ManagedMediaPlayer.STATE_STOPPED:
                        state.setState(PlaybackState.STATE_STOPPED, getPosition(), 1f);
                        break;
                    default:
                        state.setState(PlaybackState.STATE_NONE, getPosition(), 1f);
                }
                mediaSession.setPlaybackState(state.build());
                mediaSession.setActive(true);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP);

            // Update the remote controls
            remoteControlClient.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getNowPlaying().artistName)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getNowPlaying().albumName)
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getNowPlaying().songName)
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, getNowPlaying().songDuration)
                    .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, getArt())
                    .apply();
        }
        Debug.log(Debug.VERBOSE, TAG, "Media session updated", context);
    }

    public void pause() {
        Debug.log(Debug.VERBOSE, TAG, "Pause method has been called", context);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Debug.log(Debug.VERBOSE, TAG, "Player has been paused", context);
        } else {
            if (getFocus()) {
                if (shuffle) {
                    if (positionShuffled + 1 == queueShuffled.size() && mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() < 100) {
                        positionShuffled = 0;
                        begin();
                        Debug.log(Debug.VERBOSE, TAG, "Restarting queue (shuffled)...", context);
                    } else {
                        mediaPlayer.start();
                        Debug.log(Debug.VERBOSE, TAG, "Playback resumed", context);
                    }
                } else {
                    if (position + 1 == queue.size() && mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition() < 100) {
                        position = 0;
                        begin();
                        Debug.log(Debug.VERBOSE, TAG, "Restarting queue (unshuffled)...", context);
                    } else {
                        mediaPlayer.start();
                        Debug.log(Debug.VERBOSE, TAG, "Playback resumed", context);
                    }
                }
            }
        }

        notifyNowPlaying();
        context.sendBroadcast(new Intent(UPDATE_BROADCAST));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
        Debug.log(Debug.VERBOSE, TAG, "Pause method has finished", context);
    }

    public void stop() {
        Debug.log(Debug.VERBOSE, TAG, "Stop method called", context);
        if (isPlaying()) {
            pause();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
        ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocus(this);
        active = false;
        Debug.log(Debug.VERBOSE, TAG, "Stop method finished", context);
    }

    //
    //              QUEUE METHODS
    //

    public boolean getFocus() {
        Debug.log(Debug.VERBOSE, TAG, "Attempting to get audio focus...", context);
        if (!active) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            active = (audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        return active;
    }

    public void previous() {
        Debug.log(Debug.VERBOSE, TAG, "Previous method called", context);
        if (shuffle) {
            if (mediaPlayer.getCurrentPosition() > 5000 || positionShuffled < 1) {
                mediaPlayer.seekTo(0);
                Debug.log(Debug.VERBOSE, TAG, "Track has been restarted", context);
            } else {
                positionShuffled--;
                begin();
                Debug.log(Debug.VERBOSE, TAG, "Returned to previous track", context);
            }
        } else {
            if (mediaPlayer.getCurrentPosition() > 5000 || position < 1) {
                mediaPlayer.seekTo(0);
                Debug.log(Debug.VERBOSE, TAG, "The track has been restarted", context);
            } else {
                position--;
                begin();
                Debug.log(Debug.VERBOSE, TAG, "Returned to previous track", context);
            }
        }
        context.sendBroadcast(new Intent(UPDATE_BROADCAST));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
    }

    public void skip() {
        Debug.log(Debug.VERBOSE, TAG, "Skip method called", context);
        Debug.log(Debug.DEBUG, TAG, "The track " + getNowPlaying() + " is currently at " + mediaPlayer.getCurrentPosition() + "ms out of " + mediaPlayer.getDuration() + "ms", context);
        if (shuffle) {
            if (positionShuffled + 1 < queueShuffled.size()) {
                positionShuffled++;
                begin();
                Debug.log(Debug.VERBOSE, TAG, "Began next song (shuffled)", context);
            } else {
                if (repeat) {
                    positionShuffled = 0;
                    begin();
                    Debug.log(Debug.VERBOSE, TAG, "Reset queue (shuffled)", context);
                } else {
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(mediaPlayer.getDuration());
                    Debug.log(Debug.VERBOSE, TAG, "No more songs left in the queue (shuffled)", context);
                }
            }
        } else {
            if (position + 1 < queue.size()) {
                position++;
                begin();
                Debug.log(Debug.VERBOSE, TAG, "Began next song (unshuffled)", context);
            } else {
                if (repeat) {
                    position = 0;
                    begin();
                    Debug.log(Debug.VERBOSE, TAG, "Reset queue (unshuffled)", context);
                } else {
                    mediaPlayer.pause();
                    mediaPlayer.seekTo(mediaPlayer.getDuration());
                    Debug.log(Debug.VERBOSE, TAG, "No more songs left in the queue (shuffled)", context);
                }

            }
        }
        context.sendBroadcast(new Intent(UPDATE_BROADCAST));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
    }

    public void seek(int position) {
        Debug.log(Debug.VERBOSE, TAG, "Seeking to " + position + "ms out of " + mediaPlayer.getDuration() + "ms", context);
        if (position <= mediaPlayer.getDuration() && getNowPlaying() != null) {
            mediaPlayer.seekTo(position);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isPlaying()) {
                    mediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, (long) Player.instance.mediaPlayer.getCurrentPosition(), 1f).build());
                } else {
                    mediaSession.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, (long) Player.instance.mediaPlayer.getCurrentPosition(), 1f).build());
                }
            }
        }
    }

    //
    //              PLAYER OPTION METHODS
    //

    public void changeSong(int newPosition) {
        Debug.log(Debug.VERBOSE, TAG, "Changing song to position " + position + " in the queue", context);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateMediaSession();
        }
    }

    public void queueNext(final Song song) {
        Debug.log(Debug.VERBOSE, TAG, "Queueing " + song + " next...", context);
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
        Debug.log(Debug.VERBOSE, TAG, "Queueing " + song + " last...", context);
        if (queue.size() != 0) {
            if (shuffle) {
                queueShuffled.add(queueShuffled.size(), song);
                queue.add(queueShuffled.size(), song);
            } else {
                queue.add(queue.size(), song);
            }
        } else {
            ArrayList<Song> newQueue = new ArrayList<>();
            newQueue.add(song);
            setQueue(newQueue, 0);
            begin();
        }
    }

    //
    //              BASIC ACCESSOR METHODS
    //

    public void queueNext(final ArrayList<Song> songs) {
        Debug.log(Debug.VERBOSE, TAG, "Queueing a list of songs next...", context);
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
        Debug.log(Debug.VERBOSE, TAG, "Queueing a list of songs last...", context);
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

    public void toggleShuffle() {
        Debug.log(Debug.VERBOSE, TAG, "Toggling shuffle...", context);
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
        Debug.log(Debug.VERBOSE, TAG, "Shuffling the queue...", context);
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
        Debug.log(Debug.VERBOSE, TAG, "Toggling repeat...", context);
        if (repeat) {
            repeat = false;
            repeatOne = true;
            mediaPlayer.setLooping(true);
        } else {
            if (repeatOne) {
                repeat = false;
                repeatOne = false;
            } else {
                repeat = true;
                repeatOne = false;
            }
            mediaPlayer.setLooping(false);
        }
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefs.putBoolean("prefRepeat", repeat);
        prefs.putBoolean("prefRepeatOne", repeatOne);
        prefs.apply();
    }

    public Bitmap getArt() {
        return art;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public ArrayList<Song> getQueue() {
        if (shuffle) {
            return queueShuffled;
        }
        return queue;
    }

    public int getPosition() {
        if (shuffle) {
            return positionShuffled;
        }
        return position;
    }
}

