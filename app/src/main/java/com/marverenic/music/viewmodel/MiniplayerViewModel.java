package com.marverenic.music.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.ViewUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MiniplayerViewModel extends BaseObservable {

    private Context mContext;

    @Nullable
    private Song mSong;
    private boolean mPlaying;
    private boolean mAnimateSlideInOut;

    private final int mExpandedHeight;

    private final ObservableInt mProgress;
    private final ObservableInt mVerticalTranslation;
    private Subscription mPositionSubscription;

    public MiniplayerViewModel(Context context) {
        mContext = context;
        mProgress = new ObservableInt();
        mVerticalTranslation = new ObservableInt(0);

        mExpandedHeight = mContext.getResources().getDimensionPixelSize(R.dimen.miniplayer_height);
        mAnimateSlideInOut = false;
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.songArtist);
        notifyPropertyChanged(BR.songDuration);
        notifyPropertyChanged(BR.artwork);

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

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);

        if (mPlaying) {
            pollPosition();
        } else {
            stopPollingPosition();
            mProgress.set(PlayerController.getCurrentPosition());
        }
    }

    public void onActivityExitForeground() {
        stopPollingPosition();
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

    @Bindable
    public int getSongDuration() {
        if (mSong == null) {
            return Integer.MAX_VALUE;
        } else {
            return (int) mSong.getSongDuration();
        }
    }

    @Bindable
    public Bitmap getArtwork() {
        Bitmap art = PlayerController.getArtwork();
        if (art == null) {
            Drawable defaultArt = ContextCompat.getDrawable(mContext, R.drawable.art_default);
            return ViewUtils.drawableToBitmap(defaultArt);
        } else {
            return art;
        }
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

    private void pollPosition() {
        if (mPositionSubscription != null) {
            return;
        }

        mPositionSubscription = Observable.interval(200, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(tick -> PlayerController.getCurrentPosition())
                .subscribe(
                        mProgress::set,
                        throwable -> {
                            Timber.e(throwable, "failed to update position");
                        });
    }

    private void stopPollingPosition() {
        if (mPositionSubscription != null) {
            mPositionSubscription.unsubscribe();
            mPositionSubscription = null;
        }
    }

    public View.OnClickListener onClickMiniplayer() {
        return v -> mContext.startActivity(NowPlayingActivity.newIntent(mContext));
    }

    public View.OnClickListener onClickTogglePlay() {
        return v -> PlayerController.togglePlay();
    }

    public View.OnClickListener onClickSkip() {
        return v -> PlayerController.skip();
    }

}
