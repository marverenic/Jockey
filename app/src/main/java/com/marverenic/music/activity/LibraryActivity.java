package com.marverenic.music.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.marverenic.music.Player;
import com.marverenic.music.R;
import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.fragments.MiniplayerManager;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Updater;
import com.marverenic.music.view.SlidingTabLayout;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoUtils;

public class LibraryActivity extends FragmentActivity implements View.OnClickListener {

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    // Set the intent's action to this to avoid automatically going to the Now Playing page
    public static final String ACTION_LIBRARY = "com.marverenic.music.activity.LibraryActivity.LIBRARY";

    // Intent flag used to determine whether to open the now playing activity or not
    public static final String START_NOW_PLAYING = "com.marverernic.music.LibraryActivity.GOTOPLAYING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
        Themes.setTheme(this);

        if (getActionBar() != null){
            getActionBar().setHomeButtonEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setDisplayShowHomeEnabled(false);
        }

        setContentView(R.layout.activity_library);
        findViewById(R.id.pagerSlidingTabs).setVisibility(View.INVISIBLE);
        findViewById(R.id.pager).setVisibility(View.INVISIBLE);
        ((View)(findViewById(R.id.miniplayer)).getParent()).setVisibility(View.INVISIBLE);

        new Thread(new Updater(this)).start();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!LibraryScanner.isLoaded()) {
            final Activity activity = this;
            new AsyncTask<Void, Void, Void>(){

                @Override
                public Void doInBackground(Void... voids){
                    LibraryScanner.scanAll(activity, true, true);
                    return null;
                }

                @Override
                protected void onPostExecute(Void results){
                    createPages(true);
                }

            }.execute();
        }
        else{
            createPages(false);
        }
    }

    @Override
    public void onNewIntent (Intent intent){
        super.onNewIntent(intent);
        // If the player is playing, go to the Now Playing page
        if (intent.getBooleanExtra(START_NOW_PLAYING, false)){
            Navigate.to(this, NowPlayingActivity.class);
            intent.removeExtra(START_NOW_PLAYING);
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

        View miniplayer = ((View)(findViewById(R.id.miniplayer)).getParent());
        miniplayer.setVisibility(View.VISIBLE);

        if (getResources().getConfiguration().smallestScreenWidthDp < 700 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tabs.setMini(true);
        } else {
            tabs.setMini(false);
        }

        update();

        if (fade) {
            pager.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            tabs.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            miniplayer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Themes.hasChanged(this)) {
            recreate();
        }
        if (findViewById(R.id.pager) != null && ((ViewPager) findViewById(R.id.pager)).getAdapter() != null) {
            ((LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter()).refreshPlaylists();
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
    public void onTrimMemory(int level){
        super.onTrimMemory(level);
        PicassoUtils.clearCache(Picasso.with(this));
    }

    @Override
    public void onStop() {
        PicassoUtils.clearCache(Picasso.with(this));
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigate.to(this, SettingsActivity.class);
                return true;
            case R.id.action_refresh_library:
                final LibraryActivity activity = this;
                LibraryScanner.refresh(this, true, true, new LibraryScanner.onScanCompleteListener() {
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
