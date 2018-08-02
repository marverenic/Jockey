package com.marverenic.music.ui;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.databinding.ActivityLibraryBaseWrapperBinding;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivityViewModel.OnBottomSheetStateChangeListener.BottomSheetState;
import com.marverenic.music.ui.nowplaying.MiniplayerFragment;
import com.marverenic.music.ui.nowplaying.NowPlayingFragment;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class BaseLibraryActivity extends SingleFragmentActivity {

    private static final String KEY_WAS_NOW_PLAYING_EXPANDED = "NowPlayingPageExpanded";

    @Inject PlayerController _mPlayerController;

    private ActivityLibraryBaseWrapperBinding mBinding;
    private BaseLibraryActivityViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        JockeyApplication.getComponent(this).injectBaseLibraryActivity(this);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.miniplayer_container, MiniplayerFragment.newInstance())
                    .commit();

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.now_playing_container, NowPlayingFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onCreateLayout(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_library_base_wrapper);
        mViewModel = new BaseLibraryActivityViewModel(this, !isToolbarCollapsing());
        mBinding.setViewModel(mViewModel);

        _mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .map(nowPlaying -> nowPlaying != null)
                .subscribe(mViewModel::setPlaybackOngoing, throwable -> {
                    Timber.e(throwable, "Failed to set playback state");
                });

        mViewModel.setStateChangeListener(this::onBottomSheetStateChange);

        if (savedInstanceState != null) {
            boolean expanded = savedInstanceState.getBoolean(KEY_WAS_NOW_PLAYING_EXPANDED, false);
            if (expanded) expandBottomSheet();
        }
    }

    @Override
    protected int getFragmentContainerId() {
        return R.id.library_base_wrapper_container;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(mBinding.miniplayerHolder);
        boolean expanded = bottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED;
        outState.putBoolean(KEY_WAS_NOW_PLAYING_EXPANDED, expanded);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViewModel.onActivityExitForeground();
        mBinding.executePendingBindings();
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.onActivityEnterForeground();
        mBinding.executePendingBindings();
    }

    @Override
    public void onBackPressed() {
        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(mBinding.miniplayerHolder);
        if (bottomSheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void showSnackbar(String message) {
        if (mBinding.libraryBaseWrapperContainer.getVisibility() == View.VISIBLE) {
            super.showSnackbar(message);
        }
    }

    public boolean isToolbarCollapsing() {
        return false;
    }

    public void expandBottomSheet() {
        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(mBinding.miniplayerHolder);
        bottomSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    protected void onBottomSheetStateChange(BottomSheetState newState) {

    }

}
