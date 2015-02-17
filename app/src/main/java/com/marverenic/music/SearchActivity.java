package com.marverenic.music;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.SearchView;

import com.marverenic.music.adapters.AlbumGridAdapter;
import com.marverenic.music.adapters.ArtistListAdapter;
import com.marverenic.music.adapters.GenreListAdapter;
import com.marverenic.music.adapters.LibraryPagerAdapter;
import com.marverenic.music.adapters.PlaylistListAdapter;
import com.marverenic.music.adapters.SongListAdapter;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Debug;
import com.marverenic.music.view.SlidingTabLayout;

import java.util.ArrayList;

public class SearchActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Debug.log(Debug.WTF, "SearchActivity", "The search activity was created.", this);

        if (!isTaskRoot()) {
            finish();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


        setContentView(R.layout.activity_library);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setCurrentItem(Integer.parseInt(prefs.getString("prefDefaultPage", "1")));

        SlidingTabLayout tabs = ((SlidingTabLayout) findViewById(R.id.pagerSlidingTabs));
        tabs.setViewPager(pager);
        tabs.setActivePage(Integer.parseInt(prefs.getString("prefDefaultPage", "1")));
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.search:
                super.onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            // THIS IS HORRENDOUSLY BROKEN

            ArrayList<Album> albumResults = Library.getAlbums();
            ArrayList<Artist> artistResults = Library.getArtists();
            ArrayList<Genre> genreResults = Library.getGenres();
            ArrayList<Playlist> playlistResults = Library.getPlaylists();
            ArrayList<Song> songResults = Library.getSongs();

            String query = intent.getStringExtra(SearchManager.QUERY);

            if (!query.equals("")) {

                albumResults = new ArrayList<>();
                artistResults = new ArrayList<>();
                genreResults = new ArrayList<>();
                playlistResults = new ArrayList<>();
                songResults = new ArrayList<>();

                for (int i = 0; i < Library.getAlbums().size(); i++) {
                    Log.i("SEARCH", "Looking at album " + Library.getAlbums().get(i));
                    if (Library.getAlbums().get(i).albumName.contains(query) || Library.getAlbums().get(i).artistName.contains(query)) {
                        albumResults.add(Library.getAlbums().get(i));
                        Log.i("SEARCH", "Added album " + Library.getAlbums().get(i));
                    }
                }

                for (int i = 0; i < Library.getArtists().size(); i++) {
                    Log.i("SEARCH", "Looking at artist " + Library.getArtists().get(i));
                    if (Library.getArtists().get(i).artistName.contains(query)) {
                        artistResults.add(Library.getArtists().get(i));
                        Log.i("SEARCH", "Added artist " + Library.getArtists().get(i));
                    }
                }

                for (int i = 0; i < Library.getGenres().size(); i++) {
                    Log.i("SEARCH", "Looking at genre " + Library.getGenres().get(i));
                    if (Library.getGenres().get(i).genreName.contains(query)) {
                        genreResults.add(Library.getGenres().get(i));
                        Log.i("SEARCH", "Added genre " + Library.getGenres().get(i));
                    }
                }

                for (int i = 0; i < Library.getPlaylists().size(); i++) {
                    Log.i("SEARCH", "Looking at playlist " + Library.getPlaylists().get(i));
                    if (Library.getPlaylists().get(i).playlistName.contains(query)) {
                        playlistResults.add(Library.getPlaylists().get(i));
                        Log.i("SEARCH", "Added genre " + Library.getPlaylists().get(i));
                    }
                }

                for (int i = 0; i < Library.getSongs().size(); i++) {
                    Log.i("SEARCH", "Looking at song " + Library.getSongs().get(i));
                    if (Library.getSongs().get(i).songName.contains(query) ||
                            Library.getSongs().get(i).artistName.contains(query) ||
                            Library.getSongs().get(i).albumName.contains(query)) {
                        songResults.add(Library.getSongs().get(i));
                        Log.i("SEARCH", "Added song " + Library.getSongs().get(i));
                    }
                }

            }

            LibraryPagerAdapter adapter = (LibraryPagerAdapter) ((ViewPager) findViewById(R.id.pager)).getAdapter();


            ((PlaylistListAdapter) ((ListView) (adapter.getItem(0)).getView().findViewById(R.id.list)).getAdapter()).updateData(playlistResults);
            ((SongListAdapter) ((ListView) (adapter.getItem(1)).getView().findViewById(R.id.list)).getAdapter()).updateData(songResults);
            ((ArtistListAdapter) ((ListView) (adapter.getItem(2)).getView().findViewById(R.id.list)).getAdapter()).updateData(artistResults);
            ((AlbumGridAdapter) ((GridView) (adapter.getItem(3)).getView().findViewById(R.id.albumGrid)).getAdapter()).updateData(albumResults);
            ((GenreListAdapter) ((ListView) (adapter.getItem(4)).getView().findViewById(R.id.list)).getAdapter()).updateData(genreResults);
        }
    }
}
