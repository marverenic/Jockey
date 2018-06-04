package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.marverenic.music.R;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Single;

abstract class AbstractSongDirectory extends MediaBrowserDirectory {

    private static final String ENTRY_SHUFFLE_ALL = "_shuffle";

    private Context mContext;

    public AbstractSongDirectory(Context context, String id, String name) {
        super(id, name);
        mContext = context;
    }

    public AbstractSongDirectory(Context context, String id, @StringRes int name) {
        super(id, name);
        mContext = context;
    }

    @Override
    protected final Single<List<MediaBrowserDirectory>> getSubdirectories() {
        return Single.just(Collections.emptyList());
    }

    @Override
    protected final Single<List<MediaBrowserCompat.MediaItem>> getMedia() {
        return getSongs().map(this::convertSongsToMediaItems).map(mediaItems -> {
            mediaItems.add(0, new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(ENTRY_SHUFFLE_ALL)
                            .setTitle(mContext.getString(R.string.action_shuffle_all))
                            .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            return mediaItems;
        });
    }

    @Override
    protected final Single<MediaList> getQueueForMediaItem(String id) {
        return getSongs().map(songs -> {
            if (id.equals(ENTRY_SHUFFLE_ALL)) {
                int startIndex = (int) (Math.random() * songs.size());
                return new MediaList(songs, startIndex, true);
            } else {
                return new MediaList(songs, getIndexForSongId(id), false);
            }
        });
    }

    abstract Single<List<Song>> getSongs();

    abstract String getIdForSong(Song song, int index);

    abstract int getIndexForSongId(String id);

    private List<MediaBrowserCompat.MediaItem> convertSongsToMediaItems(List<Song> songs) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(getIdForSong(song, i))
                            .setTitle(song.getSongName())
                            .setSubtitle(song.getArtistName())
                            .setDescription(song.getAlbumName())
                            .setMediaUri(song.getLocation())
                            .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
        }
        return mediaItems;
    }
}
