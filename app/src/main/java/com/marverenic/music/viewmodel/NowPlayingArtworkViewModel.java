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
import com.marverenic.music.player.OldPlayerController;
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
        Song nowPlaying = OldPlayerController.getNowPlaying();
        if (mLastPlaying == null || !mLastPlaying.equals(nowPlaying)) {
            notifyPropertyChanged(BR.nowPlayingArtwork);
            mLastPlaying = nowPlaying;
        }
    }

    @Bindable
    public Drawable getNowPlayingArtwork() {
        Bitmap image = OldPlayerController.getArtwork();
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
                (OldPlayerController.isPlaying())
                        ? R.drawable.ic_play_arrow_36dp
                        : R.drawable.ic_pause_36dp);
    }

    public GestureView.OnGestureListener getGestureListener() {
        return new GestureView.OnGestureListener() {
            @Override
            public void onLeftSwipe() {
                OldPlayerController.skip();
            }

            @Override
            public void onRightSwipe() {
                int queuePosition = OldPlayerController.getQueuePosition() - 1;
                if (queuePosition < 0 && mPrefStore.getRepeatMode() == MusicPlayer.REPEAT_ALL) {
                    queuePosition += OldPlayerController.getQueueSize();
                }

                if (queuePosition >= 0) {
                    OldPlayerController.changeSong(queuePosition);
                } else {
                    OldPlayerController.seek(0);
                }

            }

            @Override
            public void onTap() {
                OldPlayerController.togglePlay();
                notifyPropertyChanged(BR.tapIndicator);
            }
        };
    }

}
