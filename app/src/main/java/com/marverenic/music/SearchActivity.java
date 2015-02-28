package com.marverenic.music;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.SlidingTabLayout;

import java.util.ArrayList;

public class SearchActivity extends FragmentActivity implements View.OnClickListener {

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    private ArrayList<Album> albumResults = new ArrayList<>();
    private ArrayList<Artist> artistResults = new ArrayList<>();
    private ArrayList<Genre> genreResults = new ArrayList<>();
    private ArrayList<Playlist> playlistResults = new ArrayList<>();
    private ArrayList<Song> songResults = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);
        setContentView(R.layout.activity_library);
        search(getIntent());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int page = Integer.parseInt(prefs.getString("prefDefaultPage", "1"));

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        SearchPagerAdapter adapter = new SearchPagerAdapter(this, playlistResults, songResults, artistResults, albumResults, genreResults);
        pager.setAdapter(adapter);
        pager.setCurrentItem(page);

        SlidingTabLayout tabs = ((SlidingTabLayout) findViewById(R.id.pagerSlidingTabs));
        tabs.setViewPager(pager);
        tabs.setActivePage(page);

        if (getResources().getConfiguration().smallestScreenWidthDp < 700 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tabs.setMini(true);
        } else {
            tabs.setMini(false);
        }

        Themes.themeActivity(R.layout.activity_library, getWindow().getDecorView().findViewById(android.R.id.content), this);

        startService(new Intent(this, Player.class));
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onResume() {
        update();
        Themes.setApplicationIcon(this);
        registerReceiver(updateReceiver, new IntentFilter(Player.UPDATE_BROADCAST));
        super.onResume();
    }

    @Override
    public void onPause() {
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            Debug.log(Debug.LogLevel.ERROR, "LibraryActivity", "Unable to unregister receiver", this);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        if (getIntent().getStringExtra(SearchManager.QUERY) != null) {
            searchView.setQuery(getIntent().getStringExtra(SearchManager.QUERY), false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                Navigate.home(this);
                return true;
            case R.id.action_settings:
                Navigate.to(this, SettingsActivity.class);
                return true;
            case R.id.search:
                onSearchRequested();
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        search(intent);
    }

    private void search(Intent intent){
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            search(intent.getStringExtra(SearchManager.QUERY).toLowerCase());
        }
    }

    private void search(CharSequence query) {
        if (!query.equals("")) {

            albumResults = new ArrayList<>();
            artistResults = new ArrayList<>();
            genreResults = new ArrayList<>();
            playlistResults = new ArrayList<>();
            songResults = new ArrayList<>();

            for(Album a : Library.getAlbums()){
                if (a.albumName.toLowerCase().contains(query) || a.artistName.toLowerCase().contains(query)) {
                    albumResults.add(a);
                }
            }

            for(Artist a : Library.getArtists()){
                if (a.artistName.toLowerCase().contains(query)) {
                    artistResults.add(a);
                }
            }

            for(Genre g : Library.getGenres()){
                if (g.genreName.toLowerCase().contains(query)) {
                    genreResults.add(g);
                }
            }

            for(Playlist p : Library.getPlaylists()){
                if (p.playlistName.toLowerCase().contains(query)) {
                    playlistResults.add(p);
                }
            }

            for(Song s : Library.getSongs()){
                if (s.songName.toLowerCase().contains(query)
                        || s.artistName.toLowerCase().contains(query)
                        || s.albumName.toLowerCase().contains(query)) {
                    songResults.add(s);
                }
            }
        }
    }
}
