package com.marverenic.music.player.browser;

import android.content.Context;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;

import java.util.List;
import java.util.NoSuchElementException;

import rx.Single;

public class AllSongsDirectory extends AbstractSongDirectory {

    private static final String ID = "PLAYLISTS";

    private Context mContext;

    public AllSongsDirectory(Context context) {
        super(context, ID, R.string.header_all_songs);
        mContext = context;
    }

    private List<Song> getSongsFromMediaStore() {
        return MediaStoreUtil.getSongs(mContext, null, null);
    }

    @Override
    Single<List<Song>> getSongs() {
        return Single.just(getSongsFromMediaStore());
    }

    @Override
    String getIdForSong(Song song, int index) {
        return Long.toString(song.getSongId());
    }

    @Override
    int getIndexForSongId(String id) {
        try {
            long songId = Long.parseLong(id);
            List<Song> songsFromMediaStore = getSongsFromMediaStore();
            for (int i = 0; i < songsFromMediaStore.size(); i++) {
                Song song = songsFromMediaStore.get(i);
                if (song.getSongId() == songId) {
                    return i;
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid id " + id, e);
        }
        throw new NoSuchElementException("No song with id " + id + " was found");
    }
}
