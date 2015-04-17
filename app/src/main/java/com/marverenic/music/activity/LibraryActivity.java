package com.marverenic.music.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.marverenic.music.R;
import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.utils.Updater;
import com.marverenic.music.view.SlidingTabLayout;

public class LibraryActivity extends BaseActivity {

    // Set the intent's action to this to avoid automatically going to the Now Playing page
    public static final String ACTION_LIBRARY = "com.marverenic.music.activity.LibraryActivity.LIBRARY";

    // Intent flag used to determine whether to open the now playing activity or not
    public static final String START_NOW_PLAYING = "com.marverernic.music.LibraryActivity.GOTOPLAYING";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.activity_library);
        setContentView(R.id.pager);
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        findViewById(R.id.pagerSlidingTabs).setVisibility(View.INVISIBLE);
        findViewById(R.id.pager).setVisibility(View.INVISIBLE);
        ((View)(findViewById(R.id.miniplayer)).getParent()).setVisibility(View.INVISIBLE);

        new Thread(new Updater(this)).start();

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
        Themes.setApplicationIcon(this);
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
    public void themeActivity() {
        super.themeActivity();

        findViewById(R.id.pagerSlidingTabs).setBackgroundColor(Themes.getPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.pagerSlidingTabs).setElevation(0);
        }
        LayerDrawable backgroundDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(Themes.getBackground());
        topDrawable.setColor(Themes.getPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findViewById(R.id.pager).setBackground(backgroundDrawable);
        }
        else {
            findViewById(R.id.pager).setBackgroundDrawable(backgroundDrawable);
        }
    }
}
