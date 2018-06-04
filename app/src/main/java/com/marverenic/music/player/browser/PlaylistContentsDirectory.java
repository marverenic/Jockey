package com.marverenic.music.player.browser;

import android.content.Context;

import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Single;

class PlaylistContentsDirectory extends AbstractSongDirectory {

    private static final String ID_PREFIX = "playlist-";
    private static final String ENTRY_PREFIX = "#";

    private Context mContext;
    private long mPlaylistId;

    PlaylistContentsDirectory(Context context, Playlist playlist) {
        super(context, ID_PREFIX + playlist.getPlaylistId(), playlist.getPlaylistName());
        mContext = context;
        mPlaylistId = playlist.getPlaylistId();
    }

    @Override
    Single<List<Song>> getSongs() {
        return Single.just(MediaStoreUtil.getPlaylistSongs(mContext, mPlaylistId));
    }

    @Override
    String getIdForSong(Song song, int index) {
        return ENTRY_PREFIX + index;
    }

    @Override
    int getIndexForSongId(String id) {
        if (id.startsWith(ENTRY_PREFIX)) {
            try {
                return Integer.parseInt(id.substring(ENTRY_PREFIX.length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ID " + id, e);
            }
        }
        throw new IllegalArgumentException("Invalid ID " + id);
    }
}
