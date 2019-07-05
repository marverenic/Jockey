package com.marverenic.music.ui.nowplaying;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.FragmentMiniplayerBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.view.ViewUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class MiniplayerFragment extends BaseFragment {

    @Inject PlayerController mPlayerController;
    @Inject ThemeStore mThemeStore;

    public static MiniplayerFragment newInstance() {
        return new MiniplayerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(getContext()).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        FragmentMiniplayerBinding mBinding = FragmentMiniplayerBinding.inflate(inflater, container, false);
        MiniplayerViewModel viewModel = new MiniplayerViewModel(getContext(), mPlayerController);

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setSong, throwable -> {
                    Timber.e(throwable, "Failed to set song");
                });

        mPlayerController.isPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setPlaying, throwable -> {
                    Timber.e(throwable, "Failed to set playing state");
                });

        mPlayerController.getCurrentPosition()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setCurrentPosition, throwable -> {
                    Timber.e(throwable, "Failed to set progress");
                });

        mPlayerController.getDuration()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setDuration, throwable -> {
                    Timber.e(throwable, "Failed to set duration");
                });

        mPlayerController.getArtwork()
                .compose(bindToLifecycle())
                .map(artwork -> {
                    if (artwork == null) {
                        return ViewUtils.drawableToBitmap(
                                ContextCompat.getDrawable(getContext(), R.drawable.art_default));
                    } else {
                        return artwork;
                    }
                })
                .subscribe(viewModel::setArtwork, throwable -> {
                    Timber.e(throwable, "Failed to set artwork");
                });

        mBinding.setViewModel(viewModel);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = mBinding.miniplayerProgress;
            LayerDrawable progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();

            Drawable progress = progressBarDrawable.findDrawableByLayerId(android.R.id.progress);
            progress.setColorFilter(mThemeStore.getAccentColor(), PorterDuff.Mode.SRC_ATOP);
        }

        return mBinding.getRoot();
    }

}
