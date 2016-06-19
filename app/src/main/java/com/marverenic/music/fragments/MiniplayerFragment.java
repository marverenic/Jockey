package com.marverenic.music.fragments;

import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.marverenic.music.databinding.FragmentMiniplayerBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.viewmodel.MiniplayerViewModel;

public class MiniplayerFragment extends Fragment implements PlayerController.UpdateListener {

    private FragmentMiniplayerBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = FragmentMiniplayerBinding.inflate(inflater, container, false);
        mBinding.setViewModel(new MiniplayerViewModel(getContext()));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar progress = mBinding.songProgress;
            LayerDrawable progressDrawable = (LayerDrawable) progress.getProgressDrawable();

            progressDrawable.findDrawableByLayerId(android.R.id.progress).setColorFilter(
                    Themes.getAccent(), PorterDuff.Mode.SRC_ATOP);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        onUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
        mBinding.getViewModel().onActivityExitForeground();
    }

    @Override
    public void onUpdate() {
        mBinding.getViewModel().setSong(PlayerController.getNowPlaying());
        mBinding.getViewModel().setPlaying(PlayerController.isPlaying());
    }
}
