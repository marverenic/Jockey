package com.marverenic.music.fragments;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.marverenic.music.BR;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentNowPlayingBinding;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.dialog.DurationPickerDialogFragment;
import com.marverenic.music.dialog.NumberPickerDialogFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.player.PlayerState;
import com.marverenic.music.view.TimeView;
import com.marverenic.music.viewmodel.NowPlayingArtworkViewModel;
import com.trello.rxlifecycle.FragmentEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public class NowPlayingFragment extends BaseFragment implements Toolbar.OnMenuItemClickListener,
        NumberPickerDialogFragment.OnNumberPickedListener,
        DurationPickerDialogFragment.OnDurationPickedListener {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";
    private static final String TAG_APPEND_PLAYLIST = "AppendPlaylistDialog";
    private static final String TAG_MULTI_REPEAT_PICKER = "MultiRepeatPickerDialog";
    private static final String TAG_SLEEP_TIMER_PICKER = "SleepTimerPickerDialog";

    private static final int DEFAULT_MULTI_REPEAT_VALUE = 3;

    @Inject PreferenceStore mPrefStore;
    @Inject PlayerController mPlayerController;

    private FragmentNowPlayingBinding mBinding;
    private NowPlayingArtworkViewModel mArtworkViewModel;

    private MenuItem mCreatePlaylistMenuItem;
    private MenuItem mAppendToPlaylistMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mShuffleMenuItem;

    private Subscription mSleepTimerSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JockeyApplication.getComponent(this).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_now_playing,
                container, false);

        mArtworkViewModel = new NowPlayingArtworkViewModel(this);
        mBinding.setArtworkViewModel(mArtworkViewModel);

        setupToolbar(mBinding.nowPlayingToolbar);

        mPlayerController.getSleepTimerEndTime()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(this::updateSleepTimerCounter, throwable -> {
                    Timber.e(throwable, "Failed to update sleep timer end timestamp");
                });

        mPlayerController.getInfo()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(this::showSnackbar, throwable -> {
                    Timber.e(throwable, "Failed to display info message");
                });

        mPlayerController.getError()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(this::showSnackbar, throwable -> {
                    Timber.e(throwable, "Failed to display error message");
                });

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        mArtworkViewModel.notifyPropertyChanged(BR.gesturesEnabled);
        super.onResume();
    }

    private void setupToolbar(Toolbar toolbar) {
        if (getResources().getConfiguration().orientation != ORIENTATION_LANDSCAPE) {
            toolbar.setBackground(new ColorDrawable(Color.TRANSPARENT));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(getResources().getDimension(R.dimen.header_elevation));
        }

        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_clear_24dp);

        toolbar.inflateMenu(R.menu.activity_now_playing);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationOnClickListener(v -> {
            getActivity().onBackPressed();
        });

        mCreatePlaylistMenuItem = toolbar.getMenu().findItem(R.id.menu_now_playing_save);
        mAppendToPlaylistMenuItem = toolbar.getMenu().findItem(R.id.menu_now_playing_append);
        mShuffleMenuItem = toolbar.getMenu().findItem(R.id.menu_now_playing_shuffle);
        mRepeatMenuItem = toolbar.getMenu().findItem(R.id.menu_now_playing_repeat);

        mPlayerController.getQueue()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .map(this::queueContainsLocalSongs)
                .subscribe(this::updatePlaylistActionEnabled, throwable -> {
                    Timber.e(throwable, "Failed to update playlist enabled state");
                });

        mPlayerController.isShuffleEnabled()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(this::updateShuffleIcon, throwable -> {
                    Timber.e(throwable, "Failed to update shuffle icon");
                });

        mPlayerController.getRepeatMode()
                .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(this::updateRepeatIcon, throwable -> {
                    Timber.e(throwable, "Failed to update repeat icon");
                });
    }

    private boolean queueContainsLocalSongs(List<Song> queue) {
        for (Song song : queue) {
            if (song.isInLibrary()) {
                return true;
            }
        }
        return false;
    }

    private void updatePlaylistActionEnabled(boolean canCreatePlaylist) {
        mCreatePlaylistMenuItem.setEnabled(canCreatePlaylist);
        mAppendToPlaylistMenuItem.setEnabled(canCreatePlaylist);
    }

    private void updateShuffleIcon(boolean shuffled) {
        if (shuffled) {
            mShuffleMenuItem.getIcon().setAlpha(255);
            mShuffleMenuItem.setTitle(getResources().getString(R.string.action_disable_shuffle));
        } else {
            mShuffleMenuItem.getIcon().setAlpha(128);
            mShuffleMenuItem.setTitle(getResources().getString(R.string.action_enable_shuffle));
        }
    }

    private void updateRepeatIcon(int repeatMode) {
        @DrawableRes int icon;
        boolean active = true;

        if (repeatMode > 1) {
            switch (repeatMode) {
                case 2:
                    icon = R.drawable.ic_repeat_two_24dp;
                    break;
                case 3:
                    icon = R.drawable.ic_repeat_three_24dp;
                    break;
                case 4:
                    icon = R.drawable.ic_repeat_four_24dp;
                    break;
                case 5:
                    icon = R.drawable.ic_repeat_five_24dp;
                    break;
                case 6:
                    icon = R.drawable.ic_repeat_six_24dp;
                    break;
                case 7:
                    icon = R.drawable.ic_repeat_seven_24dp;
                    break;
                case 8:
                    icon = R.drawable.ic_repeat_eight_24dp;
                    break;
                case 9:
                default:
                    icon = R.drawable.ic_repeat_nine_24dp;
                    break;
            }
        } else if (mPrefStore.getRepeatMode() == MusicPlayer.REPEAT_ALL) {
            icon = R.drawable.ic_repeat_24dp;
        } else if (mPrefStore.getRepeatMode() == MusicPlayer.REPEAT_ONE) {
            icon = R.drawable.ic_repeat_one_24dp;
        } else {
            icon = R.drawable.ic_repeat_24dp;
            active = false;
        }

        mRepeatMenuItem.setIcon(icon);
        mRepeatMenuItem.getIcon().setAlpha(active ? 255 : 128);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_now_playing_shuffle:
                toggleShuffle();
                return true;
            case R.id.menu_now_playing_repeat:
                showRepeatMenu();
                return true;
            case R.id.menu_now_playing_sleep_timer:
                mPlayerController.getSleepTimerEndTime()
                        .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                        .take(1)
                        .subscribe(this::showSleepTimerDialog, throwable -> {
                            Timber.e(throwable, "Failed to show sleep timer dialog");
                        });
                return true;
            case R.id.menu_now_playing_save:
                saveQueueAsPlaylist();
                return true;
            case R.id.menu_now_playing_append:
                addQueueToPlaylist();
                return true;
            case R.id.menu_now_playing_clear:
                clearQueue();
                return true;
        }
        return false;
    }

    private void toggleShuffle() {
        mPrefStore.toggleShuffle();
        mPlayerController.updatePlayerPreferences(mPrefStore);

        if (mPrefStore.isShuffled()) {
            showSnackbar(R.string.confirm_enable_shuffle);
        } else {
            showSnackbar(R.string.confirm_disable_shuffle);
        }
    }

    private void showRepeatMenu() {
        View anchor = mBinding.getRoot().findViewById(R.id.menu_now_playing_repeat);
        PopupMenu menu = new PopupMenu(getContext(), anchor, Gravity.END);
        menu.inflate(R.menu.activity_now_playing_repeat);

        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_item_repeat_all:
                    changeRepeatMode(MusicPlayer.REPEAT_ALL, R.string.confirm_enable_repeat);
                    return true;
                case R.id.menu_item_repeat_none:
                    changeRepeatMode(MusicPlayer.REPEAT_NONE, R.string.confirm_disable_repeat);
                    return true;
                case R.id.menu_item_repeat_one:
                    changeRepeatMode(MusicPlayer.REPEAT_ONE, R.string.confirm_enable_repeat_one);
                    return true;
                case R.id.menu_item_repeat_multi:
                    showMultiRepeatDialog();
                    return true;
                default:
                    return false;
            }
        });

        menu.show();
    }

    private void changeRepeatMode(int repeatMode, @StringRes int confirmationMessage) {
        mPrefStore.setRepeatMode(repeatMode);
        mPlayerController.setMultiRepeatCount(0);
        mPlayerController.updatePlayerPreferences(mPrefStore);
        showSnackbar(confirmationMessage);
    }

    private void showSleepTimerDialog(long currentSleepTimerEndTime) {
        long timeLeftInMs = currentSleepTimerEndTime - System.currentTimeMillis();
        int defaultValue;

        if (timeLeftInMs > 0) {
            long minutes = TimeUnit.MINUTES.convert(timeLeftInMs, TimeUnit.MILLISECONDS);
            long seconds = TimeUnit.SECONDS.convert(timeLeftInMs, TimeUnit.MILLISECONDS) % 60;

            defaultValue = (int) minutes + ((seconds >= 30) ? 1 : 0);
            defaultValue = Math.max(defaultValue, 1);
        } else {
            long prevTimeInMillis = mPrefStore.getLastSleepTimerDuration();
            defaultValue = (int) TimeUnit.MINUTES.convert(prevTimeInMillis, TimeUnit.MILLISECONDS);
        }

        new DurationPickerDialogFragment.Builder(this)
                .setMinValue(1)
                .setDefaultValue(defaultValue)
                .setMaxValue(120)
                .setTitle(getString(R.string.enable_sleep_timer))
                .setDisableButtonText((timeLeftInMs > 0)
                        ? getString(R.string.action_disable_sleep_timer)
                        : null)
                .show(TAG_SLEEP_TIMER_PICKER);
    }

    @Override
    public void onDurationPicked(int durationInMinutes) {
        // Callback for when a sleep timer value is chosen
        if (durationInMinutes == DurationPickerDialogFragment.NO_VALUE) {
            mPlayerController.disableSleepTimer();
            showSnackbar(R.string.confirm_disable_sleep_timer);
            return;
        }

        long durationInMillis = TimeUnit.MILLISECONDS.convert(durationInMinutes, TimeUnit.MINUTES);
        long endTimestamp = System.currentTimeMillis() + durationInMillis;
        mPlayerController.setSleepTimerEndTime(endTimestamp);

        String confirmationMessage = getResources().getQuantityString(
                R.plurals.confirm_enable_sleep_timer, durationInMinutes, durationInMinutes);
        showSnackbar(confirmationMessage);

        mPrefStore.setLastSleepTimerDuration(durationInMillis);
    }

    private void updateSleepTimerCounter(long endTimestamp) {
        TimeView sleepTimerCounter = mBinding.nowPlayingSleepTimer;
        long sleepTimerValue = endTimestamp - System.currentTimeMillis();

        if (mSleepTimerSubscription != null) {
            mSleepTimerSubscription.unsubscribe();
        }

        if (sleepTimerValue <= 0) {
            sleepTimerCounter.setVisibility(View.GONE);
        } else {
            sleepTimerCounter.setVisibility(View.VISIBLE);
            sleepTimerCounter.setTime((int) sleepTimerValue);

            mSleepTimerSubscription = Observable.interval(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.computation())
                    .map(tick -> (int) (sleepTimerValue - 500 * tick))
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                    .subscribe(time -> {
                        sleepTimerCounter.setTime(time);
                        if (time <= 0) {
                            mSleepTimerSubscription.unsubscribe();
                            animateOutSleepTimerCounter();
                        }
                    }, throwable -> {
                        Timber.e(throwable, "Failed to update sleep timer value");
                    });
        }
    }

    private void animateOutSleepTimerCounter() {
        TimeView sleepTimerCounter = mBinding.nowPlayingSleepTimer;

        Animation transition = AnimationUtils.loadAnimation(getContext(), R.anim.tooltip_out_down);
        transition.setStartOffset(250);
        transition.setDuration(300);
        transition.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);

        sleepTimerCounter.startAnimation(transition);

        new Handler().postDelayed(() -> sleepTimerCounter.setVisibility(View.GONE), 550);
    }

    private void showMultiRepeatDialog() {
        mPlayerController.getRepeatMode().take(1).subscribe(currentCount -> {
            new NumberPickerDialogFragment.Builder(this)
                    .setMinValue(2)
                    .setMaxValue(10)
                    .setDefaultValue((currentCount > 1)
                            ? currentCount
                            : DEFAULT_MULTI_REPEAT_VALUE)
                    .setWrapSelectorWheel(false)
                    .setTitle(getString(R.string.enable_multi_repeat_title))
                    .setMessage(getString(R.string.multi_repeat_description))
                    .show(TAG_MULTI_REPEAT_PICKER);
        }, throwable -> {
            Timber.e(throwable, "Failed to show multi repeat dialog");
        });
    }

    @Override
    public void onNumberPicked(int chosen) {
        // Callback for when a Multi-Repeat value is chosen
        mPlayerController.setMultiRepeatCount(chosen);
        showSnackbar(getString(R.string.confirm_enable_multi_repeat, chosen));
    }

    private void saveQueueAsPlaylist() {
        mPlayerController.getQueue().take(1).subscribe(queue -> {
            new CreatePlaylistDialogFragment.Builder(getFragmentManager())
                    .setSongs(queue)
                    .showSnackbarIn(R.id.now_playing_artwork)
                    .show(TAG_MAKE_PLAYLIST);
        }, throwable -> {
            Timber.e(throwable, "Failed to save queue as playlist");
        });

    }

    private void addQueueToPlaylist() {
        mPlayerController.getQueue()
                .compose(bindToLifecycle())
                .take(1)
                .subscribe(queue -> {
                    new AppendPlaylistDialogFragment.Builder(getContext(), getFragmentManager())
                            .setTitle(getString(R.string.header_add_queue_to_playlist))
                            .setSongs(queue)
                            .showSnackbarIn(R.id.now_playing_artwork)
                            .show(TAG_APPEND_PLAYLIST);
                }, throwable -> {
                    Timber.e(throwable, "Failed to add queue to playlist");
                });
    }

    private void clearQueue() {
        mPlayerController.getPlayerState()
                .compose(this.<PlayerState>bindToLifecycle().forSingle())
                .subscribe(playerState -> {
                    mPlayerController.clearQueue();

                    Snackbar.make(mBinding.getRoot(), R.string.confirm_clear_queue, LENGTH_LONG)
                            .setAction(R.string.action_undo, view -> {
                                mPlayerController.restorePlayerState(playerState);
                            })
                            .show();
                }, throwable -> {
                    Timber.e(throwable, "Failed to clear queue");
                });
    }

    private void showSnackbar(@StringRes int stringId) {
        showSnackbar(getString(stringId));
    }

    private void showSnackbar(String message) {
        if (((View) mBinding.getRoot().getParent()).getVisibility() == View.VISIBLE) {
            Snackbar.make(mBinding.getRoot(), message, LENGTH_SHORT).show();
        }
    }
}
