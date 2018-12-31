package com.marverenic.music.player.extensions;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;

public abstract class MusicPlayerExtension {
    public void onCreateMusicPlayer(MusicPlayer musicPlayer, ReadOnlyPreferenceStore preferences) { }
    public void onSettingsChanged(ReadOnlyPreferenceStore preferences) {}
    public void onSongStarted(MusicPlayer musicPlayer) {}
    public void onSongCompleted(MusicPlayer musicPlayer, Song completed) {}
    public void onSongPaused(MusicPlayer musicPlayer) {}
    public void onSongResumed(MusicPlayer musicPlayer) {}
    public void onSeekPositionChanged(MusicPlayer musicPlayer) {}
    public void onQueueChanged(MusicPlayer musicPlayer) {}
}
