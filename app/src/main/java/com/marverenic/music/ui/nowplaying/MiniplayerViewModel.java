package com.marverenic.music.ui.nowplaying;

import android.content.Context;
import android.databinding.Bindable;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;

import timber.log.Timber;

public class MiniplayerViewModel extends BaseViewModel {

    private PlayerController mPlayerController;

    @Nullable
    private Song mSong;
    private boolean mPlaying;

    private Bitmap mArtwork;
    private int mDuration;
    private int mProgress;

    public MiniplayerViewModel(Context context, PlayerController playerController) {
        super(context);
        mPlayerController = playerController;

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
        mProgress = position;
        notifyPropertyChanged(BR.songDuration);
        notifyPropertyChanged(BR.progress);
    }

    public void setDuration(int duration) {
        mDuration = duration;
        notifyPropertyChanged(BR.songDuration);
        notifyPropertyChanged(BR.progress);
    }

    public void setArtwork(Bitmap artwork) {
        mArtwork = artwork;
        notifyPropertyChanged(BR.artwork);
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

    @Bindable
    public int getSongDuration() {
        return mDuration;
    }

    @Bindable
    public Bitmap getArtwork() {
        return mArtwork;
    }

    @Bindable
    public int getProgress() {
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
