package com.marverenic.music.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class NowPlayingControllerViewModel extends BaseObservable {

    private static final String TAG = "NowPlayingControllerViewModel";

    private Context mContext;

    @Inject MusicStore mMusicStore;

    @Nullable
    private Song mSong;
    private boolean mPlaying;
    private boolean mUserTouchingProgressBar;
    private Animation mSeekBarThumbAnimation;

    private final ObservableInt mSeekbarPosition;
    private final ObservableInt mCurrentPositionObservable;
    private Subscription mPositionSubscription;

    public NowPlayingControllerViewModel(Context context) {
        mContext = context;
        mCurrentPositionObservable = new ObservableInt();
        mSeekbarPosition = new ObservableInt();

        JockeyApplication.getComponent(mContext).inject(this);
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.artistName);
        notifyPropertyChanged(BR.artistName);
        notifyPropertyChanged(BR.songDuration);
    }

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);

        if (mPlaying) {
            pollPosition();
        } else {
            stopPollingPosition();
        }

        if (!mUserTouchingProgressBar) {
            mSeekbarPosition.set(PlayerController.getCurrentPosition());
            mCurrentPositionObservable.set(PlayerController.getCurrentPosition());
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
                        position -> {
                            mCurrentPositionObservable.set(position);
                            if (!mUserTouchingProgressBar) {
                                mSeekbarPosition.set(position);
                            }
                        },
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

    @Bindable
    public String getSongTitle() {
        if (mSong == null) {
            return mContext.getResources().getString(R.string.nothing_playing);
        } else {
            return mSong.getSongName();
        }
    }

    @Bindable
    public String getArtistName() {
        if (mSong == null) {
            return mContext.getResources().getString(R.string.unknown_artist);
        } else {
            return mSong.getArtistName();
        }
    }

    @Bindable
    public String getAlbumName() {
        if (mSong == null) {
            return mContext.getString(R.string.unknown_album);
        } else {
            return mSong.getAlbumName();
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
    public boolean getSeekbarEnabled() {
        return mSong != null;
    }

    @Bindable
    public Drawable getTogglePlayIcon() {
        if (mPlaying) {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_pause_36dp);
        } else {
            return ContextCompat.getDrawable(mContext, R.drawable.ic_play_arrow_36dp);
        }
    }

    public ObservableInt getSeekBarPosition() {
        return mSeekbarPosition;
    }

    public ObservableInt getCurrentPosition() {
        return mCurrentPositionObservable;
    }

    @ColorInt
    public int getSeekBarHeadTint() {
        return Themes.getAccent();
    }

    @Bindable
    public int getSeekBarHeadVisibility() {
        if (mUserTouchingProgressBar) {
            return View.VISIBLE;
        } else {
            return View.INVISIBLE;
        }
    }

    @Bindable
    public Animation getSeekBarHeadAnimation() {
        Animation animation = mSeekBarThumbAnimation;
        mSeekBarThumbAnimation = null;
        return animation;
    }

    private void animateSeekBarHeadOut() {
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slider_thumb_out);
        mSeekBarThumbAnimation.setInterpolator(mContext, android.R.interpolator.accelerate_quint);
        notifyPropertyChanged(BR.seekBarHeadAnimation);

        long duration = mSeekBarThumbAnimation.getDuration();
        new Handler().postDelayed(() -> notifyPropertyChanged(BR.seekBarHeadVisibility), duration);
    }

    private void animateSeekBarHeadIn() {
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slider_thumb_in);
        mSeekBarThumbAnimation.setInterpolator(mContext, android.R.interpolator.decelerate_quint);
        notifyPropertyChanged(BR.seekBarHeadAnimation);
        notifyPropertyChanged(BR.seekBarHeadVisibility);
    }

    @Bindable
    public float getSeekBarHeadMarginLeft() {
        return mSeekbarPosition.get() / (float) getSongDuration();
    }

    public View.OnClickListener onMoreInfoClick() {
        return v -> {
            if (mSong == null) {
                return;
            }

            PopupMenu menu = new PopupMenu(mContext, v, Gravity.END);
            String[] options = mContext.getResources().getStringArray(R.array.now_playing_options);

            for (int i = 0; i < options.length;  i++) {
                menu.getMenu().add(Menu.NONE, i, i, options[i]);
            }
            menu.setOnMenuItemClickListener(onMoreInfoItemClick(mSong));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMoreInfoItemClick(Song song) {
        return item -> {
            switch (item.getItemId()) {
                case 0: //Go to artist
                    mMusicStore.findArtistById(song.getArtistId()).subscribe(
                            artist -> {
                                Navigate.to(mContext, ArtistActivity.class,
                                        ArtistActivity.ARTIST_EXTRA, artist);
                            },
                            throwable -> {
                                Log.e(TAG, "Failed to find artist", throwable);
                            });

                    return true;
                case 1: //Go to album
                    mMusicStore.findAlbumById(song.getAlbumId()).subscribe(
                            album -> {
                                Navigate.to(mContext, AlbumActivity.class,
                                        AlbumActivity.ALBUM_EXTRA, album);
                            },
                            throwable -> {
                                Log.e(TAG, "Failed to find album", throwable);
                            });

                    return true;
                case 2: //Add to playlist
                    // TODO implement this using DialogFragment
                    return true;
            }
            return false;
        };
    }

    public View.OnClickListener onSkipNextClick() {
        return v -> {
            PlayerController.skip();
            setSong(PlayerController.getNowPlaying());
        };
    }

    public View.OnClickListener onSkipBackClick() {
        return v -> {
            PlayerController.previous();
            setSong(PlayerController.getNowPlaying());
        };
    }

    public View.OnClickListener onTogglePlayClick() {
        return v -> {
            PlayerController.togglePlay();
            setPlaying(PlayerController.isPlaying());
        };
    }

    public OnSeekBarChangeListener onSeek() {
        return new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekbarPosition.set(progress);
                if (fromUser) {
                    notifyPropertyChanged(BR.seekBarHeadMarginLeft);

                    if (!mUserTouchingProgressBar) {
                        // For keyboards and non-touch based things
                        onStartTrackingTouch(seekBar);
                        onStopTrackingTouch(seekBar);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserTouchingProgressBar = true;
                stopPollingPosition();
                animateSeekBarHeadIn();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserTouchingProgressBar = false;
                animateSeekBarHeadOut();

                PlayerController.seek(seekBar.getProgress());
                mCurrentPositionObservable.set(seekBar.getProgress());

                if (mPlaying) {
                    pollPosition();
                }
            }
        };
    }

    @BindingAdapter("bind:onSeekListener")
    public static void bindOnSeekListener(SeekBar seekBar, OnSeekBarChangeListener listener) {
        seekBar.setOnSeekBarChangeListener(listener);
    }

    @BindingAdapter("bind:marginLeft_percent")
    public static void bindPercentMarginLeft(View view, float percent) {
        View parent = (View) view.getParent();

        int leftOffset = (int) (parent.getWidth() * percent) - view.getWidth() / 2;

        leftOffset = Math.min(leftOffset, parent.getWidth() - view.getWidth());
        leftOffset = Math.max(leftOffset, 0);

        BindingAdapters.bindLeftMargin(view, leftOffset);
    }

}
