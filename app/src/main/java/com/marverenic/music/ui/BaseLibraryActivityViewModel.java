package com.marverenic.music.ui;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.Bindable;
import android.databinding.ObservableFloat;
import android.databinding.ObservableInt;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.ui.BaseLibraryActivityViewModel.OnBottomSheetStateChangeListener.BottomSheetState;

import java.util.NoSuchElementException;

public class BaseLibraryActivityViewModel extends BaseViewModel {

    private final boolean mFitSystemWindows;
    private final int mExpandedHeight;
    private final ObservableInt mMiniplayerHeight;
    private final ObservableFloat mMiniplayerAlpha;
    private final ObservableFloat mNowPlayingToolbarAlpha;
    private final ColorDrawable mNowPlayingBackground;

    private int mBottomSheetState;

    @Nullable
    private OnBottomSheetStateChangeListener mStateListener;

    private boolean mAnimateSlideInOut;

    public BaseLibraryActivityViewModel(Context context, boolean fitSystemWindows) {
        super(context);

        mFitSystemWindows = fitSystemWindows;
        mExpandedHeight = getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;

        mMiniplayerHeight = new ObservableInt(0);
        mMiniplayerAlpha = new ObservableFloat(1.0f);
        mNowPlayingToolbarAlpha = new ObservableFloat(0.0f);

        int backgroundColor = getColor(R.color.background);
        mNowPlayingBackground = new ColorDrawable(backgroundColor);

        setPlaybackOngoing(false);
    }

    public void setPlaybackOngoing(boolean isPlaybackOngoing) {
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

    public void setStateChangeListener(@Nullable OnBottomSheetStateChangeListener listener) {
        mStateListener = listener;
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
    public int getStatusBarHeight() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return 0;
        }

        int statusBarHeightResId = getResources().getIdentifier("status_bar_height", "dimen", "android");

        if (statusBarHeightResId < 0) {
            return 0;
        }

        return getDimensionPixelSize(statusBarHeightResId);
    }

    @Bindable
    public int getContentStatusBarHeight() {
        if (!mFitSystemWindows) {
            return 0;
        } else {
            return getStatusBarHeight();
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
    public boolean isToolbarExpanded() {
        return mBottomSheetState == BottomSheetBehavior.STATE_COLLAPSED;
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
    public Drawable getNowPlayingContentBackground() {
        if (mBottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            return null;
        } else {
            return mNowPlayingBackground;
        }
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
                notifyPropertyChanged(BR.nowPlayingContentBackground);
                notifyPropertyChanged(BR.toolbarExpanded);

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mNowPlayingToolbarAlpha.set(1.0f);
                    mMiniplayerAlpha.set(0.0f);
                }

                if (mStateListener != null) {
                    BottomSheetState state = BottomSheetState.lookupBehaviorConstant(newState);
                    mStateListener.onBottomSheetStateChange(state);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (mBottomSheetState == BottomSheetBehavior.STATE_EXPANDED
                        || mBottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
                    onStateChanged(bottomSheet, BottomSheetBehavior.STATE_DRAGGING);
                }
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

    public interface OnBottomSheetStateChangeListener {

        enum BottomSheetState {
            DRAGGING(BottomSheetBehavior.STATE_DRAGGING),
            SETTLING(BottomSheetBehavior.STATE_SETTLING),
            EXPANDED(BottomSheetBehavior.STATE_EXPANDED),
            COLLAPSED(BottomSheetBehavior.STATE_COLLAPSED),
            HIDDEN(BottomSheetBehavior.STATE_HIDDEN);

            private int mBehaviorConstant;

            BottomSheetState(int behaviorConstant) {
                mBehaviorConstant = behaviorConstant;
            }

            static BottomSheetState lookupBehaviorConstant(int behaviorConstant) {
                for (BottomSheetState state : values()) {
                    if (state.mBehaviorConstant == behaviorConstant) {
                        return state;
                    }
                }
                throw new NoSuchElementException("No state for constant " + behaviorConstant);
            }

        }

        void onBottomSheetStateChange(BottomSheetState state);

    }

}
