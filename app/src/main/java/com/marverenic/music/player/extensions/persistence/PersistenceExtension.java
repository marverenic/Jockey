package com.marverenic.music.player.extensions.persistence;

import android.content.Context;
import android.net.Uri;

import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerOptions;
import com.marverenic.music.player.PlayerState;
import com.marverenic.music.player.extensions.MusicPlayerExtension;
import com.marverenic.music.player.persistence.PlaybackPersistenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersistenceExtension extends MusicPlayerExtension {

    private PlaybackPersistenceManager mPlaybackPersistenceManager;
    private Context mContext;

    public PersistenceExtension(PlaybackPersistenceManager playbackPersistenceManager, Context context) {
        mPlaybackPersistenceManager = playbackPersistenceManager;
        mContext = context;
    }

    @Override
    public void onCreateMusicPlayer(MusicPlayer musicPlayer) {
        PlaybackPersistenceManager.State state = mPlaybackPersistenceManager.getStateBlocking();

        musicPlayer.restorePlayerState(new PlayerState.Builder()
                .setPlaying(false)
                .setQueue(MediaStoreUtil.buildSongListFromUris(state.getQueue(), mContext))
                .setShuffledQueue(MediaStoreUtil.buildSongListFromUris(state.getShuffledQueue(), mContext))
                .setQueuePosition(state.getQueuePosition())
                .setSeekPosition((int) state.getSeekPosition())
                .build());
    }

    @Override
    public void onQueueChanged(MusicPlayer musicPlayer) {
        mPlaybackPersistenceManager.setState(convertPersistableState(musicPlayer.getState()));
    }

    @Override
    public void onSongStarted(MusicPlayer musicPlayer) {
        updatePosition(musicPlayer);
    }

    @Override
    public void onSongPaused(MusicPlayer musicPlayer) {
        updatePosition(musicPlayer);
    }

    @Override
    public void onSeekPositionChanged(MusicPlayer musicPlayer) {
        updatePosition(musicPlayer);
    }

    private void updatePosition(MusicPlayer musicPlayer) {
        int seekPosition = musicPlayer.getCurrentPosition();
        int queueIndex = musicPlayer.getQueuePosition();
        mPlaybackPersistenceManager.setPosition(seekPosition, queueIndex);
    }

    private PlaybackPersistenceManager.State convertPersistableState(PlayerState state) {
        return new PlaybackPersistenceManager.State(
                state.getSeekPosition(),
                state.getQueuePosition(),
                convertSongsToUris(state.getQueue()),
                convertSongsToUris(state.getShuffledQueue())
        );
    }

    private List<Uri> convertSongsToUris(List<Song> songs) {
        if (songs == null) {
            return Collections.emptyList();
        }

        List<Uri> uris = new ArrayList<>(songs.size());
        for (Song song : songs) {
            uris.add(song.getLocation());
        }
        return uris;
    }

}
