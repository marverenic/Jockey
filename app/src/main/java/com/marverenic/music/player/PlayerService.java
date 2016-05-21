package com.marverenic.music.player;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.IPlayerService;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.MediaStyleHelper;

import java.util.List;

public class PlayerService extends Service implements MusicPlayer.OnPlaybackChangeListener {

    private static final String TAG = "PlayerService";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static final int NOTIFICATION_ID = 1;

    // Intent Action & Extra names
    /**
     * Toggle between play and pause
     */
    public static final String ACTION_TOGGLE_PLAY = "com.marverenic.music.action.TOGGLE_PLAY";
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
    private boolean finished = false; // Don't attempt to release resources more than once

    @Override
    public IBinder onBind(Intent intent) {
        if (binder == null) {
            binder = new Stub();
        }
        return binder;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.i(TAG, "onCreate() called");

        if (instance == null) {
            instance = this;
        } else {
            if (DEBUG) Log.w(TAG, "Attempted to create a second PlayerService");
            stopSelf();
            return;
        }

        if (musicPlayer == null) {
            musicPlayer = new MusicPlayer(this);
        }

        musicPlayer.setPlaybackChangeListener(this);
        musicPlayer.loadState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            MediaButtonReceiver.handleIntent(musicPlayer.getMediaSession(), intent);
            Log.i(TAG, intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT).toString());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "Called onDestroy()");
        try {
            musicPlayer.saveState(null);
        } catch (Exception ignored) {

        }
        finish();
        super.onDestroy();
    }

    public static PlayerService getInstance() {
        return instance;
    }

    /**
     * Generate and post a notification for the current player status
     * Posts the notification by starting the service in the foreground
     */
    public void notifyNowPlaying() {
        if (DEBUG) Log.i(TAG, "notifyNowPlaying() called");

        if (musicPlayer.getNowPlaying() == null) {
            if (DEBUG) Log.i(TAG, "Not showing notification -- nothing is playing");
            return;
        }

        NotificationCompat.Builder builder =
                MediaStyleHelper.from(this, musicPlayer.getMediaSession());

        // TODO set color
        builder
                .setSmallIcon(
                        (musicPlayer.isPlaying() || musicPlayer.isPreparing())
                                ? R.drawable.ic_play_arrow_24dp
                                : R.drawable.ic_pause_24dp)
                .setDeleteIntent(
                        MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP));

        builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_skip_previous_36dp, getString(R.string.action_previous),
                        MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS)));

        if (musicPlayer.isPlaying()) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause_36dp, getString(R.string.action_pause),
                    MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play_arrow_36dp, getString(R.string.action_play),
                    MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)));
        }

        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_skip_next_36dp, getString(R.string.action_skip),
                MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_NEXT)));

        builder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                        MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP))
                .setMediaSession(musicPlayer.getMediaSession().getSessionToken()));

        // TODO handle stopForeground
        startForeground(NOTIFICATION_ID, builder.build());
    }

    public void stop() {
        if (DEBUG) Log.i(TAG, "stop() called");

        // If the UI process is still running, don't kill the process, only remove its notification
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos =
                activityManager.getRunningAppProcesses();
        for (int i = 0; i < procInfos.size(); i++) {
            if (procInfos.get(i).processName.equals(BuildConfig.APPLICATION_ID)) {
                musicPlayer.pause();
                stopForeground(true);
                return;
            }
        }

        // If the UI process has already ended, kill the service and close the player
        finish();
    }

    public void finish() {
        if (DEBUG) Log.i(TAG, "finish() called");
        if (!finished) {
            musicPlayer.release();
            musicPlayer = null;
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
            instance.stop();
        }

        @Override
        public void skip() throws RemoteException {
            instance.musicPlayer.skip();
        }

        @Override
        public void previous() throws RemoteException {
            instance.musicPlayer.skipPrevious();
        }

        @Override
        public void begin() throws RemoteException {
            instance.musicPlayer.prepare(true);
        }

        @Override
        public void togglePlay() throws RemoteException {
            instance.musicPlayer.togglePlay();
        }

        @Override
        public void play() throws RemoteException {
            instance.musicPlayer.play();
        }

        @Override
        public void pause() throws RemoteException {
            instance.musicPlayer.play();
        }

        @Override
        public void setShuffle(boolean shuffle) throws RemoteException {
            instance.musicPlayer.setShuffle(shuffle);
        }

        @Override
        public void setRepeat(int repeat) throws RemoteException {
            instance.musicPlayer.setRepeat(repeat);
        }

        @Override
        public void setQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            instance.musicPlayer.setQueue(newQueue, newPosition);
        }

        @Override
        public void changeSong(int position) throws RemoteException {
            instance.musicPlayer.changeSong(position);
        }

        @Override
        public void editQueue(List<Song> newQueue, int newPosition) throws RemoteException {
            instance.musicPlayer.editQueue(newQueue, newPosition);
        }

        @Override
        public void queueNext(Song song) throws RemoteException {
            instance.musicPlayer.queueNext(song);
        }

        @Override
        public void queueNextList(List<Song> songs) throws RemoteException {
            instance.musicPlayer.queueNext(songs);
        }

        @Override
        public void queueLast(Song song) throws RemoteException {
            instance.musicPlayer.queueLast(song);
        }

        @Override
        public void queueLastList(List<Song> songs) throws RemoteException {
            instance.musicPlayer.queueLast(songs);
        }

        @Override
        public void seekTo(int position) throws RemoteException {
            instance.musicPlayer.seekTo(position);
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return instance.musicPlayer.isPlaying();
        }

        @Override
        public Song getNowPlaying() throws RemoteException {
            return instance.musicPlayer.getNowPlaying();
        }

        @Override
        public List<Song> getQueue() throws RemoteException {
            return instance.musicPlayer.getQueue();
        }

        @Override
        public int getQueuePosition() throws RemoteException {
            return instance.musicPlayer.getQueuePosition();
        }

        @Override
        public int getQueueSize() throws RemoteException {
            return instance.musicPlayer.getQueueSize();
        }

        @Override
        public int getCurrentPosition() throws RemoteException {
            return instance.musicPlayer.getCurrentPosition();
        }

        @Override
        public int getDuration() throws RemoteException {
            return instance.musicPlayer.getDuration();
        }

        @Override
        public int getAudioSessionId() throws RemoteException {
            return instance.musicPlayer.getAudioSessionId();
        }
    }
}

