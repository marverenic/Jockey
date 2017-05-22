package com.marverenic.music.player;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.IPlayerService;
import com.marverenic.music.R;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.Internal;
import com.marverenic.music.utils.MediaStyleHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class PlayerService extends Service implements MusicPlayer.OnPlaybackChangeListener {

    public static final String ACTION_STOP = "PlayerService.stop";

    private static final String EXTRA_START_SILENT = "PlayerService.SILENT_START";

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
    @Internal MusicPlayer musicPlayer;

    /**
     * Used to to prevent errors caused by freeing resources twice
     */
    private boolean finished;
    /**
     * Used to keep track of whether the notification has been dismissed or not
     */
    private boolean mStopped;

    /**
     * When set to true, notifications will not be displayed until the service enters the foreground
     */
    private boolean mBeQuiet;

    public static Intent newIntent(Context context, boolean silent) {
        Intent intent = new Intent(context, PlayerService.class);
        intent.putExtra(EXTRA_START_SILENT, silent);

        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("onBind called");

        if (binder == null) {
            binder = new Stub(this);
        }
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

        if (intent != null && MediaStoreUtil.hasPermission(this)) {
            mBeQuiet = intent.getBooleanExtra(EXTRA_START_SILENT, false);

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

        /*
            By default, when this service stops, Android will keep a cached version of it so it can
            be restarted easily. When this happens, the service enters a state where the main app
            can no longer bind to it when it is started the next time. We therefore prevent this
            entirely by not allowing Android to keep the service process cached.

            This is a VERY bad idea, so make sure that this service always has its own process, and
            make sure to be very careful about cleaning up all resources before this method returns.
         */
        Process.killProcess(Process.myPid());
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Timber.i("onTaskRemoved called");

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

        if (mStopped || !musicPlayer.isPlaying()) {
            NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mgr.cancel(NOTIFICATION_ID);

            finish();
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
        if ((mBeQuiet || mStopped) && !musicPlayer.isPlaying()) {
            return;
        }

        mStopped = false;
        mBeQuiet &= !musicPlayer.isPlaying();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            startForeground(NOTIFICATION_ID, notification);
        } else if (!musicPlayer.isPlaying()) {
            Timber.i("Removing service from foreground");

            /*
               The following call to startService is a workaround for API 21 and 22 devices. If the
               main UI process is not running, then calling stopForeground() here will end the
               service completely. We therefore have the service start itself. If the service does
               then get killed, it will be restarted automatically. If the service doesn't get
               killed (regardless of API level), then this call does nothing since Android won't
               start a second instance of a service.
            */
            startService(new Intent(this, PlayerService.class));

            stopForeground(false);
            Timber.i("Bringing service into background");

            NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mgr.notify(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private boolean isUiProcessRunning() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            for (int i = 0; i < processes.size(); i++) {
                if (processes.get(i).processName.equals(BuildConfig.APPLICATION_ID)) {
                    return true;
                }
            }

            return false;
        } else {
            return !am.getAppTasks().isEmpty();
        }
    }

    public void stop() {
        Timber.i("stop called");

        mStopped = true;

        // If the UI process is still running, don't kill the process, only remove its notification
        if (isUiProcessRunning()) {
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

        private PlayerService mService;
        private List<Song> mQueueTemp;

        public Stub(PlayerService service) {
            mService = service;
        }

        private boolean isMusicPlayerReady() {
            return mService != null && mService.musicPlayer != null;
        }

        @Override
        public void stop() throws RemoteException {
            try {
                mService.stop();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.stop() failed");
                throw exception;
            }
        }

        @Override
        public void skip() throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.skip(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.skip();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.skip() failed");
                throw exception;
            }
        }

        @Override
        public void previous() throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.skip(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.skipPrevious();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.previous() failed");
                throw exception;
            }
        }

        @Override
        public void togglePlay() throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.togglePlay(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.togglePlay();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.togglePlay() failed");
                throw exception;
            }
        }

        @Override
        public void play() throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.play(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.play();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.play() failed");
                throw exception;
            }
        }

        @Override
        public void pause() throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.pause(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.pause();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.pause() failed");
                throw exception;
            }
        }

        @Override
        public void setPreferences(ImmutablePreferenceStore preferences) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setPreferences(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.updatePreferences(preferences);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setPreferences(...) failed");
                throw exception;
            }
        }

        @Override
        public void setQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setQueue(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.setQueue(newQueue, newPosition);
                if (newQueue.isEmpty()) {
                    mService.stop();
                }
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setQueue(...) failed");
                throw exception;
            }
        }

        @Override
        public void beginBigQueue() throws RemoteException {
            mQueueTemp = new ArrayList<>();
        }

        @Override
        public void sendQueueChunk(List<Song> chunk) throws RemoteException {
            List<Song> queue = mQueueTemp;
            if (queue == null) {
                throw new IllegalStateException("Must call beginBigQueue() to start a transaction");
            }
            queue.addAll(chunk);
        }

        @Override
        public void endBigQueue(boolean editQueue, int newPosition) throws RemoteException {
            List<Song> queue = mQueueTemp;
            mQueueTemp = null;
            if (queue == null) {
                throw new IllegalStateException("Must call beginBigQueue() to start a transaction");
            }
            if (editQueue) {
                editQueue(queue, newPosition);
            } else {
                setQueue(queue, newPosition);
            }
        }

        @Override
        public void changeSong(int position) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.changeSong(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.changeSong(position);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.changeSong(...) failed");
                throw exception;
            }
        }

        @Override
        public void editQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.editQueue(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.editQueue(newQueue, newPosition);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.editQueue(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueNext(Song song) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.queueNext(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.queueNext(song);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueNext(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueNextList(List<Song> songs) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.queueNextList(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.queueNext(songs);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueNextList(...) failed");
                throw exception;
            }
        }

        @Override
        public void queueLast(Song song) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.queueLast(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.queueLast(song);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueLast() failed");
                throw exception;
            }
        }

        @Override
        public void queueLastList(List<Song> songs) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.queueLastList(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.queueLast(songs);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.queueLastList(...) failed");
                throw exception;
            }
        }

        @Override
        public void seekTo(int position) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.seekTo(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.seekTo(position);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.seekTo() failed");
                throw exception;
            }
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return false;
            }

            try {
                return mService.musicPlayer.isPlaying();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.isPlaying() failed");
                throw exception;
            }
        }

        @Override
        public Song getNowPlaying() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return null;
            }

            try {
                return mService.musicPlayer.getNowPlaying();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getNowPlaying() failed");
                throw exception;
            }
        }

        @Override
        public List<Song> getQueue() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return Collections.emptyList();
            }

            try {
                return mService.musicPlayer.getQueue();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.editQueue() failed");
                throw exception;
            }
        }

        @Override
        public int getQueuePosition() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getQueuePosition();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getQueuePosition() failed");
                throw exception;
            }
        }

        @Override
        public int getQueueSize() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getQueueSize();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getQueueSize() failed");
                throw exception;
            }
        }

        @Override
        public List<Song> getQueueChunk(int offset, int length) throws RemoteException {
            List<Song> currentQueue = getQueue();
            try {
                return currentQueue.subList(offset, offset + length);
            } catch (IndexOutOfBoundsException ex) {
                // https://blog.classycode.com/dealing-with-exceptions-in-aidl-9ba904c6d63
                // propagate exception to client to handle, maybe queue is changed while client side
                // try to read it
                Timber.e(ex, "getQueueChunk ERROR: offset: %d; length: %d; queue size: %d",
                        offset, length, currentQueue.size());
                throw new IllegalArgumentException("Invalid offset:" + offset
                        + "and length:" + length
                        + "; queue size: " + currentQueue.size());
            } catch (RuntimeException ex) {
                Timber.e(ex, "Remote call to PlayerService.getQueueChunk() failed");
                throw ex;
            }
        }

        @Override
        public int getCurrentPosition() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getCurrentPosition();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getCurrentPosition() failed");
                throw exception;
            }
        }

        @Override
        public int getDuration() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getDuration();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getDuration() failed");
                throw exception;
            }
        }

        @Override
        public PlayerState getPlayerState() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return null;
            }

            try {
                return mService.musicPlayer.getState();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getPlayerState() failed");
                throw exception;
            }
        }

        @Override
        public void restorePlayerState(PlayerState state) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("restorePlayerState(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.restorePlayerState(state);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.restorePlayerState() failed");
                throw exception;
            }
        }

        @Override
        public int getMultiRepeatCount() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getMultiRepeatCount();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getMultiRepeatCount() failed");
                throw exception;
            }
        }

        @Override
        public void setMultiRepeatCount(int count) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setMultiRepeat(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.setMultiRepeat(count);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setMultiRepeatCount() failed");
                throw exception;
            }
        }

        @Override
        public long getSleepTimerEndTime() throws RemoteException {
            if (!isMusicPlayerReady()) {
                return 0;
            }

            try {
                return mService.musicPlayer.getSleepTimerEndTime();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.getSleepTimerEndTime() failed");
                throw exception;
            }
        }

        @Override
        public void setSleepTimerEndTime(long timestampInMillis) throws RemoteException {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setSleepTimer(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.setSleepTimer(timestampInMillis);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setSleepTimerEndTime() failed");
            }
        }
    }
}

