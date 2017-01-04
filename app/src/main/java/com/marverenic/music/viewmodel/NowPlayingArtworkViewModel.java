package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.GestureView;

import javax.inject.Inject;

public class NowPlayingArtworkViewModel extends BaseObservable {

    @Inject
    PreferenceStore mPrefStore;

    private Context mContext;
    private Song mLastPlaying;

    public NowPlayingArtworkViewModel(Context context) {
        mContext = context;
        JockeyApplication.getComponent(context).inject(this);
    }

    public int getPortraitArtworkHeight() {
        // Only used when in portrait orientation
        int reservedHeight = (int) mContext.getResources().getDimension(R.dimen.player_frame_peek);

        // Default to a square view, so set the height equal to the width
        //noinspection SuspiciousNameCombination
        int preferredHeight = mContext.getResources().getDisplayMetrics().widthPixels;
        int maxHeight = mContext.getResources().getDisplayMetrics().heightPixels - reservedHeight;

        return Math.min(preferredHeight, maxHeight);
    }

    public void onSongChanged() {
        Song nowPlaying = PlayerController.getNowPlaying();
        if (nowPlaying != mLastPlaying) {
            notifyPropertyChanged(BR.nowPlayingArtwork);
            mLastPlaying = nowPlaying;
        }
    }

    @Bindable
    public Drawable getNowPlayingArtwork() {
        Bitmap image = PlayerController.getArtwork();
        if (image == null) {
            return ContextCompat.getDrawable(mContext, R.drawable.art_default_xl);
        } else {
            return new BitmapDrawable(mContext.getResources(), image);
        }
    }

    public boolean getGesturesEnabled() {
        return mPrefStore.enableNowPlayingGestures();
    }

    @Bindable
    public Drawable getTapIndicator() {
        return ContextCompat.getDrawable(mContext,
                (PlayerController.isPlaying())
                        ? R.drawable.ic_play_arrow_36dp
                        : R.drawable.ic_pause_36dp);
    }

    public GestureView.OnGestureListener getGestureListener() {
        return new GestureView.OnGestureListener() {
            @Override
            public void onLeftSwipe() {
                PlayerController.skip();
            }

            @Override
            public void onRightSwipe() {
                int queuePosition = PlayerController.getQueuePosition() - 1;
                if (queuePosition < 0 && mPrefStore.getRepeatMode() == MusicPlayer.REPEAT_ALL) {
                    queuePosition += PlayerController.getQueueSize();
                }

                if (queuePosition >= 0) {
                    PlayerController.changeSong(queuePosition);
                } else {
                    PlayerController.seek(0);
                }

            }

            @Override
            public void onTap() {
                PlayerController.togglePlay();
                notifyPropertyChanged(BR.tapIndicator);
            }
        };
    }

}
