package com.marverenic.music.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.marverenic.music.R;
import com.marverenic.music.fragments.QueueFragment;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.PlaylistDialog;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Prefs;
import com.marverenic.music.view.GestureView;

import java.io.File;
import java.util.ArrayList;

public class NowPlayingActivity extends BaseActivity implements GestureView.OnGestureListener {

    private ImageView artwork;
    private GestureView artworkWrapper;
    private Song lastPlaying;
    private QueueFragment queueFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
        setContentView(R.layout.activity_now_playing);

        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        if (!landscape) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
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
            artworkWrapper.setGesturesEnabled(
                    Prefs.getPrefs(this).getBoolean(Prefs.ENABLE_NOW_PLAYING_GESTURES, true));
        }

        onUpdate();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Handle incoming requests to play media from other applications
        if (intent.getData() == null) return;

        // If this intent is a music intent, process it
        if (intent.getType().contains("audio") || intent.getType().contains("application/ogg")
                || intent.getType().contains("application/x-ogg")
                || intent.getType().contains("application/itunes")) {

            // The queue to be passed to the player service
            ArrayList<Song> queue = new ArrayList<>();
            int position = 0;

            // Have the LibraryScanner class get a song list for this file
            try {
                position = Library.getSongListFromFile(this,
                        new File(intent.getData().getPath()), intent.getType(), queue);
            } catch (Exception e) {
                e.printStackTrace();
                queue = new ArrayList<>();
            }

            if (queue.isEmpty()) {
                // No music was found
                Toast toast = Toast.makeText(this, R.string.message_play_error_not_found,
                        Toast.LENGTH_SHORT);
                toast.show();
                finish();
            } else {
                if (PlayerController.isServiceStarted()) {
                    PlayerController.setQueue(queue, position);
                    PlayerController.begin();
                } else {
                    // If the service hasn't been bound yet, then we need to wait for the service to
                    // start before we can pass data to it. This code will bind a short-lived
                    // BroadcastReceiver to wait for the initial UPDATE broadcast to be sent before
                    // sending data. Once it has fulfilled its purpose it will unbind itself to
                    // avoid a lot of problems later on.

                    final ArrayList<Song> pendingQueue = queue;
                    final int pendingPosition = position;

                    final BroadcastReceiver binderWaiter = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            PlayerController.setQueue(pendingQueue, pendingPosition);
                            PlayerController.begin();
                            NowPlayingActivity.this.unregisterReceiver(this);
                        }
                    };
                    registerReceiver(binderWaiter, new IntentFilter(MusicPlayer.UPDATE_BROADCAST));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_now_playing, menu);

        if (PlayerController.isShuffle()) {
            menu.getItem(0).setTitle(getResources().getString(R.string.action_disable_shuffle));
        } else {
            menu.getItem(0).getIcon().setAlpha(128);
            menu.getItem(0).setTitle(getResources().getString(R.string.action_enable_shuffle));
        }

        if (PlayerController.isRepeat()) {
            menu.getItem(1).setTitle(getResources().getString(R.string.action_enable_repeat_one));
        } else {
            if (PlayerController.isRepeatOne()) {
                menu.getItem(1).setIcon(R.drawable.ic_repeat_one_24dp);
                menu.getItem(1).setTitle(getResources().getString(R.string.action_disable_repeat));
            } else {
                menu.getItem(1).getIcon().setAlpha(128);
                menu.getItem(1).setTitle(getResources().getString(R.string.action_enable_repeat));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                PlayerController.toggleShuffle();
                if (PlayerController.isShuffle()) {
                    item.getIcon().setAlpha(255);
                    item.setTitle(getResources().getString(R.string.action_disable_shuffle));
                    showSnackbar(R.string.confirm_enable_shuffle);
                } else {
                    item.getIcon().setAlpha(128);
                    item.setTitle(getResources().getString(R.string.action_enable_shuffle));
                    showSnackbar(R.string.confirm_disable_shuffle);
                }
                queueFragment.updateShuffle();
                return true;
            case R.id.action_repeat:
                PlayerController.toggleRepeat();
                if (PlayerController.isRepeat()) {
                    item.getIcon().setAlpha(255);
                    item.setTitle(getResources().getString(R.string.action_enable_repeat_one));
                    showSnackbar(R.string.confirm_enable_repeat);
                } else {
                    if (PlayerController.isRepeatOne()) {
                        item.setIcon(R.drawable.ic_repeat_one_24dp);
                        item.setTitle(getResources().getString(R.string.action_disable_repeat));
                        showSnackbar(R.string.confirm_enable_repeat_one);
                    } else {
                        item.setIcon(R.drawable.ic_repeat_24dp);
                        item.getIcon().setAlpha(128);
                        item.setTitle(getResources().getString(R.string.action_enable_repeat));
                        showSnackbar(R.string.confirm_disable_repeat);
                    }
                }
                return true;
            case R.id.save:
                PlaylistDialog.MakeNormal.alert(
                        findViewById(R.id.imageArtwork),
                        PlayerController.getQueue());
                return true;
            case R.id.add_to_playlist:
                PlaylistDialog.AddToNormal.alert(
                        findViewById(R.id.imageArtwork),
                        PlayerController.getQueue(),
                        R.string.header_add_queue_to_playlist);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    }

    private void showSnackbar(@StringRes int stringId) {
        showSnackbar(getString(stringId));
    }

    @Override
    protected void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.imageArtwork), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onLeftSwipe() {
        PlayerController.skip();
    }

    @Override
    public void onRightSwipe() {
        int queuePosition = PlayerController.getQueuePosition() - 1;
        if (queuePosition < 0 && PlayerController.isRepeat()) {
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
