package com.marverenic.music.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.fragments.BaseFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.ViewUtils;

import javax.inject.Inject;

import timber.log.Timber;

public class MiniplayerViewModel extends BaseObservable {

    private Context mContext;

    @Inject PlayerController mPlayerController;

    @Nullable
    private Song mSong;
    private boolean mPlaying;
    private boolean mAnimateSlideInOut;

    private final int mExpandedHeight;

    private final ObservableField<Bitmap> mArtwork;
    private final ObservableInt mDuration;
    private final ObservableInt mProgress;
    private final ObservableInt mVerticalTranslation;

    public MiniplayerViewModel(BaseFragment fragment) {
        mContext = fragment.getContext();
        JockeyApplication.getComponent(mContext).inject(this);

        mArtwork = new ObservableField<>();
        mProgress = new ObservableInt();
        mDuration = new ObservableInt();
        mVerticalTranslation = new ObservableInt(0);

        mExpandedHeight = mContext.getResources().getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;

        setSong(null);

        mPlayerController.getNowPlaying()
                .compose(fragment.bindToLifecycle())
                .subscribe(this::setSong, throwable -> {
                    Timber.e(throwable, "Failed to set song");
                });

        mPlayerController.isPlaying()
                .compose(fragment.bindToLifecycle())
                .subscribe(this::setPlaying, throwable -> {
                    Timber.e(throwable, "Failed to set playing state");
                });

        mPlayerController.getCurrentPosition()
                .compose(fragment.bindToLifecycle())
                .subscribe(mProgress::set, throwable -> {
                    Timber.e(throwable, "Failed to set progress");
                });

        mPlayerController.getDuration()
                .compose(fragment.bindToLifecycle())
                .subscribe(mDuration::set, throwable -> {
                    Timber.e(throwable, "Failed to set duration");
                });

        mPlayerController.getArtwork()
                .compose(fragment.bindToLifecycle())
                .map(artwork -> {
                    if (artwork == null) {
                        return ViewUtils.drawableToBitmap(
                                ContextCompat.getDrawable(mContext, R.drawable.art_default));
                    } else {
                        return artwork;
                    }
                })
                .subscribe(mArtwork::set, throwable -> {
                    Timber.e(throwable, "Failed to set artwork");
                });
    }

    private void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.songArtist);

        if (mAnimateSlideInOut) {
            animateTranslation();
        } else {
            mVerticalTranslation.set((mSong == null) ? -mExpandedHeight : 0);
        }
    }

    private void animateTranslation() {
        int currentTranslation = mVerticalTranslation.get();
        int nextTranslation;
        TimeInterpolator interpolator;
        if (mSong == null) {
            nextTranslation = -mExpandedHeight;
            interpolator = new FastOutLinearInInterpolator();
        } else {
            nextTranslation = 0;
            interpolator = new LinearOutSlowInInterpolator();
        }

        ObjectAnimator slideAnimation = ObjectAnimator.ofInt(mVerticalTranslation, "",
                currentTranslation, nextTranslation);
        slideAnimation.setInterpolator(interpolator);
        slideAnimation.setDuration(225);
        slideAnimation.start();
    }

    private void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);
    }

    public void onActivityExitForeground() {
        mAnimateSlideInOut = false;
    }

    public void onActivityEnterForeground() {
        mAnimateSlideInOut = true;
    }

    public ObservableInt getVerticalTranslation() {
        return mVerticalTranslation;
    }

    @Bindable
    public String getSongTitle() {
        if (mSong == null) {
            return mContext.getResources().getString(R.string.nothing_playing);
        } else {
            return mSong.getSongName();
        }
    }

    @Bindable
    public String getSongArtist() {
        if (mSong == null) {
            return null;
        } else {
            return mSong.getArtistName();
        }
    }

    public ObservableInt getSongDuration() {
        return mDuration;
    }

    public ObservableField<Bitmap> getArtwork() {
        return mArtwork;
    }

    public ObservableInt getProgress() {
        return mProgress;
    }

    @Bindable
    public Drawable getTogglePlayIcon() {
        if (mPlaying) {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_pause_32dp);
        } else {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_play_arrow_32dp);
        }
    }

    public View.OnClickListener onClickMiniplayer() {
        return v -> mContext.startActivity(NowPlayingActivity.newIntent(mContext));
    }

    public View.OnClickListener onClickTogglePlay() {
        return v -> mPlayerController.togglePlay();
    }

    public View.OnClickListener onClickSkip() {
        return v -> mPlayerController.skip();
    }

}
