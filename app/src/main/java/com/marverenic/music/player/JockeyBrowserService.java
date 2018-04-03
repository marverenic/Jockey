package com.marverenic.music.player;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.utils.BrowserServicePackageValidator;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import timber.log.Timber;

public class JockeyBrowserService extends MediaBrowserServiceCompat {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    private MediaBrowserHelper mBrowserHelper;
    private BrowserServicePackageValidator mPackageValidator;

    private Subscription mMediaSessionSubscription;
    private Subscription mQueueInvalidatorSubscription;

    @Override
    public void onCreate() {
        super.onCreate();
        JockeyApplication.getComponent(this).inject(this);

        mBrowserHelper = new MediaBrowserHelper(this);
        mPackageValidator = new BrowserServicePackageValidator(this);
        mMediaSessionSubscription = mPlayerController.getMediaSessionToken()
                .subscribe(this::setSessionToken, e -> {
                    Timber.e(e, "Failed to post media session token");
                });

        mQueueInvalidatorSubscription = mPlayerController.getQueue()
                .subscribe(queue -> {
                    notifyChildrenChanged(MediaBrowserHelper.MEDIA_ID_QUEUE_ROOT);
                }, e -> {
                    Timber.e(e, "Failed to notify queue change");
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaSessionSubscription != null) {
            mMediaSessionSubscription.unsubscribe();
        }

        if (mQueueInvalidatorSubscription != null) {
            mQueueInvalidatorSubscription.unsubscribe();
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints) {
        boolean allowed = mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid);
        return mBrowserHelper.getRoot(!allowed);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
        mBrowserHelper.getContents(parentId, mPlayerController, mMusicStore, mPlaylistStore)
                .subscribe(result::sendResult, throwable -> {
                    Timber.e(throwable, "Failed to load media browser children");
                    result.sendError(null);
                });
    }

}
