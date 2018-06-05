package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import rx.Single;

public class PlaylistRootDirectory extends MediaBrowserDirectory {

    private static final String ID = "PLAYLISTS";

    private Context mContext;

    public PlaylistRootDirectory(Context context) {
        super(ID, R.string.header_playlists);
        mContext = context;
    }

    @Override
    protected Single<List<MediaBrowserDirectory>> getSubdirectories() {
        List<Playlist> playlists = MediaStoreUtil.getAllPlaylists(mContext);
        List<MediaBrowserDirectory> subdirs = new ArrayList<>();

        for (Playlist playlist : playlists) {
            subdirs.add(new PlaylistContentsDirectory(mContext, playlist));
        }

        return Single.just(subdirs);
    }

    @Override
    protected Single<List<MediaBrowserCompat.MediaItem>> getMedia() {
        return Single.just(Collections.emptyList());
    }

    @Override
    protected Single<MediaList> getQueueForMediaItem(String id) {
        return Single.error(new NoSuchElementException("playlist root does not contain item " + id));
    }

}
