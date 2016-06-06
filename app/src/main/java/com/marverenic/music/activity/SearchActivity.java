package com.marverenic.music.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.AlbumSection;
import com.marverenic.music.instances.section.ArtistSection;
import com.marverenic.music.instances.section.BasicEmptyState;
import com.marverenic.music.instances.section.GenreSection;
import com.marverenic.music.instances.section.HeaderSection;
import com.marverenic.music.instances.section.PlaylistSection;
import com.marverenic.music.instances.section.SongSection;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;

    private static String lastQuery = null;
    private SearchView searchView;
    private HeterogeneousAdapter adapter;

    private final List<Playlist> playlistResults = new ArrayList<>();
    private final List<Song> songResults = new ArrayList<>();
    private final List<Album> albumResults = new ArrayList<>();
    private final List<Artist> artistResults = new ArrayList<>();
    private final List<Genre> genreResults = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        JockeyApplication.getComponent(this).inject(this);

        // Set up the RecyclerView's adapter
        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        adapter = new HeterogeneousAdapter()
                .addSection(
                        new HeaderSection(getString(R.string.header_playlists), PlaylistSection.ID))
                .addSection(new PlaylistSection(playlistResults))
                .addSection(new HeaderSection(getString(R.string.header_songs), SongSection.ID))
                .addSection(new SongSection(songResults))
                .addSection(new HeaderSection(getString(R.string.header_albums), AlbumSection.ID))
                .addSection(new AlbumSection(albumResults))
                .addSection(new HeaderSection(getString(R.string.header_artists), ArtistSection.ID))
                .addSection(new ArtistSection(artistResults))
                .addSection(new HeaderSection(getString(R.string.header_genres), GenreSection.ID))
                .addSection(new GenreSection(genreResults));

        adapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                return (lastQuery == null || lastQuery.isEmpty())
                        ? ""
                        : getString(R.string.empty_search);
            }
        });

        list.setAdapter(adapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == AlbumSection.ID) {
                    return 1;
                }
                return numColumns;
            }
        });
        list.setLayoutManager(layoutManager);

        // Add item decorations
        list.addItemDecoration(new GridSpacingDecoration(
                (int) getResources().getDimension(R.dimen.grid_margin),
                numColumns, AlbumSection.ID));
        list.addItemDecoration(
                new BackgroundDecoration(Themes.getBackgroundElevated(), R.id.subheaderFrame));
        list.addItemDecoration(
                new DividerDecoration(this,
                        R.id.albumInstance, R.id.subheaderFrame, R.id.empty_layout));

        handleIntent(getIntent());
    }

    @Override
    public void onBackPressed() {
        lastQuery = null;
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setIconified(false);

        if (lastQuery != null && !lastQuery.isEmpty()) {
            searchView.setQuery(lastQuery, true);
        } else {
            searchView.requestFocus();
        }
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
    public boolean onQueryTextSubmit(String query) {
        search(query);
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search(newText);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            // Handle standard searches
            if (Intent.ACTION_SEARCH.equals(intent.getAction())
                    || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(intent.getAction())) {
                search(intent.getStringExtra(SearchManager.QUERY));
            }

            /** Handle play from search actions */
            else if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {

                search(intent.getStringExtra(SearchManager.QUERY));
                final String focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);

                /** PLAYLISTS */
                // If there are playlists that match this search, and either the specified focus is
                // playlists or there are only playlist results, then play the appropriate result
                if (!playlistResults.isEmpty()
                        && (focus.equals(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
                        || (genreResults.isEmpty() && songResults.isEmpty()))) {

                    // If there is a playlist with this exact name, use it, otherwise fallback
                    // to the first result
                    Playlist playlist = playlistResults.get(0);
                    for (Playlist p : playlistResults) {
                        if (p.getPlaylistName().equalsIgnoreCase(
                                intent.getStringExtra(SearchManager.QUERY))) {
                            playlist = p;
                            break;
                        }
                    }
                    PlayerController.setQueue(Library.getPlaylistEntries(this, playlist), 0);
                    PlayerController.begin();
                }
                /** ARTISTS **/
                else if (!artistResults.isEmpty()
                        && focus.equals(MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    // If one or more artists with this name exist, play songs by all of them
                    // (Ideally this only includes collaborating artists and keeps
                    // the search relevant)
                    if (!artistResults.isEmpty()) {
                        ArrayList<Song> songs = new ArrayList<>();
                        for (Artist a : artistResults) {
                            songs.addAll(Library.getArtistSongEntries(a));
                        }

                        PlayerController.setQueue(songs, 0);
                        PlayerController.begin();
                    }
                }
                /** ALBUMS */
                else if (!albumResults.isEmpty()
                        && focus.equals(MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    if (!albumResults.isEmpty()) {
                        // If albums with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Album album = albumResults.get(0);
                        for (Album a : albumResults) {
                            if (a.getAlbumName().equalsIgnoreCase(
                                    intent.getStringExtra(SearchManager.QUERY))) {
                                album = a;
                                break;
                            }
                        }
                        PlayerController.setQueue(Library.getAlbumEntries(album), 0);
                        PlayerController.begin();
                    }
                }
                /** GENRES */
                else if (!genreResults.isEmpty()
                        && (focus.equals(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
                        || songResults.isEmpty())) {

                    if (!genreResults.isEmpty()) {
                        // If genres with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Genre genre = genreResults.get(0);
                        for (Genre g : genreResults) {
                            if (g.getGenreName().equalsIgnoreCase(
                                    intent.getStringExtra(SearchManager.QUERY))) {
                                genre = g;
                                break;
                            }
                        }
                        PlayerController.setQueue(Library.getGenreEntries(genre), 0);
                        PlayerController.begin();
                    }
                }
                /** SONGS */
                // If we can't figure out what's going on (And I can understand why) or if
                // the focus is songs, then just play all of the song results
                else {
                    if (!songResults.isEmpty()) {
                        PlayerController.setQueue(songResults, 0);
                        PlayerController.begin();
                    }
                }
            }
        }
    }

    public void search(CharSequence searchInput) {
        String query = searchInput.toString().trim().toLowerCase();
        lastQuery = searchInput.toString();

        clearAll();

        if (!query.isEmpty()) {
            searchAlbums(query);
            searchArtists(query);
            searchGenres(query);
            searchSongs(query);
            searchPlaylists(query);
        }

        adapter.notifyDataSetChanged();
    }

    private void clearAll() {
        albumResults.clear();
        artistResults.clear();
        genreResults.clear();
        songResults.clear();
        playlistResults.clear();
    }

    private void searchAlbums(String query) {
        for (Album a : Library.getAlbums()) {
            if (a.getAlbumName().toLowerCase().contains(query)
                    || a.getArtistName().toLowerCase().contains(query)) {
                albumResults.add(a);
            }
        }
    }

    private void searchArtists(String query) {
        for (Artist a : Library.getArtists()) {
            if (a.getArtistName().toLowerCase().contains(query)) {
                artistResults.add(a);
            }
        }
    }

    private void searchGenres(String query) {
        for (Genre g : Library.getGenres()) {
            if (g.getGenreName().toLowerCase().contains(query)) {
                genreResults.add(g);
            }
        }
    }

    private void searchSongs(String query) {
        for (Song s : Library.getSongs()) {
            if (s.getSongName().toLowerCase().contains(query)
                    || s.getArtistName().toLowerCase().contains(query)
                    || s.getAlbumName().toLowerCase().contains(query)) {
                songResults.add(s);

                if (s.getGenreId() != -1) {
                    Genre currGenre = Library.findGenreById(s.getGenreId());
                    if (currGenre != null && !genreResults.contains(currGenre)) {
                        genreResults.add(currGenre);
                    }
                }

                Album currAlbum = Library.findAlbumById(s.getAlbumId());
                if (currAlbum != null && !albumResults.contains(currAlbum)) {
                    albumResults.add(currAlbum);
                }

                Artist currArtist = Library.findArtistById(s.getArtistId());
                if (currArtist != null && !artistResults.contains(currArtist)) {
                    artistResults.add(currArtist);
                }
            }
        }

        Collections.sort(genreResults);
        Collections.sort(albumResults);
        Collections.sort(artistResults);
    }

    private void searchPlaylists(String query) {
        for (Playlist p : Library.getPlaylists()) {
            if (p.getPlaylistName().toLowerCase().contains(query)) {
                playlistResults.add(p);
            }
        }
    }
}
