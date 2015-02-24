package com.marverenic.music;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

public class NowPlayingActivity extends Activity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private MediaObserver observer = null;
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };
    private boolean userTouchingProgressBar = false; // This probably shouldn't be here...

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) //Don't worry Lint. Everything is going to be okay.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setTheme(R.style.AppTheme);
                getWindow().setStatusBarColor(Themes.getPrimaryDark());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActionBar() != null) {
                    getActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));
                }
            } else {
                setTheme(R.style.NowPlayingTheme);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActionBar() != null) {
                    getActionBar().setElevation(0);
                }
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        Themes.themeActivity(R.layout.activity_now_playing, getWindow().getDecorView().findViewById(android.R.id.content), this);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.playButton).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);
        ((SeekBar) findViewById(R.id.songSeekBar)).setOnSeekBarChangeListener(this);

        observer = new MediaObserver(this);
        new Thread(observer).start();

        if (PlayerService.isInitialized()) {
            update();
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_now_playing, menu);

        if (PlayerService.isShuffle()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle);
            }
            menu.getItem(0).setTitle("Disable Shuffle");
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle_off);
            }
            menu.getItem(0).setTitle("Enable Shuffle");
        }

        if (PlayerService.isRepeat()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                menu.getItem(1).setIcon(R.drawable.ic_vector_repeat);
            }
            menu.getItem(1).setTitle("Enable Repeat One");
        } else {
            if (PlayerService.isRepeatOne()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_one);
                }
                menu.getItem(1).setTitle("Disable Repeat");
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_off);
                }
                menu.getItem(1).setTitle("Enable Repeat");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Navigate.up(this);
                return true;
            case R.id.action_shuffle:
                PlayerService.toggleShuffle();
                if (PlayerService.isShuffle()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(R.drawable.ic_vector_shuffle);
                    }
                    item.setTitle("Disable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(R.drawable.ic_vector_shuffle_off);
                    }
                    item.setTitle("Enable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Disabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_repeat:
                PlayerService.toggleRepeat();
                if (PlayerService.isRepeat()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(R.drawable.ic_vector_repeat);
                    }
                    item.setTitle("Enable Repeat One");
                    Toast toast = Toast.makeText(this, "Repeat Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (PlayerService.isRepeatOne()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            item.setIcon(R.drawable.ic_vector_repeat_one);
                        }
                        item.setTitle("Disable Repeat");
                        Toast toast = Toast.makeText(this, "Repeat One Enabled", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            item.setIcon(R.drawable.ic_vector_repeat_off);
                        }
                        item.setTitle("Enable Repeat");
                        Toast toast = Toast.makeText(this, "Repeat Disabled", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                return true;
            case R.id.action_queue:
                Navigate.to(this, QueueActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
    }

    @Override
    public void onPause() {
        observer.stop();
        unregisterReceiver(updateReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        new Thread(observer).start();
        update();
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playButton) {
            if (PlayerService.getNowPlaying() != null) {
                PlayerService.togglePlay();
            }
        } else if (v.getId() == R.id.nextButton) {
            PlayerService.skip();
        } else if (v.getId() == R.id.previousButton) {
            PlayerService.previous();
        }
        update();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Navigate.back(this);
    }

    public void update() {
        if (PlayerService.getNowPlaying() != null) {

            //final ViewGroup background = (ViewGroup) findViewById(R.id.playerControlFrame);
            final TextView songTitle = (TextView) findViewById(R.id.textSongTitle);
            final TextView artistName = (TextView) findViewById(R.id.textArtistName);
            final TextView albumTitle = (TextView) findViewById(R.id.textAlbumTitle);
            final SeekBar seekBar = ((SeekBar) findViewById(R.id.songSeekBar));

            songTitle.setText(PlayerService.getNowPlaying().songName);
            artistName.setText(PlayerService.getNowPlaying().artistName);
            albumTitle.setText(PlayerService.getNowPlaying().albumName);
            seekBar.setMax(PlayerService.getNowPlaying().songDuration);

            if (PlayerService.getArt() != null) {
                ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(PlayerService.getArt());
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
                        ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default_xxl);
                    } else {
                        ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default_xl);
                    }
                }
            }
        }
        if (PlayerService.isPlaying()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause_circle_fill);
            } else {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_circle_fill);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play_circle_fill);
            } else {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_circle_fill);
            }
            observer.stop();
            new Thread(observer).start();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !userTouchingProgressBar) {
            // For keyboards and non-touch based things
            onStartTrackingTouch(seekBar);
            onStopTrackingTouch(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        observer.stop();
        userTouchingProgressBar = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        PlayerService.seek(seekBar.getProgress());
        observer = new MediaObserver(this);
        new Thread(observer).start();
        userTouchingProgressBar = false;

    }

    private class MediaObserver implements Runnable {
        private boolean stop = false;
        private SeekBar progress;
        private NowPlayingActivity parent;

        public MediaObserver(NowPlayingActivity parent) {
            progress = (SeekBar) findViewById(R.id.songSeekBar);
            this.parent = parent;
        }

        public void stop() {
            stop = true;
        }

        @Override
        public void run() {
            stop = false;
            while (!stop) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setProgress(PlayerService.getCurrentPosition());
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                    Debug.log(Debug.LogLevel.WTF, "NowPlayingActivity/MediaObserver", "Some horrible thread exception has occurred", parent);
                }
            }
        }
    }
}
