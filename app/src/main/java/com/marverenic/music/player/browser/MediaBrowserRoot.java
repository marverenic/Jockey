package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import com.marverenic.music.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import rx.Observable;
import rx.Single;

public class MediaBrowserRoot extends MediaBrowserDirectory {

    private static final String ID = "JOCKEY";

    private final List<MediaBrowserDirectory> mFlatChildren;
    private final List<MediaBrowserDirectory> mChildren;

    @Inject
    public MediaBrowserRoot(Context context) {
        super(ID, R.string.app_name);
        mFlatChildren = Collections.unmodifiableList(Collections.singletonList(
                new PlaylistRootDirectory(context)
        ));

        mChildren = Collections.unmodifiableList(Collections.singletonList(
                new AllSongsDirectory(context)
        ));
    }

    @Override
    protected Single<List<MediaBrowserDirectory>> getSubdirectories() {
        return Observable.from(mFlatChildren)
                .flatMap(child -> child.getSubdirectories().toObservable())
                .reduce(new ArrayList<MediaBrowserDirectory>(), (aggregate, items) -> {
                    aggregate.addAll(items);
                    return aggregate;
                })
                .map(flatChildren -> {
                    List<MediaBrowserDirectory> children = new ArrayList<>(flatChildren);
                    children.addAll(mChildren);
                    return children;
                })
                .toSingle();
    }

    @Override
    protected Single<List<MediaItem>> getMedia() {
        return Observable.from(mFlatChildren)
                .flatMap(mediaBrowserDirectory -> mediaBrowserDirectory.getMedia().toObservable())
                .reduce((List<MediaItem>) new ArrayList<MediaItem>(), (aggregate, items) -> {
                    aggregate.addAll(items);
                    return aggregate;
                })
                .toSingle();
    }

    @Override
    protected Single<MediaList> getQueueForMediaItem(String id) {
        return Single.error(new NoSuchElementException(id + " does not exist in the browser root"));
    }

}
