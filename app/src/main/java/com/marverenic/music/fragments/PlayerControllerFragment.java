package com.marverenic.music.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.databinding.ViewNowPlayingControlPanelBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.viewmodel.NowPlayingControllerViewModel;

public class PlayerControllerFragment extends Fragment implements PlayerController.UpdateListener {

    private ViewNowPlayingControlPanelBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = ViewNowPlayingControlPanelBinding.inflate(inflater, container, false);
        mBinding.setViewModel(new NowPlayingControllerViewModel(this));

        onUpdate();

        Drawable progress = mBinding.playerScrubber.songSeekBar.getProgressDrawable();
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

    @Override
    public void onPause() {
        super.onPause();
        PlayerController.unregisterUpdateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        onUpdate();
    }

    @Override
    public void onUpdate() {
        if (mBinding == null || mBinding.getViewModel() == null) {
            return;
        }

        mBinding.getViewModel().setSong(PlayerController.getNowPlaying());
        mBinding.getViewModel().setPlaying(PlayerController.isPlaying());
        mBinding.executePendingBindings();
    }

}
