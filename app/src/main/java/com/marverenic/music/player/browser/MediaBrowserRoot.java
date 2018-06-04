package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import com.marverenic.music.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import rx.Single;

public class MediaBrowserRoot extends MediaBrowserDirectory {

    private static final String ID = "JOCKEY";

    private final List<MediaBrowserDirectory> mChildren;

    @Inject
    public MediaBrowserRoot(Context context) {
        super(ID, R.string.app_name);
        mChildren = Collections.unmodifiableList(Arrays.asList(
                new PlaylistRootDirectory(context)
        ));
    }

    @Override
    protected Single<List<MediaBrowserDirectory>> getSubdirectories() {
        return Single.just(mChildren);
    }

    @Override
    protected Single<List<MediaItem>> getMedia() {
        return Single.just(Collections.emptyList());
    }

    @Override
    protected Single<MediaList> getQueueForMediaItem(String id) {
        return Single.error(new NoSuchElementException(id + " does not exist in the browser root"));
    }

}
