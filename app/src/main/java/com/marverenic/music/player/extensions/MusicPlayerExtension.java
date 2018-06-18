package com.marverenic.music.player.extensions;

import com.marverenic.music.model.Song;

public abstract class MusicPlayerExtension {
    public void onSongStarted(Song song) {}
    public void onSongCompleted(Song song) {}
    public void onSongPaused(Song song) {}
    public void onSongResumed(Song song) {}
}
