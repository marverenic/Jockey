package com.marverenic.music.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.Player;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.io.File;
import java.util.ArrayList;

public class NowPlayingActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private MediaObserver observer = null;
    private boolean userTouchingProgressBar = false; // This probably shouldn't be here...

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) //Don't worry Lint. Everything is going to be okay.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean isTabletHorizontal = false;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
            // If the activity is landscape on a tablet, use a different theme
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                isTabletHorizontal = true;
                Themes.setTheme(this);
                getWindow().setStatusBarColor(Themes.getPrimaryDark());
            }
        } else {
            // For devices that aren't tablets
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentLayout(R.layout.activity_now_playing);
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());

        if (!isTabletHorizontal && getSupportActionBar() != null){
            getSupportActionBar().setTitle("");
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.skrim_now_playing));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                getWindow().setStatusBarColor(0x66000000);
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getSupportActionBar() != null)
            getSupportActionBar().setElevation(getResources().getDimension(R.dimen.header_elevation));

        findViewById(R.id.playButton).setOnClickListener(this);
        findViewById(R.id.nextButton).setOnClickListener(this);
        findViewById(R.id.previousButton).setOnClickListener(this);
        ((SeekBar) findViewById(R.id.songSeekBar)).setOnSeekBarChangeListener(this);

        observer = new MediaObserver(this);
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
                position = LibraryScanner.getSongListFromFile(this, new File(intent.getData().getPath()), intent.getType(), queue);
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
                PlayerController.setQueue(queue, position);
                PlayerController.begin();

                startService(new Intent(this, Player.class));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.now_playing, menu);

        if (PlayerController.isShuffle()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle);
            else
                menu.getItem(0).setIcon(R.drawable.ic_shuffle);

            menu.getItem(0).setTitle("Disable Shuffle");
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(0).setIcon(R.drawable.ic_vector_shuffle_off);
            else
                menu.getItem(0).setIcon(R.drawable.ic_shuffle_off);

            menu.getItem(0).setTitle("Enable Shuffle");
        }

        if (PlayerController.isRepeat()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                menu.getItem(1).setIcon(R.drawable.ic_vector_repeat);
            else
                menu.getItem(1).setIcon(R.drawable.ic_repeat);

            menu.getItem(1).setTitle("Enable Repeat One");
        } else {
            if (PlayerController.isRepeatOne()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_one);
                else
                    menu.getItem(1).setIcon(R.drawable.ic_repeat_one);
                menu.getItem(1).setTitle("Disable Repeat");
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    menu.getItem(1).setIcon(R.drawable.ic_vector_repeat_off);
                else
                    menu.getItem(1).setIcon(R.drawable.ic_repeat_off);
                menu.getItem(1).setTitle("Enable Repeat");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                PlayerController.toggleShuffle();
                if (!PlayerController.isShuffle()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_shuffle);
                    else
                        item.setIcon(R.drawable.ic_shuffle);

                    item.setTitle("Disable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_shuffle_off);
                    else
                        item.setIcon(R.drawable.ic_shuffle_off);

                    item.setTitle("Enable Shuffle");
                    Toast toast = Toast.makeText(this, "Shuffle Disabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_repeat:
                PlayerController.toggleRepeat();
                if (PlayerController.isRepeat()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        item.setIcon(R.drawable.ic_vector_repeat);
                    else
                        item.setIcon(R.drawable.ic_repeat);

                    item.setTitle("Enable Repeat One");
                    Toast toast = Toast.makeText(this, "Repeat Enabled", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    if (PlayerController.isRepeatOne()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            item.setIcon(R.drawable.ic_vector_repeat_one);
                        else
                            item.setIcon(R.drawable.ic_repeat_one);

                        item.setTitle("Disable Repeat");
                        Toast toast = Toast.makeText(this, "Repeat One Enabled", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            item.setIcon(R.drawable.ic_vector_repeat_off);
                        else
                            item.setIcon(R.drawable.ic_repeat_off);

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
            PlayerController.skip();
            observer.stop();
            SeekBar seekBar = (SeekBar)findViewById(R.id.songSeekBar);
            seekBar.setMax(Integer.MAX_VALUE);
            seekBar.setProgress(Integer.MAX_VALUE);
        }
        else if (v.getId() == R.id.previousButton) {
            PlayerController.previous();
            SeekBar seekBar = (SeekBar)findViewById(R.id.songSeekBar);
            seekBar.setProgress(0);
        }
        else if (v.getId() == R.id.songInfo){
            final Context context = this;
            final Song nowPlaying = PlayerController.getNowPlaying();

            if (nowPlaying != null) {
                AlertDialog.Builder alert;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    alert = new AlertDialog.Builder(this, Themes.getAlertTheme(this));
                }
                else{
                    alert = new AlertDialog.Builder(new ContextThemeWrapper(this, Themes.getAlertTheme(this)));
                }
                alert
                        .setTitle(nowPlaying.songName)
                        .setNegativeButton("Cancel", null)
                        .setItems(R.array.now_playing_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: //Go to artist
                                        Artist artist = LibraryScanner.findArtistById(nowPlaying.artistId);

                                        Navigate.to(context, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, artist);
                                        break;
                                    case 1: //Go to album
                                        Album album = LibraryScanner.findAlbumById(nowPlaying.albumId);

                                        Navigate.to(context, InstanceActivity.class, "entry", album);
                                        break;
                                    case 2: //Add to playlist
                                        ArrayList<Playlist> playlists = Library.getPlaylists();
                                        String[] playlistNames = new String[playlists.size()];

                                        for (int i = 0; i < playlists.size(); i++ ){
                                            playlistNames[i] = playlists.get(i).toString();
                                        }

                                        new AlertDialog.Builder(context, Themes.getAlertTheme(context))
                                                .setTitle("Add \"" + nowPlaying.songName + "\" to playlist")
                                                .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        LibraryScanner.addPlaylistEntry(context, Library.getPlaylists().get(which), nowPlaying);
                                                    }
                                                })
                                                .setNeutralButton("Cancel", null)
                                                .show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .show();
            }
        }
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

            seekBar.setThumb(thumb);
            seekBar.setProgressDrawable(progress);
        } else {
            // For whatever reason, the control frame seems to need a reminder as to what color it should be
            findViewById(R.id.playerControlFrame).setBackgroundColor(getResources().getColor(R.color.player_control_background));
            if (getSupportActionBar() != null)
                getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        }
    }

    @Override
    public void updateMiniplayer(){}

    @Override
    public void update() {
        Song nowPlaying = PlayerController.getNowPlaying();
        if (nowPlaying != null) {

            if (PlayerController.isPlaying() && !observer.isRunning()) new Thread(observer).start();

            final TextView songTitle = (TextView) findViewById(R.id.textSongTitle);
            final TextView artistName = (TextView) findViewById(R.id.textArtistName);
            final TextView albumTitle = (TextView) findViewById(R.id.textAlbumTitle);
            final SeekBar seekBar = ((SeekBar) findViewById(R.id.songSeekBar));

            songTitle.setText(nowPlaying.songName);
            artistName.setText(nowPlaying.artistName);
            albumTitle.setText(nowPlaying.albumName);

            ImageView artImageView = (ImageView) findViewById(R.id.imageArtwork);
            if (PlayerController.getFullArt() != null) {
                artImageView.setImageBitmap(PlayerController.getFullArt());
            }
            else if (PlayerController.getArt() != null){
                artImageView.setImageBitmap(PlayerController.getArt());
            }
            else {
                if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
                    artImageView.setImageResource(R.drawable.art_default_xxl);
                } else {
                    artImageView.setImageResource(R.drawable.art_default_xl);
                }
            }

            if ((PlayerController.isPlaying() || PlayerController.isPreparing())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause_circle_fill);
                } else {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_circle_fill);
                }
                if (!PlayerController.isPreparing()) {
                    if (!observer.isRunning()) new Thread(observer).start();
                    seekBar.setMax(PlayerController.getDuration());
                }
                else{
                    observer.stop();
                    seekBar.setProgress(0);
                    seekBar.setMax(Integer.MAX_VALUE);
                }
            }
            else {
                seekBar.setMax(PlayerController.getDuration());
                seekBar.setProgress(PlayerController.getCurrentPosition());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play_circle_fill);
                } else {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_circle_fill);
                }
            }
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
        PlayerController.seek(seekBar.getProgress());
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
                        progress.setProgress(PlayerController.getCurrentPosition());
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

        public boolean isRunning() {
            return !stop;
        }
    }
}
