package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import rx.Observable;
import rx.Single;

public abstract class MediaBrowserDirectory {

    private static final int NO_NAME_RESOURCE = -1;

    private final String mId;
    private final String mPath;

    @StringRes
    private final int mNameRes;
    private final String mName;

    public MediaBrowserDirectory(String id, @StringRes int name) {
        mId = id;
        mPath = id + "/";
        mNameRes = name;
        mName= null;
    }

    public MediaBrowserDirectory(String id, String name) {
        mId = id;
        mPath = id + "/";
        mNameRes = NO_NAME_RESOURCE;
        mName = name;
    }

    public String getId() {
        return mId;
    }

    private String getName(Context context) {
        if (mNameRes != NO_NAME_RESOURCE) {
            return context.getString(mNameRes);
        }
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    private boolean contains(String path) {
        return path.startsWith(mPath);
    }

    public MediaItem getDirectory(Context context) {
        return new MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(getPath())
                        .setTitle(getName(context))
                        .build(),
                MediaItem.FLAG_BROWSABLE);
    }

    public final Single<List<MediaItem>> getContents(Context context, String path) {
        if (!path.startsWith(getPath())) {
            return Single.just(Collections.emptyList());
        }
        String childPath = path.substring(getPath().length());

        return getSubdirectories().flatMap(dirs -> {
            for (MediaBrowserDirectory dir : dirs) {
                if (dir.contains(childPath)) {
                    return dir.getContents(context, childPath)
                            .map(mediaItems -> {
                                List<MediaItem> relative = new ArrayList<>();
                                for (MediaItem item : mediaItems) {
                                    relative.add(makeRelative(item, getPath()));
                                }
                                return relative;
                            });
                }
            }

            return getChildren(context);
        });
    }

    private Single<List<MediaItem>> getChildren(Context context) {
        Single<List<MediaItem>> directories = getSubdirectories().map(dirs -> {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (MediaBrowserDirectory dir : dirs) {
                mediaItems.add(dir.getDirectory(context));
            }
            return mediaItems;
        });

        return directories.zipWith(getMedia(), (dirs, media) -> {
            List<MediaItem> contents = new ArrayList<>();
            contents.addAll(dirs);
            contents.addAll(media);
            return contents;
        }).map(mediaItems -> {
            List<MediaItem> relative = new ArrayList<>();
            for (MediaItem item : mediaItems) {
                relative.add(makeRelative(item, getPath()));
            }
            return relative;
        });
    }

    public final Single<MediaList> getQueue(String path) {
        if (!path.startsWith(getPath())) {
            return Single.error(new NoSuchElementException(getPath() + " does not contain " + path));
        }
        String childPath = path.substring(getPath().length());

        return getSubdirectories().flatMap(dirs -> {
            for (MediaBrowserDirectory dir : dirs) {
                if (dir.contains(childPath)) {
                    return dir.getQueue(childPath);
                }
            }

            return getQueueForMediaItem(childPath);
        });
    }

    public final Single<MediaList> getQueueForSearch(String query) {
        return searchForMediaItem(query)
                .flatMap(mediaItemId -> {
                    if (mediaItemId != null) {
                        return getQueueForMediaItem(mediaItemId);
                    } else {
                        return getSubdirectories()
                                .flatMapObservable(Observable::from)
                                .flatMap(subdir -> subdir.getQueueForSearch(query).toObservable())
                                .filter(result -> result != null)
                                .firstOrDefault(null)
                                .toSingle();
                    }
                });
    }

    private Single<String> searchForMediaItem(String query) {
        return getMedia().map(media -> {
            for (MediaItem m : media) {
                MediaDescriptionCompat description = m.getDescription();
                if (matchesQuery(description.getTitle(), query)) {
                    return m.getMediaId();
                }

                if (matchesQuery(description.getSubtitle(), query)) {
                    return m.getMediaId();
                }
            }
            return null;
        });
    }

    private boolean matchesQuery(CharSequence field, String query) {
        return field != null && field.toString().toLowerCase().contains(query.toLowerCase());
    }

    protected abstract Single<List<MediaBrowserDirectory>> getSubdirectories();

    protected abstract Single<List<MediaItem>> getMedia();

    protected abstract Single<MediaList> getQueueForMediaItem(String id);

    private MediaItem makeRelative(MediaItem item, String idPrefix) {
        MediaDescriptionCompat mediaDescription = item.getDescription();

        return new MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(idPrefix + mediaDescription.getMediaId())
                        .setTitle(mediaDescription.getTitle())
                        .setSubtitle(mediaDescription.getSubtitle())
                        .setDescription(mediaDescription.getDescription())
                        .setIconBitmap(mediaDescription.getIconBitmap())
                        .setIconUri(mediaDescription.getIconUri())
                        .setExtras(mediaDescription.getExtras())
                        .setMediaUri(mediaDescription.getMediaUri())
                        .build(),
                item.getFlags()
        );
    }

}
