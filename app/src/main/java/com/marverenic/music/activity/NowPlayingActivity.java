package com.marverenic.music.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.Library;
import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.PlaylistDialog;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.TimeView;

import java.io.File;
import java.util.ArrayList;

public class NowPlayingActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener,
        PopupMenu.OnMenuItemClickListener {

    private MediaObserver observer = null;
    private boolean userTouchingProgressBar = false; // This probably shouldn't be here...
    private Song currentReference; // Used to reduce unnecessary view updates when an UPDATE broadcast is received

    private SeekBar mSeekBar;
    private TimeView mThumb;
    private TextView songTitle;
    private TextView artistName;
    private TextView albumTitle;
    private TimeView songDuration;
    private TimeView songPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean isTabletHorizontal = false;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            // If the activity is landscape on a tablet, use a different theme
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                isTabletHorizontal = true;
                Themes.setTheme(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    getWindow().setStatusBarColor(Themes.getPrimaryDark());
            }
        } else {
            // For devices that aren't tablets
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        onNewIntent(getIntent());

        if (!isTabletHorizontal && getSupportActionBar() != null){
            getSupportActionBar().setTitle("");
            getSupportActionBar().setBackgroundDrawable(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(0x66000000);
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSupportActionBar() != null) {
            getSupportActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));
        }

        findViewById(R.id.playButton).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);
        findViewById(R.id.songDetail).setOnClickListener(this);

        songTitle = (TextView) findViewById(R.id.textSongTitle);
        artistName = (TextView) findViewById(R.id.textArtistName);
        albumTitle = (TextView) findViewById(R.id.textAlbumTitle);
        songDuration = (TimeView) findViewById(R.id.songTimeMax);
        songPosition = (TimeView) findViewById(R.id.songTimeCurr);

        mSeekBar = ((SeekBar) findViewById(R.id.songSeekBar));
        mSeekBar.setOnSeekBarChangeListener(this);

        observer = new MediaObserver(this);
        mThumb = (TimeView) findViewById(R.id.seekThumb);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Handle incoming requests to play media from other applications
        if (intent.getData() == null) return;

        // If this intent is a music intent, process it
        if (intent.getType().contains("audio") || intent.getType().contains("application/ogg")
                || intent.getType().contains("application/x-ogg") || intent.getType().contains("application/itunes")){

            // The queue to be passed to the player service
            ArrayList<Song> queue = new ArrayList<>();
            int position = 0;

            // Have the LibraryScanner class get a song list for this file
            try{
                position = Library.getSongListFromFile(this, new File(intent.getData().getPath()), intent.getType(), queue);
            }
            catch (Exception e){
                e.printStackTrace();
                queue = new ArrayList<>();
            }

            if (queue.size() == 0){
                // No music was found
                Toast toast = Toast.makeText(getApplicationContext(), R.string.message_play_error_not_found, Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
            else {
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
                    registerReceiver(binderWaiter, new IntentFilter(Player.UPDATE_BROADCAST));
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
                    Toast toast = Toast.makeText(this, R.string.confirm_enable_shuffle, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    item.getIcon().setAlpha(128);
                    item.setTitle(getResources().getString(R.string.action_enable_shuffle));
                    Toast toast = Toast.makeText(this, R.string.confirm_disable_shuffle, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_repeat:
                PlayerController.toggleRepeat();
                if (PlayerController.isRepeat()) {
                    item.getIcon().setAlpha(255);
                    item.setTitle(getResources().getString(R.string.action_enable_repeat_one));
                    Toast toast = Toast.makeText(this, R.string.confirm_enable_repeat, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (PlayerController.isRepeatOne()) {
                        item.setIcon(R.drawable.ic_repeat_one_24dp);
                        item.setTitle(getResources().getString(R.string.action_disable_repeat));
                        Toast toast = Toast.makeText(this, R.string.confirm_enable_repeat_one, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        item.setIcon(R.drawable.ic_repeat_24dp);
                        item.getIcon().setAlpha(128);
                        item.setTitle(getResources().getString(R.string.action_enable_repeat));
                        Toast toast = Toast.makeText(this, R.string.confirm_disable_repeat, Toast.LENGTH_SHORT);
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
    public void onPause() {
        super.onPause();
        observer.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(observer).start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playButton) {
            PlayerController.togglePlay();
        }
        else if (v.getId() == R.id.nextButton) {
            // Next song
            PlayerController.skip();
        }
        else if (v.getId() == R.id.previousButton) {
            // Previous song
            PlayerController.previous();
            SeekBar seekBar = (SeekBar)findViewById(R.id.songSeekBar);
            seekBar.setProgress(0);
        }
        else if (v.getId() == R.id.songDetail){
            // Song info
            final Song nowPlaying = PlayerController.getNowPlaying();

            if (nowPlaying != null) {
                final PopupMenu menu = new PopupMenu(this, v, Gravity.END);
                String[] options = getResources().getStringArray(R.array.now_playing_options);

                for (int i = 0; i < options.length;  i++) {
                    menu.getMenu().add(Menu.NONE, i, i, options[i]);
                }
                menu.setOnMenuItemClickListener(this);
                menu.show();
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final Song nowPlaying = PlayerController.getNowPlaying();
        if (nowPlaying == null) {
            return false;
        }

        switch (item.getItemId()) {
            case 0: //Go to artist
                Artist artist = Library.findArtistById(nowPlaying.artistId);
                Navigate.to(this, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, artist);
                return true;
            case 1: //Go to album
                Album album = Library.findAlbumById(nowPlaying.albumId);

                Navigate.to(this, AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, album);
                return true;
            case 2: //Add to playlist
                PlaylistDialog.AddToNormal.alert(
                        findViewById(R.id.imageArtwork),
                        nowPlaying,
                        getString(
                                R.string.header_add_song_name_to_playlist,
                                nowPlaying.songName));
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void themeActivity() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Themes.getPrimary()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SeekBar seekBar = (SeekBar) findViewById(R.id.songSeekBar);

            Drawable thumb = seekBar.getThumb();
            thumb.setColorFilter(Themes.getAccent(), PorterDuff.Mode.SRC_IN);

            Drawable progress = seekBar.getProgressDrawable();
            progress.setTint(Themes.getAccent());
        } else {
            // For whatever reason, the control frame seems to need a reminder as to what color it should be
            findViewById(R.id.playerControlFrame).setBackgroundColor(getResources().getColor(R.color.player_control_background));
            if (getSupportActionBar() != null)
                getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        }

        findViewById(R.id.seekThumb).getBackground()
                .setColorFilter(Themes.getAccent(), PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void updateMiniplayer(){}

    @Override
    public void onUpdate() {
        super.onUpdate();

        Song nowPlaying = PlayerController.getNowPlaying();
        mSeekBar.setEnabled(nowPlaying != null);

        if (nowPlaying != null) {
            if (nowPlaying != currentReference) {
                // The following code only needs to be executed when the song changes, which
                // doesn't happen on every single UPDATE broadcast. Because of this, we can
                // reduce the number of redundant calls by only running this if the song has
                // changed.

                if (PlayerController.isPlaying() && !observer.isRunning())
                    new Thread(observer).start();

                songTitle.setText(nowPlaying.songName);
                artistName.setText(nowPlaying.artistName);
                albumTitle.setText(nowPlaying.albumName);

                ImageView artImageView = (ImageView) findViewById(R.id.imageArtwork);
                Bitmap artwork = Util.fetchFullArt(nowPlaying);
                if (artwork == null || artwork.getHeight() == 0 || artwork.getWidth() == 0) {
                    artImageView.setImageResource(R.drawable.art_default_xl);
                } else {
                    artImageView.setImageBitmap(artwork);
                }
                currentReference = nowPlaying;
            }

            if ((PlayerController.isPlaying() || PlayerController.isPreparing())) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_circle_fill_56dp);

                if (!observer.isRunning()) new Thread(observer).start();
                mSeekBar.setMax(PlayerController.getDuration());
                songDuration.setTime(mSeekBar.getMax());
            }
            else {
                int duration = PlayerController.getDuration();
                int progress = PlayerController.getCurrentPosition();

                mSeekBar.setMax(duration);
                songDuration.setTime(duration);
                mSeekBar.setProgress(progress);
                songPosition.setTime(progress);

                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_circle_fill_56dp);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (!userTouchingProgressBar) {
                // For keyboards and non-touch based things
                onStartTrackingTouch(seekBar);
                onStopTrackingTouch(seekBar);
            } else {
                alignSeekThumb();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userTouchingProgressBar = true;
        alignSeekThumb();
        showSeekThumb();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        PlayerController.seek(seekBar.getProgress());
        userTouchingProgressBar = false;
        hideSeekThumb();
    }

    private void showSeekThumb() {
        Animation fabAnim = AnimationUtils.loadAnimation(this, R.anim.slider_thumb_in);
        fabAnim.setDuration(300);
        fabAnim.setInterpolator(this, android.R.interpolator.decelerate_quint);

        mThumb.startAnimation(fabAnim);
        mThumb.setVisibility(View.VISIBLE);
    }

    private void hideSeekThumb() {
        Animation fabAnim = AnimationUtils.loadAnimation(this, R.anim.slider_thumb_out);
        fabAnim.setDuration(300);
        fabAnim.setInterpolator(this, android.R.interpolator.accelerate_quint);

        mThumb.startAnimation(fabAnim);

        // Make sure to hide the thumb after the animation finishes
        mThumb.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!userTouchingProgressBar) mThumb.setVisibility(View.INVISIBLE);
            }
        }, 300);
    }

    private void alignSeekThumb() {
        mThumb.setTime(mSeekBar.getProgress());
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mThumb.getLayoutParams();

        double progress = mSeekBar.getProgress() / (double) mSeekBar.getMax();
        int leftOffset = (int) (mSeekBar.getWidth() * progress) - mThumb.getWidth() / 2;
        leftOffset = Math.min(leftOffset, mSeekBar.getWidth() - mThumb.getWidth());
        leftOffset = Math.max(leftOffset, 0);

        params.setMargins(leftOffset, params.topMargin, params.rightMargin, params.bottomMargin);
        mThumb.setLayoutParams(params);
    }

    private class MediaObserver implements Runnable {
        private boolean stop = false;
        private SeekBar progress;
        private TimeView timeStamp;
        private NowPlayingActivity parent;

        public MediaObserver(NowPlayingActivity parent) {
            progress = (SeekBar) findViewById(R.id.songSeekBar);
            timeStamp = (TimeView) findViewById(R.id.songTimeCurr);
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
                        if (PlayerController.getNowPlaying() != null) {
                            int position = PlayerController.getCurrentPosition();
                            if (!userTouchingProgressBar) progress.setProgress(position);
                            timeStamp.setTime(position);
                        }
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (Exception ignored) {
                }
            }
        }

        public boolean isRunning() {
            return !stop;
        }
    }
}
