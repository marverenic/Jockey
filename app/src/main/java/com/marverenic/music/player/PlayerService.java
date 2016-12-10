package com.marverenic.music.player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

import com.marverenic.music.IPlayerService;
import com.marverenic.music.R;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.MediaStyleHelper;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class PlayerService extends Service implements MusicPlayer.OnPlaybackChangeListener {

    public static final String ACTION_STOP = "PlayerService.stop";

    public static final int NOTIFICATION_ID = 1;

    /**
     * The service instance in use (singleton)
     */
    private static PlayerService instance;

    /**
     * Used in binding and unbinding this service to the UI process
     */
    private static IBinder binder;

    // Instance variables
    /**
     * The media player for the service instance
     */
    private MusicPlayer musicPlayer;

    /**
     * Used to to prevent errors caused by freeing resources twice
     */
    private boolean finished;
    /**
     * Used to keep track of whether the main application window is open or not
     */
    private boolean mAppRunning;
    /**
     * Used to keep track of whether the notification has been dismissed or not
     */
    private boolean mStopped;

    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("onBind called");

        if (binder == null) {
            binder = new Stub();
        }
        mAppRunning = true;
        return binder;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("onCreate() called");

        if (!MediaStoreUtil.hasPermission(this)) {
            Timber.w("Attempted to start service without Storage permission. Aborting.");
            stopSelf();
            return;
        }

        if (instance == null) {
            instance = this;
        } else {
            Timber.w("Attempted to create a second PlayerService");
            stopSelf();
            return;
        }

        if (musicPlayer == null) {
            musicPlayer = new MusicPlayer(this);
        }

        mStopped = false;
        finished = false;

        musicPlayer.setPlaybackChangeListener(this);
        musicPlayer.loadState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("onStartCommand called");
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            if (intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
                MediaButtonReceiver.handleIntent(musicPlayer.getMediaSession(), intent);
                Timber.i(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString());
            } else if (ACTION_STOP.equals(intent.getAction())) {
                stop();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.i("Called onDestroy");
        finish();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Timber.i("onTaskRemoved called");
        mAppRunning = false;

        /*
            When the application is removed from the overview page, we make the notification
            dismissible on Lollipop and higher devices if music is paused. To do this, we have to
            move the service out of the foreground state. As soon as this happens, ActivityManager
            will kill the service because it isn't in the foreground. Because the service is
            sticky, it will get queued to be restarted.

            Because our service has a chance of getting recreated as a result of this event in
            the lifecycle, we have to save the state of the media player under the assumption that
            we're about to be killed. If we are killed, this state will just be reloaded when the
            service is recreated, and all the user sees is their notification temporarily
            disappearing when they pause music and swipe Jockey out of their recent apps.

            There is no other way I'm aware of to implement a remote service that transitions
            between the foreground and background (as required by the platform's media style since
            it can't have a close button on L+ devices) without being recreated and requiring
            this workaround.
         */
        try {
            musicPlayer.saveState();
        } catch (IOException exception) {
            Timber.e(exception, "Failed to save music player state");
        }

        if (mStopped) {
            stop();
        } else {
            notifyNowPlaying();
        }
    }

    public static PlayerService getInstance() {
        return instance;
    }

    /**
     * Generate and post a notification for the current player status
     * Posts the notification by starting the service in the foreground
     */
    public void notifyNowPlaying() {
        Timber.i("notifyNowPlaying called");

        if (musicPlayer.getNowPlaying() == null) {
            Timber.i("Not showing notification -- nothing is playing");
            return;
        }

        MediaSessionCompat mediaSession = musicPlayer.getMediaSession();
        if (mediaSession == null) {
            Timber.i("Not showing notification. Media session is uninitialized");
            return;
        }

        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession);

        setupNotificationActions(builder);

        builder.setSmallIcon(getNotificationIcon())
                .setDeleteIntent(getStopIntent())
                .setStyle(
                        new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(getStopIntent())
                                .setMediaSession(musicPlayer.getMediaSession().getSessionToken()));

        showNotification(builder.build());
    }

    @DrawableRes
    private int getNotificationIcon() {
        if (musicPlayer.isPlaying()) {
            return R.drawable.ic_play_arrow_24dp;
        } else {
            return R.drawable.ic_pause_24dp;
        }
    }

    private void setupNotificationActions(NotificationCompat.Builder builder) {
        addNotificationAction(builder, R.drawable.ic_skip_previous_36dp,
                R.string.action_previous, KeyEvent.KEYCODE_MEDIA_PREVIOUS);

        if (musicPlayer.isPlaying()) {
            addNotificationAction(builder, R.drawable.ic_pause_36dp,
                    R.string.action_pause, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else {
            addNotificationAction(builder, R.drawable.ic_play_arrow_36dp,
                    R.string.action_play, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }

        addNotificationAction(builder, R.drawable.ic_skip_next_36dp,
                R.string.action_skip, KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    private void addNotificationAction(NotificationCompat.Builder builder,
                                       @DrawableRes int icon, @StringRes int string,
                                       int keyEvent) {

        PendingIntent intent = MediaStyleHelper.getActionIntent(this, keyEvent);
        builder.addAction(new NotificationCompat.Action(icon, getString(string), intent));
    }

    private PendingIntent getStopIntent() {
        Intent intent = new Intent(this, PlayerService.class);
        intent.setAction(ACTION_STOP);

        return PendingIntent.getService(this, 0, intent, 0);
    }

    private void showNotification(Notification notification) {
        mStopped = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startForeground(NOTIFICATION_ID, notification);
        } else if (!musicPlayer.isPlaying()) {
            stopForeground(false);

            NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mgr.notify(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    public void stop() {
        Timber.i("stop called");

        mStopped = true;

        // If the UI process is still running, don't kill the process, only remove its notification
        if (mAppRunning) {
            musicPlayer.pause();
            stopForeground(true);
            return;
        }

        // If the service is being completely stopped by the user, turn off the sleep timer
        musicPlayer.setSleepTimer(0);

        // If the UI process has already ended, kill the service and close the player
        finish();
    }

    public void finish() {
        Timber.i("finish() called");
        if (!finished) {
            if (musicPlayer != null) {
                try {
                    musicPlayer.saveState();
                } catch (IOException exception) {
                    Timber.e(exception, "Failed to save player state");
                }

                musicPlayer.release();
                musicPlayer = null;
            }
            stopForeground(true);
            instance = null;
            stopSelf();
            finished = true;
        }
    }

    @Override
    public void onPlaybackChange() {
        notifyNowPlaying();
    }

    public static class Stub extends IPlayerService.Stub {

        @Override
        public void stop() throws RemoteException {
            try {
                instance.stop();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.stop() failed");
                throw exception;
            }
        }

        @Override
        public void skip() throws RemoteException {
            try {
                instance.musicPlayer.skip();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.skip() failed");
                throw exception;
            }
        }

        @Override
        public void previous() throws RemoteException {
            try {
                instance.musicPlayer.skipPrevious();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.previous() failed");
                throw exception;
            }
        }

        @Override
        public void togglePlay() throws RemoteException {
            try {
                instance.musicPlayer.togglePlay();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.togglePlay() failed");
                throw exception;
            }
        }

        @Override
        public void play() throws RemoteException {
            try {
                instance.musicPlayer.play();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.play() failed");
                throw exception;
            }
        }

        @Override
        public void pause() throws RemoteException {
            try {
                instance.musicPlayer.play();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.pause() failed");
                throw exception;
            }
        }

        @Override
        public void setPreferences(ImmutablePreferenceStore preferences) throws RemoteException {
            try {
                instance.musicPlayer.updatePreferences(preferences);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setPreferences(...) failed");
                throw exception;
            }
        }

        @Override
        public void setQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            try {
                instance.musicPlayer.setQueue(newQueue, newPosition);
                if (newQueue.isEmpty()) {
                    instance.stop();
                }
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setQueue(...) failed");
                throw exception;
            }
        }

        @Override
        public void changeSong(int position) throws RemoteException {
            try {
                instance.musicPlayer.changeSong(position);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.changeSong(...) failed");
                throw exception;
            }
        }

        @Override
        public void editQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            try {
                instance.musicPlayer.editQueue(newQueue, newPosition);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.editQueue(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueNext(Song song) throws RemoteException {
            try {
                instance.musicPlayer.queueNext(song);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueNext(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueNextList(List<Song> songs) throws RemoteException {
            try {
                instance.musicPlayer.queueNext(songs);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueNextList(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueLast(Song song) throws RemoteException {
            try {
                instance.musicPlayer.queueLast(song);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueLast() failed");
                throw exception;
            }
        }

        @Override
        public void queueLastList(List<Song> songs) throws RemoteException {
            try {
                instance.musicPlayer.queueLast(songs);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueLastList(...) failed");
                throw exception;
            }
        }

        @Override
        public void seekTo(int position) throws RemoteException {
            try {
                instance.musicPlayer.seekTo(position);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.seekTo() failed");
                throw exception;
            }
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            try {
                return instance.musicPlayer.isPlaying();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.isPlaying() failed");
                throw exception;
            }
        }

        @Override
        public Song getNowPlaying() throws RemoteException {
            try {
                return instance.musicPlayer.getNowPlaying();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getNowPlaying() failed");
                throw exception;
            }
        }

        @Override
        public List<Song> getQueue() throws RemoteException {
            try {
                return instance.musicPlayer.getQueue();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.editQueue() failed");
                throw exception;
            }
        }

        @Override
        public int getQueuePosition() throws RemoteException {
            try {
                return instance.musicPlayer.getQueuePosition();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getQueuePosition() failed");
                throw exception;
            }
        }

        @Override
        public int getQueueSize() throws RemoteException {
            try {
                return instance.musicPlayer.getQueueSize();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getQueueSize() failed");
                throw exception;
            }
        }

        @Override
        public int getCurrentPosition() throws RemoteException {
            try {
                return instance.musicPlayer.getCurrentPosition();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getCurrentPosition() failed");
                throw exception;
            }
        }

        @Override
        public int getDuration() throws RemoteException {
            try {
                return instance.musicPlayer.getDuration();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getDuration() failed");
                throw exception;
            }
        }

        @Override
        public int getMultiRepeatCount() throws RemoteException {
            try {
                return instance.musicPlayer.getMultiRepeatCount();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getMultiRepeatCount() failed");
                throw exception;
            }
        }

        @Override
        public void setMultiRepeatCount(int count) throws RemoteException {
            try {
                instance.musicPlayer.setMultiRepeat(count);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setMultiRepeatCount() failed");
                throw exception;
            }
        }

        @Override
        public long getSleepTimerEndTime() throws RemoteException {
            try {
                return instance.musicPlayer.getSleepTimerEndTime();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getSleepTimerEndTime() failed");
                throw exception;
            }
        }

        @Override
        public void setSleepTimerEndTime(long timestampInMillis) throws RemoteException {
            try {
                instance.musicPlayer.setSleepTimer(timestampInMillis);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setSleepTimerEndTime() failed");
            }
        }
    }
}

