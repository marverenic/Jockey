package com.marverenic.music.player.browser;

import android.support.v4.media.MediaBrowserCompat;

import com.marverenic.music.R;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import rx.Single;

public class EmptyBrowserRoot extends MediaBrowserDirectory {

    private static final String ID = "EMPTY";

    @Inject
    public EmptyBrowserRoot() {
        super(ID, R.string.app_name);
    }

    @Override
    protected Single<List<MediaBrowserDirectory>> getSubdirectories() {
        return Single.just(Collections.emptyList());
    }

    @Override
    protected Single<List<MediaBrowserCompat.MediaItem>> getMedia() {
        return Single.just(Collections.emptyList());
    }

    @Override
    protected Single<MediaList> getQueueForMediaItem(String id) {
        return Single.error(new NoSuchElementException("$id does not exist in the empty root"));
    }

}
