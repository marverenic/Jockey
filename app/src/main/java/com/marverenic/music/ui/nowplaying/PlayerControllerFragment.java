package com.marverenic.music.ui.nowplaying;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.ViewNowPlayingControlPanelBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;

import javax.inject.Inject;

import timber.log.Timber;

public class PlayerControllerFragment extends BaseFragment {

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject ThemeStore mThemeStore;

    private ViewNowPlayingControlPanelBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = ViewNowPlayingControlPanelBinding.inflate(inflater, container, false);
        NowPlayingControllerViewModel viewModel = new NowPlayingControllerViewModel(getContext(),
                getFragmentManager(), mPlayerController, mMusicStore, mPlaylistStore, mThemeStore);

        mPlayerController.getCurrentPosition()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setCurrentPosition,
                        throwable -> {
                            Timber.e(throwable, "failed to update position");
                        });

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setSong,
                        throwable -> Timber.e(throwable, "Failed to set song"));

        mPlayerController.isPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setPlaying,
                        throwable -> Timber.e(throwable, "Failed to set playing"));

        mPlayerController.getDuration()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setDuration,
                        throwable -> Timber.e(throwable, "Failed to set duration"));

        mBinding.setViewModel(viewModel);

        Drawable progress = mBinding.nowPlayingControllerScrubber.nowPlayingSeekBar.getProgressDrawable();
        if (progress instanceof StateListDrawable) {
            progress = progress.getCurrent();
        }
        if (progress instanceof LayerDrawable) {
            ((LayerDrawable) progress)
                    .findDrawableByLayerId(android.R.id.background)
                    .setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
        }

        return mBinding.getRoot();
    }

}
