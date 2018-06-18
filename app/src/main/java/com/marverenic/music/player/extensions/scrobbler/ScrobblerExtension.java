package com.marverenic.music.player.extensions.scrobbler;

import android.content.Context;

import com.marverenic.music.model.Song;
import com.marverenic.music.player.extensions.MusicPlayerExtension;

public class ScrobblerExtension extends MusicPlayerExtension {
    private SlsMessenger mMessenger;

    public ScrobblerExtension(Context context) {
        mMessenger = new SlsMessenger(context);
    }

    @Override
    public void onSongStarted(Song song) {
        mMessenger.sendStart(song);
    }

    @Override
    public void onSongCompleted(Song song) {
        mMessenger.sendComplete(song);
    }

    @Override
    public void onSongPaused(Song song) {
        mMessenger.sendPause(song);
    }

    @Override
    public void onSongResumed(Song song) {
        mMessenger.sendResume(song);
    }
}
