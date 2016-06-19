package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.view.ViewUtils;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class MiniplayerViewModel extends BaseObservable {

    private static final String TAG = "MiniplayerViewModel";

    private Context mContext;

    @Nullable
    private Song mSong;
    private boolean mPlaying;

    private final ObservableInt mProgress;
    private Subscription mPositionSubscription;

    public MiniplayerViewModel(Context context) {
        mContext = context;
        mProgress = new ObservableInt();
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.songArtist);
        notifyPropertyChanged(BR.songDuration);
        notifyPropertyChanged(BR.artwork);
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
                            Log.e(TAG, "setPlaying: failed to update position", throwable);
                        });
    }

    private void stopPollingPosition() {
        if (mPositionSubscription != null) {
            mPositionSubscription.unsubscribe();
            mPositionSubscription = null;
        }
    }

    public View.OnClickListener onClickMiniplayer() {
        return v -> Navigate.to(mContext, NowPlayingActivity.class);
    }

    public View.OnClickListener onClickTogglePlay() {
        return v -> PlayerController.togglePlay();
    }

    public View.OnClickListener onClickSkip() {
        return v -> PlayerController.skip();
    }

}
