package com.marverenic.music.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.marverenic.music.R;
import com.marverenic.music.adapters.SearchPagerAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.SlidingTabLayout;

import java.util.ArrayList;

public class SearchActivity extends BaseActivity {

    private SearchPagerAdapter adapter;

    private ArrayList<Album> albumResults = new ArrayList<>();
    private ArrayList<Artist> artistResults = new ArrayList<>();
    private ArrayList<Genre> genreResults = new ArrayList<>();
    private ArrayList<Playlist> playlistResults = new ArrayList<>();
    private ArrayList<Song> songResults = new ArrayList<>();

    private static String lastQuery = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.activity_library);
        setContentView(R.id.pager);
        super.onCreate(savedInstanceState);

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
    public void onBackPressed() {
        lastQuery = null;
        super.onBackPressed();
    }

    @Override
    public void themeActivity() {
        Themes.themeActivity(R.layout.activity_library, getWindow().getDecorView().findViewById(android.R.id.content), this);
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
