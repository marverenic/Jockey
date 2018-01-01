package com.marverenic.music.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;

import rx.Observable;
import timber.log.Timber;

import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public class LibraryEmptyState extends BasicEmptyState {

    private Context mContext;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;

    public LibraryEmptyState(Context context, MusicStore musicStore, PlaylistStore playlistStore) {
        mContext = context;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
    }

    public String getEmptyMessage() {
        return mContext.getString(R.string.empty);
    }

    @Override
    public final String getMessage() {
        if (MediaStoreUtil.hasPermission(mContext)) {
            return getEmptyMessage();
        } else {
            return mContext.getString(R.string.empty_no_permission);
        }
    }

    public String getEmptyMessageDetail() {
        return mContext.getString(R.string.empty_detail);
    }

    @Override
    public final String getDetail() {
        if (MediaStoreUtil.hasPermission(mContext)) {
            return getEmptyMessageDetail();
        } else {
            return mContext.getString(R.string.empty_no_permission_detail);
        }
    }

    public String getEmptyAction1Label() {
        return mContext.getString(R.string.action_refresh);
    }

    @Override
    public final String getAction1Label() {
        if (MediaStoreUtil.hasPermission(mContext)) {
            return getEmptyAction1Label();
        } else {
            return mContext.getString(R.string.action_try_again);
        }
    }

    public String getEmptyAction2Label() {
        return super.getAction2Label();
    }

    @Override
    public final String getAction2Label() {
        if (MediaStoreUtil.hasPermission(mContext)) {
            return getEmptyAction2Label();
        } else {
            return mContext.getString(R.string.action_open_settings);
        }
    }

    @Override
    public void onAction1(View button) {
        Observable<Boolean> musicStoreResult = mMusicStore.refresh();
        Observable<Boolean> playlistStoreResult = mPlaylistStore.refresh();

        Observable<Boolean> combinedResult = Observable.combineLatest(
                musicStoreResult, playlistStoreResult, (result1, result2) -> result1 && result2);

        combinedResult.take(1)
                .subscribe(successful -> {
                    if (successful) {
                        Snackbar.make(button, R.string.confirm_refresh_library, LENGTH_SHORT)
                                .show();
                    }
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh library");
                });
    }

    @Override
    public void onAction2(View button) {
        if (!MediaStoreUtil.hasPermission(mContext)) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
            intent.setData(uri);
            mContext.startActivity(intent);
        }
    }
}
