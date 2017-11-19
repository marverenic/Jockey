package com.marverenic.music.ui.nowplaying;

import android.content.Context;
import android.databinding.Bindable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;

public class MiniplayerViewModel extends BaseViewModel {

    private PlayerController mPlayerController;

    @Nullable
    private Song mSong;
    private boolean mPlaying;

    private final ObservableField<Bitmap> mArtwork;
    private final ObservableInt mDuration;
    private final ObservableInt mProgress;

    public MiniplayerViewModel(Context context, PlayerController playerController) {
        super(context);
        mPlayerController = playerController;

        mArtwork = new ObservableField<>();
        mProgress = new ObservableInt();
        mDuration = new ObservableInt();

        setSong(null);
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.songArtist);
    }

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);
    }

    public void setCurrentPosition(int position) {
        mProgress.set(position);
    }

    public void setDuration(int duration) {
        mDuration.set(duration);
    }

    public void setArtwork(Bitmap artwork) {
        mArtwork.set(artwork);
    }

    @Bindable
    public String getSongTitle() {
        if (mSong == null) {
            return getString(R.string.nothing_playing);
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
            return getDrawable(R.drawable.ic_pause_32dp);
        } else {
            return getDrawable(R.drawable.ic_play_arrow_32dp);
        }
    }

    public View.OnClickListener onClickTogglePlay() {
        return v -> mPlayerController.togglePlay();
    }

    public View.OnClickListener onClickSkip() {
        return v -> mPlayerController.skip();
    }

}
