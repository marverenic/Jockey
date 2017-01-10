package com.marverenic.music.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.ActivityNowPlayingBinding;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.dialog.DurationPickerDialogFragment;
import com.marverenic.music.dialog.NumberPickerDialogFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.UriUtils;
import com.marverenic.music.view.TimeView;
import com.marverenic.music.viewmodel.NowPlayingArtworkViewModel;

import java.io.File;
import java.util.Collections;
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

public class NowPlayingActivity extends BaseActivity
        implements NumberPickerDialogFragment.OnNumberPickedListener,
        DurationPickerDialogFragment.OnDurationPickedListener {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";
    private static final String TAG_APPEND_PLAYLIST = "AppendPlaylistDialog";
    private static final String TAG_MULTI_REPEAT_PICKER = "MultiRepeatPickerDialog";
    private static final String TAG_SLEEP_TIMER_PICKER = "SleepTimerPickerDialog";

    private static final int DEFAULT_MULTI_REPEAT_VALUE = 3;
    public static Intent newIntent(Context context) {
        return new Intent(context, NowPlayingActivity.class);
    }

    @Inject PreferenceStore mPrefStore;
    @Inject PlayerController mPlayerController;

    private ActivityNowPlayingBinding mBinding;
    private NowPlayingArtworkViewModel mArtworkViewModel;

    private MenuItem mCreatePlaylistMenuItem;
    private MenuItem mAppendToPlaylistMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mShuffleMenuItem;

    private Subscription mSleepTimerSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_now_playing);
        mArtworkViewModel = new NowPlayingArtworkViewModel(this);
        mBinding.setArtworkViewModel(mArtworkViewModel);

        JockeyApplication.getComponent(this).inject(this);
        onNewIntent(getIntent());

        boolean landscape = getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;

        if (!landscape) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (!landscape) {
                actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar.setElevation(getResources().getDimension(R.dimen.header_elevation));
            }
            actionBar.setTitle("");
            actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp);
        }

        mPlayerController.getSleepTimerEndTime()
                .compose(bindToLifecycle())
                .subscribe(this::updateSleepTimerCounter, throwable -> {
                    Timber.e(throwable, "Failed to update sleep timer end timestamp");
                });
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Handle incoming requests to play media from other applications
        if (intent.getData() == null) return;

        // If this intent is a music intent, process it
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri songUri = intent.getData();
            String songName = UriUtils.getDisplayName(this, songUri);

            List<Song> queue = buildQueueFromFileUri(songUri);
            int position;

            if (queue == null || queue.isEmpty()) {
                queue = buildQueueFromUri(intent.getData());
                position = findStartingPositionInQueue(songUri, queue);
            } else {
                String path = UriUtils.getPathFromUri(this, songUri);
                //noinspection ConstantConditions This won't be null, because we found data from it
                Uri fileUri = Uri.fromFile(new File(path));
                position = findStartingPositionInQueue(fileUri, queue);
            }

            if (queue.isEmpty()) {
                showSnackbar(getString(R.string.message_play_error_not_found, songName));
            } else {
                startIntentQueue(queue, position);
            }
        }

        // Don't try to process this intent again
        setIntent(newIntent(this));
    }

    private List<Song> buildQueueFromFileUri(Uri fileUri) {
        // URI is not a file URI
        String path = UriUtils.getPathFromUri(this, fileUri);
        if (path == null || path.trim().isEmpty()) {
            return Collections.emptyList();
        }

        File file = new File(path);
        String mimeType = getContentResolver().getType(fileUri);
        return MediaStoreUtil.buildSongListFromFile(this, file, mimeType);
    }

    private List<Song> buildQueueFromUri(Uri uri) {
        return Collections.singletonList(Song.fromUri(this, uri));
    }

    private int findStartingPositionInQueue(Uri originalUri, List<Song> queue) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getLocation().equals(originalUri)) {
                return i;
            }
        }

        return 0;
    }

    private void startIntentQueue(List<Song> queue, int position) {
        mPlayerController.setQueue(queue, position);
        mPlayerController.play();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_now_playing, menu);

        mCreatePlaylistMenuItem = menu.findItem(R.id.menu_now_playing_save);
        mAppendToPlaylistMenuItem = menu.findItem(R.id.menu_now_playing_append);
        mShuffleMenuItem = menu.findItem(R.id.menu_now_playing_shuffle);
        mRepeatMenuItem = menu.findItem(R.id.menu_now_playing_repeat);

        updateShuffleIcon();

        mPlayerController.getQueue()
                .compose(bindToLifecycle())
                .map(this::queueContainsLocalSongs)
                .subscribe(this::updatePlaylistActionEnabled, throwable -> {
                    Timber.e(throwable, "Failed to update playlist enabled state");
                });

        mPlayerController.getMultiRepeatCount()
                .compose(bindToLifecycle())
                .subscribe(this::setRepeatIcon, throwable -> {
                    Timber.e(throwable, "Failed to update repeat icon");
                });

        return true;
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

    private void updateShuffleIcon() {
        if (mPrefStore.isShuffled()) {
            mShuffleMenuItem.getIcon().setAlpha(255);
            mShuffleMenuItem.setTitle(getResources().getString(R.string.action_disable_shuffle));
        } else {
            mShuffleMenuItem.getIcon().setAlpha(128);
            mShuffleMenuItem.setTitle(getResources().getString(R.string.action_enable_shuffle));
        }
    }

    private void setRepeatIcon(int multiRepeatCount) {
        @DrawableRes int icon;
        boolean active = true;

        if (multiRepeatCount > 1) {
            switch (multiRepeatCount) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUp();
                return true;
            case R.id.menu_now_playing_shuffle:
                toggleShuffle();
                return true;
            case R.id.menu_now_playing_repeat:
                showRepeatMenu();
                return true;
            case R.id.menu_now_playing_sleep_timer:
                mPlayerController.getSleepTimerEndTime()
                        .compose(bindToLifecycle())
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
        return super.onOptionsItemSelected(item);
    }

    private void navigateUp() {
        if (isTaskRoot()) {
            Intent libraryIntent = new Intent(this, MainActivity.class);
            startActivity(libraryIntent);
        }
        finish();
    }

    private void toggleShuffle() {
        mPrefStore.toggleShuffle();
        mPlayerController.updatePlayerPreferences(mPrefStore);

        if (mPrefStore.isShuffled()) {
            showSnackbar(R.string.confirm_enable_shuffle);
        } else {
            showSnackbar(R.string.confirm_disable_shuffle);
        }

        updateShuffleIcon();
    }

    private void showRepeatMenu() {
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.menu_now_playing_repeat), Gravity.END);
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

    @Override
    public void onNumberPicked(int chosen) {
        // Callback for when a Multi-Repeat value is chosen
        mPlayerController.setMultiRepeatCount(chosen);
        showSnackbar(getString(R.string.confirm_enable_multi_repeat, chosen));
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

    private void updateSleepTimerCounter(long endTimestamp) {
        TimeView sleepTimerCounter = (TimeView) findViewById(R.id.now_playing_sleep_timer);
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
        TimeView sleepTimerCounter = (TimeView) findViewById(R.id.now_playing_sleep_timer);

        Animation transition = AnimationUtils.loadAnimation(this, R.anim.tooltip_out_down);
        transition.setStartOffset(250);
        transition.setDuration(300);
        transition.setInterpolator(this, android.R.interpolator.accelerate_quint);

        sleepTimerCounter.startAnimation(transition);

        new Handler().postDelayed(() -> sleepTimerCounter.setVisibility(View.GONE), 550);
    }

    private void showMultiRepeatDialog() {
        mPlayerController.getMultiRepeatCount().take(1).subscribe(currentCount -> {
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

    private void saveQueueAsPlaylist() {
        mPlayerController.getQueue().take(1).subscribe(queue -> {
            new CreatePlaylistDialogFragment.Builder(getSupportFragmentManager())
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
                    new AppendPlaylistDialogFragment.Builder(this)
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
                .take(1)
                .compose(bindToLifecycle())
                .subscribe(playerState -> {
                    mPlayerController.clearQueue();

                    View snackbarContainer = findViewById(R.id.now_playing_artwork);
                    Snackbar.make(snackbarContainer, R.string.confirm_clear_queue, LENGTH_LONG)
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

    @Override
    protected void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.now_playing_artwork), message, LENGTH_SHORT).show();
    }
}
