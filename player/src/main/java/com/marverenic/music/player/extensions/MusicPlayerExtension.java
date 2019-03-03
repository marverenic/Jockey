package com.marverenic.music.player.extensions;

import android.os.Bundle;

import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;

public abstract class MusicPlayerExtension {
    public void onCreateMusicPlayer(MusicPlayer musicPlayer) { }
    public void onOptionsChange(Bundle options) {}
    public void onSongStarted(MusicPlayer musicPlayer) {}
    public void onSongCompleted(MusicPlayer musicPlayer, Song completed) {}
    public void onSongPaused(MusicPlayer musicPlayer) {}
    public void onSongResumed(MusicPlayer musicPlayer) {}
    public void onSongSkipped(MusicPlayer musicPlayer, boolean byUser) {}
    public void onSeekPositionChanged(MusicPlayer musicPlayer) {}
    public void onQueueChanged(MusicPlayer musicPlayer) {}
}
