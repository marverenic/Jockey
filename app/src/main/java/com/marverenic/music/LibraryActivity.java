package com.marverenic.music;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
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
import android.widget.Toast;

import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.fragments.MiniplayerManager;
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

    // Set the intent's action to this to avoid automatically going to the Now Playing page
    public static final String ACTION_LIBRARY = "com.marverenic.music.LibraryActivity.LIBRARY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
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

    @Override
    public void onNewIntent (Intent intent){
        super.onNewIntent(intent);
        // If the player is playing, go to the Now Playing page
        if (!(intent.getAction() != null && intent.getAction().equals(ACTION_LIBRARY))
                && PlayerService.isInitialized() && PlayerService.isPlaying()){
            Navigate.to(this, NowPlayingActivity.class);
            intent.setAction(null);
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
        MiniplayerManager.onClick(v.getId(), this, R.id.pager);
    }

    public void update() {
        MiniplayerManager.update(this, R.id.pager);
    }
}
