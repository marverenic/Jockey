package com.marverenic.music.player.extensions.playcount;

import android.support.annotation.Nullable;

import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.extensions.MusicPlayerExtension;

import timber.log.Timber;

public class PlayCountExtension extends MusicPlayerExtension {

    /**
     * Defines the minimum duration that must be passed for a song to be considered "played" when
     * logging play counts
     * This value is measured in milliseconds and is currently set to 24 seconds
     */
    private static final int PLAY_COUNT_THRESHOLD = 24000;

    /**
     * Defines the maximum duration that a song can reach to be considered "skipped" when logging
     * play counts
     * This value is measured in milliseconds and is currently set to 20 seconds
     */
    private static final int SKIP_COUNT_THRESHOLD = 20000;

    private PlayCountStore mPlayCountStore;

    public PlayCountExtension(PlayCountStore playCountStore) {
        mPlayCountStore = playCountStore;
        mPlayCountStore.refresh()
            .subscribe(complete -> {
                Timber.i("init: Initialized play count store values");
            }, throwable -> {
                Timber.e(throwable, "init: Failed to read play count store values");
            });
    }

    @Override
    public void onSongCompleted(MusicPlayer musicPlayer, Song completed) {
        logPlayCount(completed, false);
    }

    @Override
    public void onSongSkipped(MusicPlayer musicPlayer, boolean byUser) {
        if (byUser) {
            logPlay(musicPlayer.getNowPlaying(), musicPlayer.getCurrentPosition(), musicPlayer.getDuration());
        }
    }

    private void logPlay(@Nullable Song nowPlaying, long seekPositionMs, long durationMs) {
        Timber.i("Logging play count...");
        if (nowPlaying != null) {
            if (seekPositionMs > PLAY_COUNT_THRESHOLD || seekPositionMs > durationMs / 2) {
                // Log a play if we're passed a certain threshold or more than 50% in a song
                // (whichever is smaller)
                Timber.i("Marking song as played");
                logPlayCount(nowPlaying, false);
            } else if (seekPositionMs < SKIP_COUNT_THRESHOLD) {
                // If we're not very far into this song, log a skip
                Timber.i("Marking song as skipped");
                logPlayCount(nowPlaying, true);
            } else {
                Timber.i("Not writing play count. Song was neither played nor skipped.");
            }
        }
    }

    private void logPlayCount(Song song, boolean skip) {
        Timber.i("Logging %s count to PlayCountStore for %s...", (skip) ? "skip" : "play", song.toString());
        if (skip) {
            mPlayCountStore.incrementSkipCount(song);
        } else {
            mPlayCountStore.incrementPlayCount(song);
            mPlayCountStore.setPlayDateToNow(song);
        }
        Timber.i("Writing PlayCountStore to disk...");
        mPlayCountStore.save();
    }
}
