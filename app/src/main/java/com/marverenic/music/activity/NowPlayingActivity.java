package com.marverenic.music.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;

import java.io.File;
import java.util.ArrayList;

public class NowPlayingActivity extends BaseActivity implements SeekBar.OnSeekBarChangeListener {

    private MediaObserver observer = null;
    private boolean userTouchingProgressBar = false; // This probably shouldn't be here...
    private Song currentReference; // Used to reduce unnecessary view updates when an UPDATE broadcast is received

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean isTabletHorizontal = false;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 700) {
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
                PlayerController.setQueue(queue, position);
                PlayerController.begin();

                startService(new Intent(this, Player.class));
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
            observer.stop();
            SeekBar seekBar = (SeekBar)findViewById(R.id.songSeekBar);
            seekBar.setMax(Integer.MAX_VALUE);
            seekBar.setProgress(Integer.MAX_VALUE);
        }
        else if (v.getId() == R.id.previousButton) {
            // Previous song
            PlayerController.previous();
            SeekBar seekBar = (SeekBar)findViewById(R.id.songSeekBar);
            seekBar.setProgress(0);
        }
        else if (v.getId() == R.id.songInfo){
            // Song info
            final Context context = this;
            final Song nowPlaying = PlayerController.getNowPlaying();

            if (nowPlaying != null) {
                // Show an AlertDialog for navigating to relevant activities
                AlertDialog infoDialog = new AlertDialog.Builder(this)
                        .setTitle(nowPlaying.songName)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setItems(R.array.now_playing_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0: //Go to artist
                                        Artist artist = Library.findArtistById(nowPlaying.artistId);

                                        Navigate.to(context, ArtistActivity.class, ArtistActivity.ARTIST_EXTRA, artist);
                                        break;
                                    case 1: //Go to album
                                        Album album = Library.findAlbumById(nowPlaying.albumId);

                                        Navigate.to(context, AlbumActivity.class, AlbumActivity.ALBUM_EXTRA, album);
                                        break;
                                    case 2: //Add to playlist
                                        ArrayList<Playlist> playlists = Library.getPlaylists();
                                        String[] playlistNames = new String[playlists.size()];

                                        for (int i = 0; i < playlists.size(); i++ ){
                                            playlistNames[i] = playlists.get(i).toString();
                                        }

                                        AlertDialog playlistDialog = new AlertDialog.Builder(context)
                                                .setTitle(getString(R.string.header_add_song_name_to_playlist, nowPlaying.songName))
                                                .setItems(playlistNames, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Library.addPlaylistEntry(context, Library.getPlaylists().get(which), nowPlaying);
                                                    }
                                                })
                                                .setNeutralButton(R.string.action_cancel, null)
                                                .show();

                                        playlistDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Themes.getAccent());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })
                        .show();

                infoDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Themes.getAccent());
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

            if (nowPlaying != currentReference) {
                // The following code only needs to be executed when the song changes, which
                // doesn't happen on every single UPDATE broadcast. Because of this, we can
                // reduce the number of redundant calls by only running this if the song has
                // changed.

                if (PlayerController.isPlaying() && !observer.isRunning())
                    new Thread(observer).start();

                final TextView songTitle = (TextView) findViewById(R.id.textSongTitle);
                final TextView artistName = (TextView) findViewById(R.id.textArtistName);
                final TextView albumTitle = (TextView) findViewById(R.id.textAlbumTitle);

                songTitle.setText(nowPlaying.songName);
                artistName.setText(nowPlaying.artistName);
                albumTitle.setText(nowPlaying.albumName);

                ImageView artImageView = (ImageView) findViewById(R.id.imageArtwork);
                Bitmap artwork = Fetch.fetchFullArt(nowPlaying);
                if (artwork == null || artwork.getHeight() == 0 || artwork.getWidth() == 0) {
                    artImageView.setImageResource(R.drawable.art_default_xl);
                } else {
                    artImageView.setImageBitmap(artwork);
                }
                currentReference = nowPlaying;
            }

            final SeekBar seekBar = ((SeekBar) findViewById(R.id.songSeekBar));

            if ((PlayerController.isPlaying() || PlayerController.isPreparing())) {
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_circle_fill_72dp);

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
                ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_circle_fill_72dp);
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
                } catch (Exception ignored) {
                }
            }
        }

        public boolean isRunning() {
            return !stop;
        }
    }
}
