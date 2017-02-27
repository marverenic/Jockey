package com.marverenic.music.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;

import com.android.databinding.library.baseAdapters.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.player.PlayerController;

import javax.inject.Inject;

import timber.log.Timber;

public class BaseLibraryActivityViewModel extends BaseObservable {

    private Context mContext;

    @Inject PlayerController mPlayerController;

    private final boolean mFitSystemWindows;
    private final int mExpandedHeight;
    private final ObservableInt mMiniplayerHeight;
    private final ObservableFloat mMiniplayerAlpha;
    private final ObservableFloat mNowPlayingToolbarAlpha;

    private int mBottomSheetState;

    private boolean mAnimateSlideInOut;

    public BaseLibraryActivityViewModel(BaseActivity activity, boolean fitSystemWindows) {
        mContext = activity;
        JockeyApplication.getComponent(mContext).inject(this);

        mFitSystemWindows = fitSystemWindows;
        mExpandedHeight = mContext.getResources().getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;

        mMiniplayerHeight = new ObservableInt(0);
        mMiniplayerAlpha = new ObservableFloat(1.0f);
        mNowPlayingToolbarAlpha = new ObservableFloat(0.0f);

        setPlaybackOngoing(false);

        mPlayerController.getNowPlaying()
                .compose(activity.bindToLifecycle())
                .map(nowPlaying -> nowPlaying != null)
                .subscribe(this::setPlaybackOngoing, throwable -> {
                    Timber.e(throwable, "Failed to set playback state");
                });
    }

    private void setPlaybackOngoing(boolean isPlaybackOngoing) {
        if (mAnimateSlideInOut) {
            animateTranslation(isPlaybackOngoing);
        } else {
            mMiniplayerHeight.set((isPlaybackOngoing) ? mExpandedHeight : 0);
        }
    }

    public void onActivityExitForeground() {
        mAnimateSlideInOut = false;
    }

    public void onActivityEnterForeground() {
        mAnimateSlideInOut = true;
    }

    private void animateTranslation(boolean isPlaybackOngoing) {
        int startOffset = mMiniplayerHeight.get();
        int endOffset;

        TimeInterpolator interpolator;
        if (isPlaybackOngoing) {
            endOffset = mExpandedHeight;
            interpolator = new LinearOutSlowInInterpolator();
        } else {
            endOffset = 0;
            interpolator = new FastOutLinearInInterpolator();
        }

        ObjectAnimator slideAnimation = ObjectAnimator.ofInt(
                mMiniplayerHeight, "", startOffset, endOffset);
        slideAnimation.setInterpolator(interpolator);
        slideAnimation.setDuration(225);
        slideAnimation.start();
    }

    @Bindable
    public boolean getFitSystemWindows() {
        return mFitSystemWindows;
    }

    @Bindable
    public int getToolbarMarginTop() {
        if (mFitSystemWindows) {
            return 0;
        } else {
            int statusBarHeightResId = mContext.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");

            if (statusBarHeightResId < 0) {
                return 0;
            }

            return mContext.getResources().getDimensionPixelSize(statusBarHeightResId);
        }
    }

    @Bindable
    public ObservableInt getMiniplayerHeight() {
        return mMiniplayerHeight;
    }

    @Bindable
    public ObservableFloat getMiniplayerAlpha() {
        return mMiniplayerAlpha;
    }

    @Bindable
    public ObservableFloat getToolbarAlpha() {
        return mNowPlayingToolbarAlpha;
    }

    @Bindable
    public int getMiniplayerVisibility() {
        return (mBottomSheetState == BottomSheetBehavior.STATE_EXPANDED)
                ? View.GONE
                : View.VISIBLE;
    }

    @Bindable
    public int getMainContentVisibillity() {
        return (mBottomSheetState == BottomSheetBehavior.STATE_EXPANDED)
                ? View.GONE
                : View.VISIBLE;
    }

    @Bindable
    public int getNowPlayingContentVisibility() {
        return (mBottomSheetState == BottomSheetBehavior.STATE_COLLAPSED)
                ? View.INVISIBLE
                : View.VISIBLE;
    }

    @Bindable
    public BottomSheetBehavior.BottomSheetCallback getBottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                mBottomSheetState = newState;
                notifyPropertyChanged(BR.miniplayerVisibility);
                notifyPropertyChanged(BR.mainContentVisibillity);
                notifyPropertyChanged(BR.nowPlayingContentVisibility);

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mNowPlayingToolbarAlpha.set(1.0f);
                    mMiniplayerAlpha.set(0.0f);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mMiniplayerAlpha.set(1.0f - 2 * slideOffset);
                mNowPlayingToolbarAlpha.set(2 * slideOffset - 1.0f);
            }
        };
    }

    @Bindable
    public View.OnClickListener getMiniplayerClickListener() {
        return v -> {
            View bottomSheet = (View) v.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        };
    }

}
