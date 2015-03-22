package com.marverenic.music;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.SearchView;

import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.fragments.MiniplayerManager;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
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

    private SearchPagerAdapter adapter;

    private ArrayList<Album> albumResults = new ArrayList<>();
    private ArrayList<Artist> artistResults = new ArrayList<>();
    private ArrayList<Genre> genreResults = new ArrayList<>();
    private ArrayList<Playlist> playlistResults = new ArrayList<>();
    private ArrayList<Song> songResults = new ArrayList<>();

    private static String lastQuery = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Themes.setTheme(this);
        setContentView(R.layout.activity_library);
        search(getIntent());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int page = Integer.parseInt(prefs.getString("prefDefaultPage", "1"));

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        adapter = new SearchPagerAdapter(this, playlistResults, songResults, artistResults, albumResults, genreResults);
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
        LibraryScanner.saveLibrary(this);
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
        getMenuInflater().inflate(R.menu.search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });

        searchView.setIconified(false);
        searchView.requestFocus();

        if (lastQuery != null) searchView.setQuery(lastQuery, true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                lastQuery = null;
                Navigate.home(this);
                return true;
            case R.id.search:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) onSearchRequested();
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

    @Override
    public void onBackPressed() {
        lastQuery = null;
        Navigate.home(this);
        super.onBackPressed();
    }

    public void update() {
        MiniplayerManager.update(this, R.id.pager);
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

    private void search(CharSequence searchInput) {
        String query = searchInput.toString().toLowerCase();

        lastQuery = searchInput.toString();

        albumResults = new ArrayList<>();
        artistResults = new ArrayList<>();
        genreResults = new ArrayList<>();
        songResults = new ArrayList<>();
        playlistResults = new ArrayList<>();

        if (!query.equals("")) {
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

            for(Song s : Library.getSongs()){
                if (s.songName.toLowerCase().contains(query)
                        || s.artistName.toLowerCase().contains(query)
                        || s.albumName.toLowerCase().contains(query)) {
                    songResults.add(s);

                    if (s.genreId != -1) {
                        Genre g = LibraryScanner.findGenreById(s.genreId);
                        if (!genreResults.contains(g)) genreResults.add(g);
                    }

                    Album thisAlbum = LibraryScanner.findAlbumById(s.albumId);
                    if(!albumResults.contains(thisAlbum)) albumResults.add(thisAlbum);

                    Artist thisArtist = LibraryScanner.findArtistById(s.artistId);
                    if(!artistResults.contains(thisArtist)) artistResults.add(thisArtist);
                }
            }

            for(Playlist p : Library.getPlaylists()){
                if (p.playlistName.toLowerCase().contains(query)) {
                    playlistResults.add(p);
                }
                else{
                    for (Song s : LibraryScanner.getPlaylistEntries(this, p)){
                        if (songResults.contains(s)){
                            if (!playlistResults.contains(p)) playlistResults.add(p);
                        }
                    }
                }
            }

            Library.sortGenreList(genreResults);
        }

        adapter.updateData(playlistResults, songResults, artistResults, albumResults, genreResults);
    }
}
