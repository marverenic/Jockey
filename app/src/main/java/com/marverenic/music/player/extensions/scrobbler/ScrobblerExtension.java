package com.marverenic.music.player.extensions.scrobbler;

import android.content.Context;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.extensions.MusicPlayerExtension;

public class ScrobblerExtension extends MusicPlayerExtension {

    private boolean mEnabled;
    private SlsMessenger mMessenger;

    public ScrobblerExtension(Context context) {
        mMessenger = new SlsMessenger(context);
    }

    @Override
    public void onCreateMusicPlayer(MusicPlayer musicPlayer, ReadOnlyPreferenceStore preferences) {
        onSettingsChanged(preferences);
    }

    @Override
    public void onSettingsChanged(ReadOnlyPreferenceStore preferences) {
        mEnabled = preferences.getEqualizerEnabled();
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
