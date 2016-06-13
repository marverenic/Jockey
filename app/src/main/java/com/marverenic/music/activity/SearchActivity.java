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

import rx.subjects.BehaviorSubject;

public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;

    private static String lastQuery = null;
    private SearchView searchView;
    private BehaviorSubject<String> mQueryObservable;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;

    private PlaylistSection mPlaylistSection;
    private SongSection mSongSection;
    private AlbumSection mAlbumSection;
    private ArtistSection mArtistSection;
    private GenreSection mGenreSection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        JockeyApplication.getComponent(this).inject(this);
        mQueryObservable = BehaviorSubject.create("");

        // Set up the RecyclerView's adapter
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        initAdapter();

        mQueryObservable
                .flatMap(query -> mPlaylistStore.searchForPlaylists(query))
                .compose(bindToLifecycle())
                .subscribe(playlists -> {
                    mPlaylistSection.setData(playlists);
                    mAdapter.notifyDataSetChanged();
                });

        mQueryObservable
                .flatMap(query -> mMusicStore.searchForSongs(query))
                .compose(bindToLifecycle())
                .subscribe(songs -> {
                    mSongSection.setData(songs);
                    mAdapter.notifyDataSetChanged();
                });

        mQueryObservable
                .flatMap(query -> mMusicStore.searchForAlbums(query))
                .compose(bindToLifecycle())
                .subscribe(albums -> {
                    mAlbumSection.setData(albums);
                    mAdapter.notifyDataSetChanged();
                });

        mQueryObservable
                .flatMap(query -> mMusicStore.searchForArtists(query))
                .compose(bindToLifecycle())
                .subscribe(artists -> {
                    mArtistSection.setData(artists);
                    mAdapter.notifyDataSetChanged();
                });

        mQueryObservable
                .flatMap(query -> mMusicStore.searchForGenres(query))
                .compose(bindToLifecycle())
                .subscribe(genres -> {
                    mGenreSection.setData(genres);
                    mAdapter.notifyDataSetChanged();
                });

        handleIntent(getIntent());
    }

    private void initAdapter() {
        mPlaylistSection = new PlaylistSection(Collections.emptyList());
        mSongSection = new SongSection(this, Collections.emptyList());
        mAlbumSection = new AlbumSection(Collections.emptyList());
        mArtistSection = new ArtistSection(this, Collections.emptyList());
        mGenreSection = new GenreSection(this, Collections.emptyList());

        mAdapter = new HeterogeneousAdapter()
                .addSection(
                        new HeaderSection(getString(R.string.header_playlists), PlaylistSection.ID))
                .addSection(mPlaylistSection)
                .addSection(new HeaderSection(getString(R.string.header_songs), SongSection.ID))
                .addSection(mSongSection)
                .addSection(new HeaderSection(getString(R.string.header_albums), AlbumSection.ID))
                .addSection(mAlbumSection)
                .addSection(new HeaderSection(getString(R.string.header_artists), ArtistSection.ID))
                .addSection(mArtistSection)
                .addSection(new HeaderSection(getString(R.string.header_genres), GenreSection.ID))
                .addSection(mGenreSection);

        mAdapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                return (lastQuery == null || lastQuery.isEmpty())
                        ? ""
                        : getString(R.string.empty_search);
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mAdapter.getItemViewType(position) == AlbumSection.ID) {
                    return 1;
                }
                return numColumns;
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);

        // Add item decorations
        mRecyclerView.addItemDecoration(new GridSpacingDecoration(
                (int) getResources().getDimension(R.dimen.grid_margin),
                numColumns, AlbumSection.ID));
        mRecyclerView.addItemDecoration(
                new BackgroundDecoration(Themes.getBackgroundElevated(), R.id.subheaderFrame));
        mRecyclerView.addItemDecoration(
                new DividerDecoration(this,
                        R.id.albumInstance, R.id.subheaderFrame, R.id.empty_layout));
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

    private void search(String query) {
        if (!mQueryObservable.getValue().equals(query)) {
            mQueryObservable.onNext(query);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private List<Playlist> getPlaylistResults() {
        return mPlaylistSection.getData();
    }

    private List<Song> getSongResults() {
        return mSongSection.getData();
    }

    private List<Artist> getArtistResults() {
        return mArtistSection.getData();
    }

    private List<Album> getAlbumResults() {
        return mAlbumSection.getData();
    }

    private List<Genre> getGenreResults() {
        return mGenreSection.getData();
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
                if (!getPlaylistResults().isEmpty()
                        && (focus.equals(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
                        || (getGenreResults().isEmpty() && getSongResults().isEmpty()))) {

                    // If there is a playlist with this exact name, use it, otherwise fallback
                    // to the first result
                    Playlist playlist = getPlaylistResults().get(0);
                    for (Playlist p : getPlaylistResults()) {
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
                else if (!getArtistResults().isEmpty()
                        && focus.equals(MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    // If one or more artists with this name exist, play songs by all of them
                    // (Ideally this only includes collaborating artists and keeps
                    // the search relevant)
                    if (!getArtistResults().isEmpty()) {
                        ArrayList<Song> songs = new ArrayList<>();
                        for (Artist a : getArtistResults()) {
                            songs.addAll(Library.getArtistSongEntries(a));
                        }

                        PlayerController.setQueue(songs, 0);
                        PlayerController.begin();
                    }
                }
                /** ALBUMS */
                else if (!getAlbumResults().isEmpty()
                        && focus.equals(MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    if (!getAlbumResults().isEmpty()) {
                        // If albums with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Album album = getAlbumResults().get(0);
                        for (Album a : getAlbumResults()) {
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
                else if (!getGenreResults().isEmpty()
                        && (focus.equals(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
                        || getSongResults().isEmpty())) {

                    if (!getGenreResults().isEmpty()) {
                        // If genres with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Genre genre = getGenreResults().get(0);
                        for (Genre g : getGenreResults()) {
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
                    if (!getSongResults().isEmpty()) {
                        PlayerController.setQueue(getSongResults(), 0);
                        PlayerController.begin();
                    }
                }
            }
        }
    }
}
