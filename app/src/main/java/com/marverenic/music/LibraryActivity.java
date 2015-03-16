package com.marverenic.music;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Updater;
import com.marverenic.music.view.SlidingTabLayout;

public class LibraryActivity extends FragmentActivity implements View.OnClickListener {

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);
        setContentView(R.layout.activity_library);
        findViewById(R.id.pagerSlidingTabs).setVisibility(View.INVISIBLE);
        findViewById(R.id.pagerSlidingTabs).setVisibility(View.INVISIBLE);

        new Thread(new Updater(this)).start();

        startService(new Intent(this, Player.class));
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!LibraryScanner.isLoaded()) {
            LibraryScanner.scanAll(this, true, new LibraryScanner.onScanCompleteListener() {
                @Override
                public void onScanComplete() {
                    createPages(true);
                }
            });
        }
        else{
            createPages(false);
        }
    }

    private void createPages(boolean fade) {
        Themes.themeActivity(R.layout.activity_library, getWindow().getDecorView().findViewById(android.R.id.content), this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int page = Integer.parseInt(prefs.getString("prefDefaultPage", "1"));

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(page);
        pager.setVisibility(View.VISIBLE);

        SlidingTabLayout tabs = ((SlidingTabLayout) findViewById(R.id.pagerSlidingTabs));
        tabs.setViewPager(pager);
        tabs.setActivePage(page);
        tabs.setVisibility(View.VISIBLE);

        if (getResources().getConfiguration().smallestScreenWidthDp < 700 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tabs.setMini(true);
        } else {
            tabs.setMini(false);
        }

        if (fade) {
            pager.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            tabs.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Themes.hasChanged(this)) {
            recreate();
        }
        update();
        Themes.setApplicationIcon(this);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        LibraryScanner.saveLibrary(this);
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            Debug.log(Debug.LogLevel.ERROR, "LibraryActivity", "Unable to unregister receiver", this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigate.to(this, SettingsActivity.class);
                return true;
            case R.id.action_refresh_library:
                Library.resetAll();
                final LibraryActivity activity = this;
                LibraryScanner.scanAll(this, false, new LibraryScanner.onScanCompleteListener() {
                    @Override
                    public void onScanComplete() {
                        Toast.makeText(activity, "Library refreshed.", Toast.LENGTH_SHORT).show();
                        recreate();
                    }
                });
                return true;
            case R.id.search:
                Navigate.to(this, SearchActivity.class);
                return true;
            case R.id.action_new_playlist:
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Playlist name");

                final Context context = this;

                new AlertDialog.Builder(this)
                        .setTitle("Create Playlist")
                        .setView(input)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LibraryScanner.createPlaylist(context, input.getText().toString(), null);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();
                return true;
            case R.id.action_about:
                Navigate.to(this, AboutActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.miniplayer:
                Navigate.to(this, NowPlayingActivity.class);
                update();
                break;
            case R.id.playButton:
                PlayerService.togglePlay();
                update();
                break;
            case R.id.skipButton:
                PlayerService.skip();
                update();
                break;
        }
    }

    public void update() {
        if (PlayerService.isInitialized() && PlayerService.getNowPlaying() != null) {
            final TextView songTitle = (TextView) findViewById(R.id.textNowPlayingTitle);
            final TextView artistName = (TextView) findViewById(R.id.textNowPlayingDetail);

            songTitle.setText(PlayerService.getNowPlaying().songName);
            artistName.setText(PlayerService.getNowPlaying().artistName);

            if (!PlayerService.isPlaying()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_play);
                    ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                } else {
                    if (Themes.isLight(this)) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer_light);
                    } else {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_play_miniplayer);
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_vector_pause);
                    ((ImageButton) findViewById(R.id.playButton)).setImageTintList(ColorStateList.valueOf(Themes.getListText()));
                } else {
                    if (Themes.isLight(this)) {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer_light);
                    } else {
                        ((ImageButton) findViewById(R.id.playButton)).setImageResource(R.drawable.ic_pause_miniplayer);
                    }
                }
            }

            if (PlayerService.getArt() != null) {
                ((ImageView) findViewById(R.id.imageArtwork)).setImageBitmap(PlayerService.getArt());
            } else {
                ((ImageView) findViewById(R.id.imageArtwork)).setImageResource(R.drawable.art_default);
            }

            RelativeLayout.LayoutParams pagerLayoutParams = (RelativeLayout.LayoutParams) (findViewById(R.id.pager)).getLayoutParams();
            pagerLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
            (findViewById(R.id.pager)).setLayoutParams(pagerLayoutParams);

            FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
            playerLayoutParams.height = getResources().getDimensionPixelSize(R.dimen.now_playing_ticker_height);
            (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
        } else {
            RelativeLayout.LayoutParams pagerLayoutParams = (RelativeLayout.LayoutParams) (findViewById(R.id.pager)).getLayoutParams();
            pagerLayoutParams.bottomMargin = 0;
            (findViewById(R.id.pager)).setLayoutParams(pagerLayoutParams);

            FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
            playerLayoutParams.height = 0;
            (findViewById(R.id.miniplayer)).setLayoutParams(playerLayoutParams);
        }
    }
}
