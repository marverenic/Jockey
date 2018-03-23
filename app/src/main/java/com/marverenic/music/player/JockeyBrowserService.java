package com.marverenic.music.player;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.BrowserServicePackageValidator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import timber.log.Timber;

public class JockeyBrowserService extends MediaBrowserServiceCompat {

    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__";
    public static final String MEDIA_ID_ROOT = "__ROOT__";

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    private BrowserServicePackageValidator mPackageValidator;

    private Subscription mMediaSessionSubscription;

    @Override
    public void onCreate() {
        super.onCreate();
        JockeyApplication.getComponent(this).inject(this);

        mPackageValidator = new BrowserServicePackageValidator(this);
        mMediaSessionSubscription = mPlayerController.getMediaSessionToken()
                .subscribe(this::setSessionToken, e -> {
                    Timber.e(e, "Failed to post media session token");
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaSessionSubscription != null) {
            mMediaSessionSubscription.unsubscribe();
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints) {
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            return new BrowserRoot(MEDIA_ID_EMPTY_ROOT, null);
        }
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (MEDIA_ID_EMPTY_ROOT.equals(parentId)) {
            result.sendResult(new ArrayList<>());
        } else {
            result.detach();
            mMusicStore.getSongs()
                    .first()
                    .map(this::convertToMediaItem)
                    .subscribe(result::sendResult, throwable -> {
                        Timber.e(throwable, "Failed to load media browser children");
                        result.sendError(null);
                    });
        }
    }

    private List<MediaBrowserCompat.MediaItem> convertToMediaItem(List<Song> songs) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs) {
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(Long.toString(song.getSongId()))
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
