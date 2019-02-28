package com.marverenic.music.player.extensions.scrobbler;

import android.content.Context;
import android.os.Bundle;

import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.extensions.MusicPlayerExtension;

public class ScrobblerExtension extends MusicPlayerExtension {

    private static final String OPTION_ENABLED = "SLS_ENABLED";

    private boolean mEnabled;
    private SlsMessenger mMessenger;

    public ScrobblerExtension(Context context) {
        mMessenger = new SlsMessenger(context);
    }

    public static Bundle createOptionsBundle(boolean enabled) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(OPTION_ENABLED, enabled);
        return bundle;
    }

    @Override
    public void onOptionsChange(Bundle options) {
        mEnabled = options.getBoolean(OPTION_ENABLED, mEnabled);
    }

    @Override
    public void onSongStarted(MusicPlayer musicPlayer) {
        if (mEnabled) {
            mMessenger.sendStart(musicPlayer.getNowPlaying());
        }
    }

    @Override
    public void onSongCompleted(MusicPlayer musicPlayer, Song completed) {
        if (mEnabled) {
            mMessenger.sendComplete(completed);
        }
    }

    @Override
    public void onSongPaused(MusicPlayer musicPlayer) {
        if (mEnabled) {
            mMessenger.sendPause(musicPlayer.getNowPlaying());
        }
    }

    @Override
    public void onSongResumed(MusicPlayer musicPlayer) {
        if (mEnabled) {
            mMessenger.sendResume(musicPlayer.getNowPlaying());
        }
    }
}
