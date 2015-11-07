package com.marverenic.music.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.marverenic.music.Library;
import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.AlbumViewHolder;
import com.marverenic.music.instances.viewholder.ArtistViewHolder;
import com.marverenic.music.instances.viewholder.GenreViewHolder;
import com.marverenic.music.instances.viewholder.HeaderViewHolder;
import com.marverenic.music.instances.viewholder.PlaylistViewHolder;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;

import java.util.ArrayList;
import java.util.Collections;

public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    private static String lastQuery = null;
    private SearchView searchView;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance);

        // Set up the RecyclerView's adapter
        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        adapter = new Adapter();
        list.setAdapter(adapter);

        // Calculate the number of columns that can fit on the screen
        final short screenWidth = (short) getResources().getConfiguration().screenWidthDp;
        final float density = getResources().getDisplayMetrics().density;
        final short globalPadding = (short) (getResources().getDimension(R.dimen.global_padding) / density);
        final short minWidth = (short) (getResources().getDimension(R.dimen.grid_width) / density);
        final short gridPadding = (short) (getResources().getDimension(R.dimen.grid_margin) / density);

        short availableWidth = (short) (screenWidth - 2 * globalPadding);
        final int numColumns = (availableWidth) / (minWidth + 2 * gridPadding);

        // Setup the layout manager
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == Adapter.ALBUM_VIEW) return 1;
                return numColumns;
            }
        };
        GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        list.setLayoutManager(layoutManager);

        // Add item decorations
        list.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns, Adapter.ALBUM_VIEW));
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this, new int[]{R.id.albumInstance, R.id.subheaderFrame}));

        handleIntent(getIntent());
    }

    @Override
    public void onResume(){
        super.onResume();
        Library.addPlaylistListener(adapter);
    }

    @Override
    public void onPause(){
        super.onPause();
        Library.removePlaylistListener(adapter);
    }

    @Override
    public void onBackPressed(){
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

        if (lastQuery != null && !lastQuery.isEmpty()){
            searchView.setQuery(lastQuery, true);
        }
        else{
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
        adapter.search(query);
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.search(newText);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){
        if (intent != null) {
            // Handle standard searches
            if (Intent.ACTION_SEARCH.equals(intent.getAction())
                    || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(intent.getAction())){
                adapter.search(intent.getStringExtra(SearchManager.QUERY));
            }

            /** Handle play from search actions */
            else if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())){

                adapter.search(intent.getStringExtra(SearchManager.QUERY));
                final String focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);

                /** PLAYLISTS */
                // If there are playlists that match this search, and either the specified focus is
                // playlists or there are only playlist results, then play the appropriate result
                if (!adapter.playlistResults.isEmpty() && (focus.equals(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE) ||
                        (adapter.genreResults.isEmpty() && adapter.songResults.isEmpty()))) {

                    // If there is a playlist with this exact name, use it, otherwise fallback to the first result
                    Playlist playlist = adapter.playlistResults.get(0);
                    for (Playlist p : adapter.playlistResults) {
                        if (p.playlistName.equalsIgnoreCase(intent.getStringExtra(SearchManager.QUERY))) {
                            playlist = p;
                            break;
                        }
                    }
                    PlayerController.setQueue(Library.getPlaylistEntries(this, playlist), 0);
                    PlayerController.begin();
                }
                /** ARTISTS **/
                else if (!adapter.artistResults.isEmpty() && focus.equals(MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    // If one or more artists with this name exist, play songs by all of them
                    // (Ideally this only includes collaborating artists and keeps the search relevant)
                    if (adapter.artistResults.size() > 0) {
                        ArrayList<Song> songs = new ArrayList<>();
                        for (Artist a : adapter.artistResults)
                            songs.addAll(Library.getArtistSongEntries(a));

                        PlayerController.setQueue(songs, 0);
                        PlayerController.begin();
                    }
                }
                /** ALBUMS */
                else if (!adapter.albumResults.isEmpty() && focus.equals(MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    if (adapter.albumResults.size() > 0) {
                        // If albums with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Album album = adapter.albumResults.get(0);
                        for (Album a : adapter.albumResults) {
                            if (a.albumName.equalsIgnoreCase(intent.getStringExtra(SearchManager.QUERY))) {
                                album = a;
                                break;
                            }
                        }
                        PlayerController.setQueue(Library.getAlbumEntries(album), 0);
                        PlayerController.begin();
                    }
                }
                /** GENRES */
                else if (!adapter.genreResults.isEmpty() && (focus.equals(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
                        || adapter.songResults.isEmpty())) {

                    if (adapter.genreResults.size() > 0) {
                        // If genres with this name exist, look for an exact match
                        // If we find one then use it, otherwise fallback to the first result
                        Genre genre = adapter.genreResults.get(0);
                        for (Genre g : adapter.genreResults) {
                            if (g.genreName.equalsIgnoreCase(intent.getStringExtra(SearchManager.QUERY))) {
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
                    if (adapter.songResults.size() > 0) {
                        PlayerController.setQueue(adapter.songResults, 0);
                        PlayerController.begin();
                    }
                }
            }
        }
    }

    public class Adapter extends RecyclerView.Adapter implements Library.PlaylistChangeListener{

        private final ArrayList<Playlist> playlistResults = new ArrayList<>();
        private final ArrayList<Song> songResults = new ArrayList<>();
        private final ArrayList<Album> albumResults = new ArrayList<>();
        private final ArrayList<Artist> artistResults = new ArrayList<>();
        private final ArrayList<Genre> genreResults = new ArrayList<>();

        private final int[] HEADERS = {
                R.string.header_playlists,
                R.string.header_songs,
                R.string.header_albums,
                R.string.header_artists,
                R.string.header_genres
        };

        private static final int HEADER_VIEW = 0;
        private static final int PLAYLIST_VIEW = 1;
        private static final int SONG_VIEW = 2;
        private static final int ALBUM_VIEW = 3;
        private static final int ARTIST_VIEW = 4;
        private static final int GENRE_VIEW = 5;

        public void search(CharSequence searchInput){
            String query = searchInput.toString().trim().toLowerCase();
            lastQuery = searchInput.toString();

            albumResults.clear();
            artistResults.clear();
            genreResults.clear();
            songResults.clear();
            playlistResults.clear();

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
                            Genre g = Library.findGenreById(s.genreId);
                            if (!genreResults.contains(g)) genreResults.add(g);
                        }

                        Album thisAlbum = Library.findAlbumById(s.albumId);
                        if(!albumResults.contains(thisAlbum)) albumResults.add(thisAlbum);

                        Artist thisArtist = Library.findArtistById(s.artistId);
                        if(!artistResults.contains(thisArtist)) artistResults.add(thisArtist);
                    }
                }

                for(Playlist p : Library.getPlaylists()){
                    if (p.playlistName.toLowerCase().contains(query)) {
                        playlistResults.add(p);
                    }
                }

                Collections.sort(genreResults);
            }

            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case HEADER_VIEW:
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.subheader, parent, false));
                case PLAYLIST_VIEW:
                    return new PlaylistViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_playlist, parent, false));
                case SONG_VIEW:
                    return new SongViewHolder(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.instance_song, parent, false),
                            songResults);
                case ALBUM_VIEW:
                    return new AlbumViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_album, parent, false));
                case ARTIST_VIEW:
                    return new ArtistViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_artist, parent, false));
                case GENRE_VIEW:
                    return new GenreViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_genre, parent, false));
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

            switch (getItemViewType(position)) {
                case HEADER_VIEW:
                    ((HeaderViewHolder) viewHolder)
                            .update(getResources().getString(HEADERS[getTypeIndex(position)]));
                    break;
                case PLAYLIST_VIEW:
                    ((PlaylistViewHolder) viewHolder)
                            .update(playlistResults.get(getTypeIndex(position)));
                    break;
                case SONG_VIEW:
                    int index = getTypeIndex(position);
                    ((SongViewHolder) viewHolder)
                            .update(songResults.get(index), index);
                    break;
                case ALBUM_VIEW:
                    ((AlbumViewHolder) viewHolder)
                            .update(albumResults.get(getTypeIndex(position)));
                    break;
                case ARTIST_VIEW:
                    ((ArtistViewHolder) viewHolder)
                            .update(artistResults.get((getTypeIndex(position))));
                    break;
                case GENRE_VIEW:
                    ((GenreViewHolder) viewHolder)
                            .update(genreResults.get(getTypeIndex(position)));
            }
        }

        /**
         * Look up the index of the applicable data type for a specified index in the adapter
         * @param position The index of the adapter to lookup a data entry index
         * @return The index of this view's corresponding data array for the given position
         */
        private int getTypeIndex(int position) {
            if (!playlistResults.isEmpty() && position <= playlistResults.size()){
                if (position == 0) return 0;
                else return position - 1;
            }

            //The number of views above the current section. This value is incremented later in the method
            int leadingViewCount = (playlistResults.isEmpty()? 0 : playlistResults.size() + 1);
            if (!songResults.isEmpty() && position <= songResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return 1;
                else return position - 1 - leadingViewCount;
            }

            leadingViewCount += (songResults.isEmpty()? 0 : songResults.size() + 1);
            if (!albumResults.isEmpty() && position <= albumResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return 2;
                else return position - 1 - leadingViewCount;
            }

            leadingViewCount += (albumResults.isEmpty()? 0 : albumResults.size() + 1);
            if (!artistResults.isEmpty() && position <= artistResults.size() + leadingViewCount){
                if (position == leadingViewCount) return 3;
                else return position - 1 - leadingViewCount;
            }

            leadingViewCount += (artistResults.isEmpty()? 0 : artistResults.size() + 1);
            if (!genreResults.isEmpty() && position <= genreResults.size() + leadingViewCount){
                if (position == leadingViewCount) return 4;
                else return position - 1 - leadingViewCount;
            }

            return 0;
        }

        @Override
        public int getItemCount() {
            return (playlistResults.isEmpty() ? 0 : 1 + playlistResults.size())
                    + (songResults.isEmpty() ? 0 : 1 + songResults.size())
                    + (albumResults.isEmpty() ? 0 : 1 + albumResults.size())
                    + (artistResults.isEmpty() ? 0 : 1 + artistResults.size())
                    + (genreResults.isEmpty() ? 0 : 1 + genreResults.size());
        }

        @Override
        public int getItemViewType(int position){
            if (!playlistResults.isEmpty() && position <= playlistResults.size()){
                if (position == 0) return HEADER_VIEW;
                else return PLAYLIST_VIEW;
            }

            //The number of views above the current section. This value is incremented later in the method
            int leadingViewCount = (playlistResults.isEmpty()? 0 : playlistResults.size() + 1);
            if (!songResults.isEmpty() && position <= songResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return HEADER_VIEW;
                else return SONG_VIEW;
            }

            leadingViewCount += (songResults.isEmpty()? 0 : songResults.size() + 1);
            if (!albumResults.isEmpty() && position <= albumResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return HEADER_VIEW;
                else return ALBUM_VIEW;
            }

            leadingViewCount += (albumResults.isEmpty()? 0 : albumResults.size() + 1);
            if (!artistResults.isEmpty() && position <= artistResults.size() + leadingViewCount){
                if (position == leadingViewCount) return HEADER_VIEW;
                else return ARTIST_VIEW;
            }

            leadingViewCount += (artistResults.isEmpty()? 0 : artistResults.size() + 1);
            if (!genreResults.isEmpty() && position <= genreResults.size() + leadingViewCount){
                if (position == leadingViewCount) return HEADER_VIEW;
                else return GENRE_VIEW;
            }

            return -1;
        }

        @Override
        public void onPlaylistRemoved(Playlist removed) {
            final int index = playlistResults.indexOf(removed);
            if (index != -1){
                playlistResults.remove(index);
                if (playlistResults.isEmpty()){
                    // Remove the header as well as the entry if there aren't any playlist results
                    notifyItemRangeRemoved(0,2);
                }
                else{
                    notifyItemRemoved(index + 1);
                }
            }
        }

        @Override
        public void onPlaylistAdded(Playlist added) {
            if (lastQuery != null && lastQuery.length() > 0 && added.playlistName.toLowerCase().contains(lastQuery.toLowerCase().trim())){
                playlistResults.add(added);
                Collections.sort(playlistResults);

                if (playlistResults.size() == 1){
                    // If we didn't have any results before, then we need to add the header as well
                    notifyItemRangeInserted(0, 2);
                }
                else {
                    notifyItemInserted(playlistResults.indexOf(added));
                }
            }
        }
    }

}
