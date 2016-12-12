package com.marverenic.music.fragments;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.FragmentMiniplayerBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.viewmodel.MiniplayerViewModel;

import javax.inject.Inject;

public class MiniplayerFragment extends Fragment implements PlayerController.UpdateListener {

    private FragmentMiniplayerBinding mBinding;

    @Inject ThemeStore mThemeStore;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(getContext()).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = FragmentMiniplayerBinding.inflate(inflater, container, false);
        mBinding.setViewModel(new MiniplayerViewModel(getContext()));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ProgressBar progressBar = mBinding.miniplayerProgress;
            LayerDrawable progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();

            Drawable progress = progressBarDrawable.findDrawableByLayerId(android.R.id.progress);
            progress.setColorFilter(mThemeStore.getAccentColor(), PorterDuff.Mode.SRC_ATOP);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerController.registerUpdateListener(this);
        onUpdate();
        mBinding.getViewModel().onActivityEnterForeground();
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
