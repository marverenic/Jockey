package com.marverenic.music.ui.nowplaying;

import android.content.Context;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ObservableInt;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.marverenic.music.BR;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.browse.MusicBrowserActivity;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.library.album.contents.AlbumActivity;
import com.marverenic.music.ui.library.artist.contents.ArtistActivity;
import com.marverenic.music.utils.BindingAdapters;

import timber.log.Timber;

public class NowPlayingControllerViewModel extends BaseViewModel {

    private static final String TAG_PLAYLIST_DIALOG = "AppendPlaylistDialog";

    private FragmentManager mFragmentManager;

    private PlayerController mPlayerController;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private ThemeStore mThemeStore;

    @Nullable
    private Song mSong;
    private boolean mPlaying;
    private int mDuration;
    private boolean mUserTouchingProgressBar;
    private Animation mSeekBarThumbAnimation;

    private final ObservableInt mSeekbarPosition;
    private final ObservableInt mCurrentPositionObservable;

    public NowPlayingControllerViewModel(Context context, FragmentManager fragmentManager,
                                         PlayerController playerController, MusicStore musicStore,
                                         PlaylistStore playlistStore, ThemeStore themeStore) {
        super(context);
        mFragmentManager = fragmentManager;

        mPlayerController = playerController;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mThemeStore = themeStore;

        mCurrentPositionObservable = new ObservableInt();
        mSeekbarPosition = new ObservableInt();
    }

    public void setSong(@Nullable Song song) {
        mSong = song;
        notifyPropertyChanged(BR.songTitle);
        notifyPropertyChanged(BR.artistName);
        notifyPropertyChanged(BR.albumName);
        notifyPropertyChanged(BR.positionVisibility);
        notifyPropertyChanged(BR.seekbarEnabled);
    }

    public void setPlaying(boolean playing) {
        mPlaying = playing;
        notifyPropertyChanged(BR.togglePlayIcon);
    }

    public void setCurrentPosition(int position) {
        mCurrentPositionObservable.set(position);
        if (!mUserTouchingProgressBar) {
            mSeekbarPosition.set(position);
        }
    }

    public void setDuration(int duration) {
        mDuration = duration;
        notifyPropertyChanged(BR.songDuration);
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
    public String getArtistName() {
        if (mSong == null) {
            return getString(R.string.unknown_artist);
        } else {
            return mSong.getArtistName();
        }
    }

    @Bindable
    public String getAlbumName() {
        if (mSong == null) {
            return getString(R.string.unknown_album);
        } else {
            return mSong.getAlbumName();
        }
    }

    @Bindable
    public int getSongDuration() {
        return mDuration;
    }

    @Bindable
    public boolean getSeekbarEnabled() {
        return mSong != null;
    }

    @Bindable
    public Drawable getTogglePlayIcon() {
        if (mPlaying) {
            return getDrawable(R.drawable.ic_pause_36dp);
        } else {
            return getDrawable(R.drawable.ic_play_arrow_36dp);
        }
    }

    public ObservableInt getSeekBarPosition() {
        return mSeekbarPosition;
    }

    public ObservableInt getCurrentPosition() {
        return mCurrentPositionObservable;
    }

    @Bindable
    public int getPositionVisibility() {
        if (mSong == null) {
            return View.INVISIBLE;
        } else {
            return View.VISIBLE;
        }
    }

    @ColorInt
    public int getSeekBarHeadTint() {
        return mThemeStore.getAccentColor();
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
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slider_thumb_out);
        mSeekBarThumbAnimation.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);
        notifyPropertyChanged(BR.seekBarHeadAnimation);

        long duration = mSeekBarThumbAnimation.getDuration();
        new Handler().postDelayed(() -> notifyPropertyChanged(BR.seekBarHeadVisibility), duration);
    }

    private void animateSeekBarHeadIn() {
        mSeekBarThumbAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.slider_thumb_in);
        mSeekBarThumbAnimation.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);
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

            PopupMenu menu = new PopupMenu(getContext(), v, Gravity.END);
            menu.inflate(mSong.isInLibrary()
                    ? R.menu.instance_song_now_playing
                    : R.menu.instance_song_now_playing_remote);
            menu.setOnMenuItemClickListener(onMoreInfoItemClick(mSong));
            menu.show();
        };
    }

    private PopupMenu.OnMenuItemClickListener onMoreInfoItemClick(Song song) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_navigate_to_artist:
                    mMusicStore.findArtistById(song.getArtistId()).take(1).subscribe(
                            artist -> {
                                startActivity(ArtistActivity.newIntent(getContext(), artist));
                            },
                            throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });

                    return true;
                case R.id.menu_item_navigate_to_album:
                    mMusicStore.findAlbumById(song.getAlbumId()).take(1).subscribe(
                            album -> {
                                startActivity(AlbumActivity.newIntent(getContext(), album));
                            },
                            throwable -> {
                                Timber.e(throwable, "Failed to find album");
                            });

                    return true;
                case R.id.menu_item_navigate_to_folder:
                    getContext().startActivity(MusicBrowserActivity.newIntent(getContext(), song));
                    return true;
                case R.id.menu_item_add_to_playlist:
                    new AppendPlaylistDialogFragment.Builder(getContext(), mFragmentManager)
                            .setSongs(song)
                            .showSnackbarIn(R.id.now_playing_artwork)
                            .show(TAG_PLAYLIST_DIALOG, mPlaylistStore);
                    return true;
            }
            return false;
        };
    }

    public View.OnClickListener onSkipNextClick() {
        return v -> mPlayerController.skip();
    }

    public View.OnClickListener onSkipBackClick() {
        return v -> mPlayerController.previous();
    }

    public View.OnClickListener onTogglePlayClick() {
        return v -> mPlayerController.togglePlay();
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
                animateSeekBarHeadIn();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserTouchingProgressBar = false;
                animateSeekBarHeadOut();

                mPlayerController.seek(seekBar.getProgress());
                mCurrentPositionObservable.set(seekBar.getProgress());
            }
        };
    }

    @BindingAdapter("onSeekListener")
    public static void bindOnSeekListener(SeekBar seekBar, OnSeekBarChangeListener listener) {
        seekBar.setOnSeekBarChangeListener(listener);
    }

    @BindingAdapter("marginLeft_percent")
    public static void bindPercentMarginLeft(View view, float percent) {
        View parent = (View) view.getParent();

        int leftOffset = (int) (parent.getWidth() * percent) - view.getWidth() / 2;

        leftOffset = Math.min(leftOffset, parent.getWidth() - view.getWidth());
        leftOffset = Math.max(leftOffset, 0);

        BindingAdapters.bindLeftMargin(view, leftOffset);
    }

}
