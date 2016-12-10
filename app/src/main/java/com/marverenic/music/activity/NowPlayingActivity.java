package com.marverenic.music.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.dialog.DurationPickerDialogFragment;
import com.marverenic.music.dialog.NumberPickerDialogFragment;
import com.marverenic.music.fragments.QueueFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.UriUtils;
import com.marverenic.music.view.GestureView;
import com.marverenic.music.view.TimeView;

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

public class NowPlayingActivity extends BaseActivity implements GestureView.OnGestureListener,
        NumberPickerDialogFragment.OnNumberPickedListener,
        DurationPickerDialogFragment.OnDurationPickedListener {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";
    private static final String TAG_APPEND_PLAYLIST = "AppendPlaylistDialog";
    private static final String TAG_MULTI_REPEAT_PICKER = "MultiRepeatPickerDialog";
    private static final String TAG_SLEEP_TIMER_PICKER = "SleepTimerPickerDialog";

    private static final int DEFAULT_MULTI_REPEAT_VALUE = 3;

    public static Intent newIntent(Context context) {
        return new Intent(context, NowPlayingActivity.class);
    }

    @Inject PreferencesStore mPrefStore;

    private ImageView artwork;
    private GestureView artworkWrapper;
    private Song lastPlaying;
    private QueueFragment queueFragment;

    private MenuItem mCreatePlaylistMenuItem;
    private MenuItem mAppendToPlaylistMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mShuffleMenuItem;

    private Subscription mSleepTimerSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        onNewIntent(getIntent());

        JockeyApplication.getComponent(this).inject(this);

        boolean landscape = getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;

        if (!landscape) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
            findViewById(R.id.artworkSwipeFrame).getLayoutParams().height = getArtworkHeight();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }

        artwork = (ImageView) findViewById(R.id.imageArtwork);
        queueFragment =
                (QueueFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);

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

        artworkWrapper = (GestureView) findViewById(R.id.artworkSwipeFrame);
        if (artworkWrapper != null) {
            artworkWrapper.setGestureListener(this);
            artworkWrapper.setGesturesEnabled(mPrefStore.enableNowPlayingGestures());
        }

        onUpdate();
    }

    private int getArtworkHeight() {
        int reservedHeight = (int) getResources().getDimension(R.dimen.player_frame_peek);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Default to a square view, so set the height equal to the width
        //noinspection SuspiciousNameCombination
        int preferredHeight = metrics.widthPixels;
        int maxHeight = metrics.heightPixels - reservedHeight;

        return Math.min(preferredHeight, maxHeight);
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
        if (PlayerController.isServiceStarted()) {
            PlayerController.setQueue(queue, position);
            PlayerController.play();
        } else {
            PlayerController.startService(getApplicationContext());
            PlayerController.registerServiceStartListener(() -> {
                PlayerController.setQueue(queue, position);
                PlayerController.play();
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_now_playing, menu);

        mCreatePlaylistMenuItem = menu.findItem(R.id.save);
        mAppendToPlaylistMenuItem = menu.findItem(R.id.add_to_playlist);
        mShuffleMenuItem = menu.findItem(R.id.action_shuffle);
        mRepeatMenuItem = menu.findItem(R.id.action_repeat);

        updateShuffleIcon();
        updateRepeatIcon();
        updatePlaylistActionEnabled();

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSleepTimerCounter();
    }

    private void updatePlaylistActionEnabled() {
        boolean canCreatePlaylist = queueContainsLocalSongs();
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

    private void updateRepeatIcon() {
        @DrawableRes int icon;
        boolean active = true;

        int multiRepeatCount = PlayerController.getMultiRepeatCount();
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
            case R.id.action_shuffle:
                toggleShuffle();
                return true;
            case R.id.action_repeat:
                showRepeatMenu();
                return true;
            case R.id.action_set_sleep_timer:
                showSleepTimerDialog();
                return true;
            case R.id.save:
                saveQueueAsPlaylist();
                return true;
            case R.id.add_to_playlist:
                addQueueToPlaylist();
                return true;
            case R.id.clear_queue:
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
        PlayerController.updatePlayerPreferences(mPrefStore);

        if (mPrefStore.isShuffled()) {
            showSnackbar(R.string.confirm_enable_shuffle);
        } else {
            showSnackbar(R.string.confirm_disable_shuffle);
        }

        updateShuffleIcon();
        queueFragment.updateShuffle();
    }

    private void showRepeatMenu() {
        PopupMenu menu = new PopupMenu(this, findViewById(R.id.action_repeat), Gravity.END);
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
        PlayerController.setMultiRepeatCount(chosen);
        updateRepeatIcon();
        showSnackbar(getString(R.string.confirm_enable_multi_repeat, chosen));
    }

    @Override
    public void onDurationPicked(int durationInMinutes) {
        // Callback for when a sleep timer value is chosen
        if (durationInMinutes == DurationPickerDialogFragment.NO_VALUE) {
            PlayerController.disableSleepTimer();
            updateSleepTimerCounter();
            showSnackbar(R.string.confirm_disable_sleep_timer);
            return;
        }

        long durationInMillis = TimeUnit.MILLISECONDS.convert(durationInMinutes, TimeUnit.MINUTES);
        long endTimestamp = System.currentTimeMillis() + durationInMillis;
        PlayerController.setSleepTimerEndTime(endTimestamp);

        String confirmationMessage = getResources().getQuantityString(
                R.plurals.confirm_enable_sleep_timer, durationInMinutes, durationInMinutes);
        showSnackbar(confirmationMessage);

        updateSleepTimerCounter();
        mPrefStore.setLastSleepTimerDuration(durationInMillis);
    }

    private void changeRepeatMode(int repeatMode, @StringRes int confirmationMessage) {
        mPrefStore.setRepeatMode(repeatMode);
        PlayerController.setMultiRepeatCount(0);
        PlayerController.updatePlayerPreferences(mPrefStore);
        updateRepeatIcon();
        showSnackbar(confirmationMessage);
    }

    private void showSleepTimerDialog() {
        long timeLeftInMs = PlayerController.getSleepTimerEndTime() - System.currentTimeMillis();
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

    private void updateSleepTimerCounter() {
        TimeView sleepTimerCounter = (TimeView) findViewById(R.id.now_playing_sleep_timer);
        long sleepTimerValue = PlayerController.getSleepTimerEndTime() - System.currentTimeMillis();

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
        int currentValue = PlayerController.getMultiRepeatCount();

        new NumberPickerDialogFragment.Builder(this)
                .setMinValue(2)
                .setMaxValue(10)
                .setDefaultValue((currentValue > 1) ? currentValue : DEFAULT_MULTI_REPEAT_VALUE)
                .setWrapSelectorWheel(false)
                .setTitle(getString(R.string.enable_multi_repeat_title))
                .setMessage(getString(R.string.multi_repeat_description))
                .show(TAG_MULTI_REPEAT_PICKER);
    }

    private void saveQueueAsPlaylist() {
        new CreatePlaylistDialogFragment.Builder(getSupportFragmentManager())
                .setSongs(PlayerController.getQueue())
                .showSnackbarIn(R.id.imageArtwork)
                .show(TAG_MAKE_PLAYLIST);
    }

    private void addQueueToPlaylist() {
        new AppendPlaylistDialogFragment.Builder(this)
                .setTitle(getString(R.string.header_add_queue_to_playlist))
                .setSongs(PlayerController.getQueue())
                .showSnackbarIn(R.id.imageArtwork)
                .show(TAG_APPEND_PLAYLIST);
    }

    private void clearQueue() {
        List<Song> previousQueue = PlayerController.getQueue();
        int previousQueueIndex = PlayerController.getQueuePosition();

        int previousSeekPosition = PlayerController.getCurrentPosition();
        boolean wasPlaying = PlayerController.isPlaying();

        PlayerController.clearQueue();

        Snackbar.make(findViewById(R.id.imageArtwork), R.string.confirm_clear_queue, LENGTH_LONG)
                .setAction(R.string.action_undo, view -> {
                    PlayerController.editQueue(previousQueue, previousQueueIndex);
                    PlayerController.seek(previousSeekPosition);

                    if (wasPlaying) {
                        PlayerController.play();
                    }
                })
                .show();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        final Song nowPlaying = PlayerController.getNowPlaying();
        if (lastPlaying == null || !lastPlaying.equals(nowPlaying)) {
            Bitmap image = PlayerController.getArtwork();
            if (image == null) {
                artwork.setImageResource(R.drawable.art_default_xl);
            } else {
                artwork.setImageBitmap(image);
            }

            lastPlaying = nowPlaying;
        }

        if (mRepeatMenuItem != null) {
            updateRepeatIcon();
        }

        if (mCreatePlaylistMenuItem != null && mAppendToPlaylistMenuItem != null) {
            updatePlaylistActionEnabled();
        }
    }

    private boolean queueContainsLocalSongs() {
        for (Song song : PlayerController.getQueue()) {
            if (song.isInLibrary()) {
                return true;
            }
        }
        return false;
    }

    private void showSnackbar(@StringRes int stringId) {
        showSnackbar(getString(stringId));
    }

    @Override
    protected void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.imageArtwork), message, LENGTH_SHORT).show();
    }

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

        //noinspection deprecation
        artworkWrapper.setTapIndicator(getResources().getDrawable(
                (PlayerController.isPlaying())
                        ? R.drawable.ic_play_arrow_36dp
                        : R.drawable.ic_pause_36dp));
    }
}
