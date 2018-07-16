package com.marverenic.music.player.browser;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.BrowserServicePackageValidator;

import java.util.List;

import javax.inject.Inject;

import rx.Single;
import rx.Subscription;
import timber.log.Timber;

public class JockeyBrowserService extends MediaBrowserServiceCompat {

    @Inject PlayerController mPlayerController;
    @Inject MediaBrowserRoot mRoot;
    @Inject EmptyBrowserRoot mEmptyRoot;

    private PlayerController.Binding mPlayerControllerBinding;
    private BrowserServicePackageValidator mPackageValidator;
    private Subscription mMediaSessionSubscription;

    @Override
    public void onCreate() {
        Timber.i("Browser service created");
        super.onCreate();
        JockeyApplication.getComponent(this).inject(this);

        mPackageValidator = new BrowserServicePackageValidator(this);
        mPlayerControllerBinding = mPlayerController.bind();
        mMediaSessionSubscription = mPlayerController.getMediaSessionToken()
                .first()
                .subscribe(this::setSessionToken, e -> {
                    Timber.e(e, "Failed to post media session token");
                });
    }

    @Override
    public void onDestroy() {
        Timber.i("Browser service destroyed");
        super.onDestroy();
        mPlayerController.unbind(mPlayerControllerBinding);
        mMediaSessionSubscription.unsubscribe();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints) {
        if (mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            return new BrowserRoot(mRoot.getPath(), null);
        } else {
            return new BrowserRoot(mEmptyRoot.getPath(), null);
        }
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.detach();
        Single.just(parentId)
                .map(id -> {
                    if (parentId.startsWith(mRoot.getId())) {
                        return mRoot;
                    } else {
                        return mEmptyRoot;
                    }
                })
                .flatMap(directory -> directory.getContents(this, parentId))
                .subscribe(result::sendResult, throwable -> {
                    Timber.e(throwable, "Failed to load media browser children");
                    result.sendError(null);
                });
    }

}
