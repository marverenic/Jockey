package com.marverenic.music.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableInt;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.player.PlayerController;

import javax.inject.Inject;

import timber.log.Timber;

public class BaseLibraryActivityViewModel extends BaseObservable {

    private Context mContext;

    @Inject PlayerController mPlayerController;

    private final int mExpandedHeight;
    private final ObservableInt mMiniplayerHeight;

    private boolean mAnimateSlideInOut;

    public BaseLibraryActivityViewModel(BaseActivity activity) {
        mContext = activity;
        JockeyApplication.getComponent(mContext).inject(this);

        mExpandedHeight = mContext.getResources().getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;

        mMiniplayerHeight = new ObservableInt(0);

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
    public ObservableInt getMiniplayerHeight() {
        return mMiniplayerHeight;
    }

    public void onActivityExitForeground() {
        mAnimateSlideInOut = false;
    }

    public void onActivityEnterForeground() {
        mAnimateSlideInOut = true;
    }

}
