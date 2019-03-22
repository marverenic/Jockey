package com.marverenic.music.player.extensions.scrobbler;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.marverenic.music.R;
import com.marverenic.music.model.Song;

import java.util.concurrent.TimeUnit;

/**
 * A helper class for interacting with Simple Last.fm Scrobbler. The API description could be found
 * here: https://github.com/tgwizard/sls/blob/master/Developer's%20API.md
 */

public class SlsMessenger {
    private static final int STATE_START = 0;
    private static final int STATE_RESUME = 1;
    private static final int STATE_PAUSE = 2;
    private static final int STATE_COMPLETE = 3;

    private static final String SLS_PACKAGE = "com.adam.aslfms";
    private static final String SLS_ACTION = "com.adam.aslfms.notify.playstatechanged";

    private static final String KEY_APP_NAME = "app-name";
    private static final String KEY_APP_PACKAGE = "app-package";
    private static final String KEY_ALBUM  = "album";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_TRACK = "track";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_TRACK_NUMBER = "track-number";
    private static final String KEY_STATE = "state";

    private Context mContext;

    public SlsMessenger(Context context) {
        mContext = context;
    }

    public static boolean isSlsInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SLS_PACKAGE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private Intent base() {
        return new Intent(SLS_ACTION)
                .putExtra(KEY_APP_NAME, mContext.getString(R.string.app_name))
                .putExtra(KEY_APP_PACKAGE, mContext.getPackageName());
    }

    private Intent withSong(Song song) {
        return base()
                .putExtra(KEY_ALBUM, song.getAlbumName())
                .putExtra(KEY_ARTIST, song.getArtistName())
                .putExtra(KEY_TRACK, song.getSongName())
                .putExtra(KEY_DURATION, (int) TimeUnit.MILLISECONDS.toSeconds(song.getSongDuration()))
                .putExtra(KEY_TRACK_NUMBER, song.getTrackNumber());
    }

    /**
     * Should be called when new song began to play
     * @param song the new song
     */
    public void sendStart(@Nullable Song song) {
        if (song != null) {
            mContext.sendBroadcast(withSong(song).putExtra(KEY_STATE, STATE_START));
        }
    }

    /**
     * Should be called when a song was resumed after pause
     * @param song the resumed song
     */
    public void sendResume(@Nullable Song song) {
        if (song != null) {
            mContext.sendBroadcast(withSong(song).putExtra(KEY_STATE, STATE_RESUME));
        }
    }

    /**
     * Should be called when song is paused
     * @param song the paused song
     */
    public void sendPause(@Nullable Song song) {
        if (song != null) {
            mContext.sendBroadcast(withSong(song).putExtra(KEY_STATE, STATE_PAUSE));
        }
    }

    /**
     * Should be called when a song is just completed
     * @param song the completed song
     */
    public void sendComplete(@Nullable Song song) {
        if (song != null) {
            mContext.sendBroadcast(withSong(song).putExtra(KEY_STATE, STATE_COMPLETE));
        }
    }
}
