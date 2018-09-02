package com.marverenic.music.player.extensions.scrobbler;

import android.content.Context;

import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.extensions.MusicPlayerExtension;

public class ScrobblerExtension extends MusicPlayerExtension {
    private SlsMessenger mMessenger;

    public ScrobblerExtension(Context context) {
        mMessenger = new SlsMessenger(context);
    }

    @Override
    public void onSongStarted(MusicPlayer musicPlayer) {
        mMessenger.sendStart(musicPlayer.getNowPlaying());
    }

    @Override
    public void onSongCompleted(MusicPlayer musicPlayer, Song completed) {
        mMessenger.sendComplete(completed);
    }

    @Override
    public void onSongPaused(MusicPlayer musicPlayer) {
        mMessenger.sendPause(musicPlayer.getNowPlaying());
    }

    @Override
    public void onSongResumed(MusicPlayer musicPlayer) {
        mMessenger.sendResume(musicPlayer.getNowPlaying());
    }
}
