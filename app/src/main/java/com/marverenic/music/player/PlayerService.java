package com.marverenic.music.player;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.MediaKeyAction;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.IPlayerService;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.browser.MediaBrowserRoot;
import com.marverenic.music.player.extensions.persistence.PersistenceExtension;
import com.marverenic.music.player.extensions.playcount.PlayCountExtension;
import com.marverenic.music.player.extensions.scrobbler.ScrobblerExtension;
import com.marverenic.music.player.persistence.PlaybackPersistenceManager;
import com.marverenic.music.player.transaction.ChunkHeader;
import com.marverenic.music.player.transaction.IncomingTransaction;
import com.marverenic.music.player.transaction.ListTransaction;
import com.marverenic.music.player.transaction.TransactionToken;
import com.marverenic.music.ui.library.LibraryActivity;
import com.marverenic.music.utils.Internal;
import com.marverenic.music.utils.MediaStyleHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class PlayerService extends Service implements MusicPlayer.OnPlaybackChangeListener {

    private static final String EXTRA_START_SILENT = "PlayerService.SILENT_START";
    private static final String EXTRA_PLAYER_OPTIONS = "PlayerService.PLAYER_OPTIONS";

    private static final String NOTIFICATION_CHANNEL_ID = "music-service";
    private static final int NOTIFICATION_ID = 1;

    // Instance variables
    /**
     * The media player for the service instance
     */
    @Internal MusicPlayer musicPlayer;

    @Inject PlaybackPersistenceManager mPlaybackPersistenceManager;
    @Inject MediaBrowserRoot mMediaBrowserRoot;
    @Inject PlayCountStore mPlayCountStore;

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

    public static Intent newIntent(Context context, PlayerOptions options, boolean silent) {
        Intent intent = new Intent(context, PlayerService.class);
        intent.putExtra(EXTRA_START_SILENT, silent);
        intent.putExtra(EXTRA_PLAYER_OPTIONS, options);

        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("onBind called");
        return new PlayerServiceBinder(this);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.i("onCreate() called");
        JockeyApplication.getComponent(this).inject(this);

        if (!MediaStoreUtil.hasPermission(this)) {
            Timber.w("Attempted to start service without Storage permission. Aborting.");
            stopSelf();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("onStartCommand called");
        super.onStartCommand(intent, flags, startId);

        if (musicPlayer == null) {
            onCreateMusicPlayer(intent);
        }

        if (intent != null && MediaStoreUtil.hasPermission(this)) {
            mBeQuiet = intent.getBooleanExtra(EXTRA_START_SILENT, false);

            if (intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
                MediaButtonReceiver.handleIntent(musicPlayer.getMediaSession(), intent);
                Timber.i(intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString());
            }
        }
        return START_STICKY;
    }

    private void onCreateMusicPlayer(Intent intent) {
        PlayerOptions options = intent.getParcelableExtra(EXTRA_PLAYER_OPTIONS);

        musicPlayer = new MusicPlayer(this, options,
                PendingIntent.getActivity(
                        this, 0,
                        LibraryActivity.newNowPlayingIntent(this)
                                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_CANCEL_CURRENT),
                mMediaBrowserRoot,
                Arrays.asList(
                        new PersistenceExtension(mPlaybackPersistenceManager, this),
                        new ScrobblerExtension(this),
                        new PlayCountExtension(mPlayCountStore)
                ));
        musicPlayer.setPlaybackChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Timber.i("Called onDestroy");
        finish();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Timber.i("onTaskRemoved called");

        if (musicPlayer == null) {
            finish();
            return;
        }

        if (mStopped || !musicPlayer.isPlaying()) {
            NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mgr.cancel(NOTIFICATION_ID);

            finish();
        } else {
            notifyNowPlaying();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.player_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);

        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mgr.createNotificationChannel(channel);
    }

    /**
     * Generate and post a notification for the current player status
     * Posts the notification by starting the service in the foreground
     */
    private void notifyNowPlaying() {
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

        NotificationCompat.Builder builder =
                MediaStyleHelper.from(this, mediaSession, NOTIFICATION_CHANNEL_ID);

        setupNotificationActions(builder);

        PendingIntent stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_STOP);

        builder.setSmallIcon(getNotificationIcon())
                .setDeleteIntent(stopIntent)
                .setStyle(
                        new MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(stopIntent)
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
                R.string.action_previous, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

        if (musicPlayer.isPlaying()) {
            addNotificationAction(builder, R.drawable.ic_pause_36dp,
                    R.string.action_pause, PlaybackStateCompat.ACTION_PLAY_PAUSE);
        } else {
            addNotificationAction(builder, R.drawable.ic_play_arrow_36dp,
                    R.string.action_play, PlaybackStateCompat.ACTION_PLAY_PAUSE);
        }

        addNotificationAction(builder, R.drawable.ic_skip_next_36dp,
                R.string.action_skip, PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
    }

    private void addNotificationAction(NotificationCompat.Builder builder,
                                       @DrawableRes int icon, @StringRes int string,
                                       @MediaKeyAction long action) {

        PendingIntent intent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, action);
        builder.addAction(new NotificationCompat.Action(icon, getString(string), intent));
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
               The following call to startService is a workaround for API 21+ devices. If the
               main UI process is not running, then calling stopForeground() here will end the
               service completely. We therefore have the service start itself. If the service does
               then get killed, it will be restarted automatically. If the service doesn't get
               killed (regardless of API level), then this call does nothing since Android won't
               start a second instance of a service.
            */
            try {
                startService(new Intent(this, PlayerService.class));
            } catch (IllegalStateException e) {
                /*
                   This is kind of a hack on top of a workaround for API 26+ devices. On Oreo+
                   devices, attempting to call startService while the service isn't in the
                   background will trigger an IllegalStateException because the service isn't in
                   the foreground. However, in this case the service is already in the background,
                   there isn't a transition between these states, so the original workaround
                   is unnecessary. Because there isn't a recommended way of telling whether a
                   service is in the foreground, let's always try this workaround. If we are
                   transitioning from foreground to background, then this will keep the service
                   alive. If we're not transitioning on an O+ device, we can safely eat this
                   exception.
                */
                Timber.i(e, "Failed to apply workaround while transitioning into background %s",
                        "(is the service already in the background?)");
            }

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

        // If the UI process has already ended, kill the service and close the player
        finish();
    }

    public void finish() {
        Timber.i("finish() called");
        if (!finished) {
            if (musicPlayer != null) {
                // If the service is being completely stopped by the user, turn off the sleep timer
                musicPlayer.setSleepTimer(0);

                musicPlayer.release();
                musicPlayer = null;
            }
            stopForeground(true);
            stopSelf();
            finished = true;
        }
    }

    @Override
    public void onPlaybackChange() {
        notifyNowPlaying();
    }

    @Override
    public void onPlaybackStop() {
        stop();
    }

    public static class PlayerServiceBinder extends IPlayerService.Stub {

        private PlayerService mService;
        private IncomingTransaction<List<Song>> mQueueTransaction;

        PlayerServiceBinder(PlayerService service) {
            mService = service;
        }

        private boolean isMusicPlayerReady() {
            return mService != null && mService.musicPlayer != null;
        }

        @Override
        public void stop() {
            try {
                mService.stop();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.stop() failed");
                throw exception;
            }
        }

        @Override
        public void skip() {
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
        public void previous() {
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
        public void togglePlay() {
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
        public void play() {
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
        public void pause() {
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
        public void setPreferences(PlayerOptions options, long seed) {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setPreferences(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.updatePreferences(options, seed);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.updatePreferences(...) failed");
                throw exception;
            }
        }

        @Override
        public void setQueue(List<Song> newQueue, int newPosition, long seed) {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.setQueue(): Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.setQueue(newQueue, newPosition, seed);
                if (newQueue.isEmpty()) {
                    mService.stop();
                }
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.setQueue(...) failed");
                throw exception;
            }
        }

        @Override
        public void beginLargeQueueTransaction(TransactionToken token) {
            if (mQueueTransaction != null) {
                Timber.i("LargeQueueTransaction is already open. Dropping previous transaction.");
            }
            mQueueTransaction = ListTransaction.receive(token);
        }

        @Override
        public void sendQueueChunk(ChunkHeader header, List<Song> chunk) {
            mQueueTransaction.receive(header, chunk);
        }

        @Override
        public void endLargeQueueTransaction(int newPosition, long seed) {
            IncomingTransaction<List<Song>> transaction = mQueueTransaction;
            if (transaction == null) {
                throw new IllegalStateException("Transaction has not started");
            } else if (!transaction.isTransmissionComplete()) {
                throw new IllegalStateException("Transaction has not completed");
            }

            setQueue(transaction.getData(), newPosition, seed);
            mQueueTransaction = null;
        }

        @Override
        public void endLargeQueueEdit(int newPosition) {
            IncomingTransaction<List<Song>> transaction = mQueueTransaction;
            if (transaction == null) {
                throw new IllegalStateException("Transaction has not started");
            } else if (!transaction.isTransmissionComplete()) {
                throw new IllegalStateException("Transaction has not completed");
            }

            editQueue(mQueueTransaction.getData(), newPosition);
            mQueueTransaction = null;
        }

        @Override
        public void changeSong(int position) {
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
        public void editQueue(List<Song> newQueue, int newPosition) {
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
        public void queueNext(Song song) {
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
        public void queueNextList(List<Song> songs) {
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
        public void queueLast(Song song) {
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
        public void queueLastList(List<Song> songs) {
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
        public void seekTo(int position) {
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
        public boolean isPlaying() {
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
        public Song getNowPlaying() {
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
        public List<Song> getQueue() {
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
        public int getQueuePosition() {
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
        public int getQueueSize() {
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
        public List<Song> getQueueChunk(int offset, int length) {
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
        public int getCurrentPosition() {
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
        public int getDuration() {
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
        public PlayerState getPlayerState() {
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
        public void restorePlayerState(PlayerState state) {
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
        public int getMultiRepeatCount() {
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
        public void setMultiRepeatCount(int count) {
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
        public boolean getShuffleMode() {
            if (!isMusicPlayerReady()) {
                return false;
            }

            return mService.musicPlayer.isShuffled();
        }

        @Override
        public int getRepeatMode() {
            if (!isMusicPlayerReady()) {
                return MusicPlayer.REPEAT_NONE;
            }

            return mService.musicPlayer.getRepeatMode();
        }

        @Override
        public long getSleepTimerEndTime() {
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
        public void setSleepTimerEndTime(long timestampInMillis) {
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

        @Override
        public void updateExtensionOptions(Bundle options) {
            if (!isMusicPlayerReady()) {
                Timber.i("PlayerService.updateExtensionOptions(): " +
                        "Service is not ready. Dropping command");
                return;
            }

            try {
                mService.musicPlayer.updateExtensionOptions(options);
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to PlayerService.updateExtensionOptions() failed");
            }
        }

        @Override
        public MediaSessionCompat.Token getMediaSessionToken() {
            if (!isMusicPlayerReady()) {
                return null;
            }

            try {
                return mService.musicPlayer.getMediaSession().getSessionToken();
            } catch (RuntimeException exception) {
                Timber.e(exception, "Remote call to getMediaSessionToken failed");
                throw exception;
            }
        }
    }
}

