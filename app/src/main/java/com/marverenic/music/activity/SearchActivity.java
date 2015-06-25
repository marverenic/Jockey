package com.marverenic.music.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.Library;
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

public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener {

    private static String lastQuery = null;
    private Adapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

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
        spanSizeLookup.setSpanIndexCacheEnabled(true); // for performance
        GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        list.setLayoutManager(layoutManager);

        // Add item decorations
        list.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns, Adapter.ALBUM_VIEW));
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this, new int[]{R.id.albumInstance, R.id.subheaderFrame}));

        // Handle a pending search
        Intent parentIntent = getIntent();
        if (parentIntent != null && Intent.ACTION_SEARCH.equals(parentIntent.getAction()) && parentIntent.hasExtra(SearchManager.QUERY)) {
            adapter.search(parentIntent.getStringExtra(SearchManager.QUERY).toLowerCase());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setIconified(false);

        if (lastQuery != null && !lastQuery.isEmpty()){
            searchView.requestFocus();
            searchView.setQuery(lastQuery, true);
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
        if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction()) && intent.hasExtra(SearchManager.QUERY)) {
            adapter.search(intent.getStringExtra(SearchManager.QUERY).toLowerCase());
        }
    }

    public class Adapter extends RecyclerView.Adapter implements PlaylistViewHolder.OnDeleteCallback{

        private final ArrayList<Playlist> playlistResults = new ArrayList<>();
        private final ArrayList<Song> songResults = new ArrayList<>();
        private final ArrayList<Album> albumResults = new ArrayList<>();
        private final ArrayList<Artist> artistResults = new ArrayList<>();
        private final ArrayList<Genre> genreResults = new ArrayList<>();

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

                Library.sortGenreList(genreResults);
            }

            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case HEADER_VIEW:
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.subheader, parent, false));
                case PLAYLIST_VIEW:
                    PlaylistViewHolder playlistViewHolder = new PlaylistViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_playlist, parent, false));
                    playlistViewHolder.setRemoveCallback(this);
                    return playlistViewHolder;
                case SONG_VIEW:
                    SongViewHolder songViewHolder = new SongViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false));
                    songViewHolder.setSongList(songResults);
                    return songViewHolder;
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
            Object item = getItem(position);

            if (item instanceof String){
                ((HeaderViewHolder) viewHolder).update((String) item);
            }
            else if (item instanceof Playlist){
                ((PlaylistViewHolder) viewHolder).update((Playlist) item);
            }
            else if (item instanceof Song){
                ((SongViewHolder) viewHolder).update((Song) item);
            }
            else if (item instanceof Album){
                ((AlbumViewHolder) viewHolder).update((Album) item);
            }
            else if (item instanceof Artist){
                ((ArtistViewHolder) viewHolder).update((Artist) item);
            }
            else if (item instanceof Genre){
                ((GenreViewHolder) viewHolder).update((Genre) item);
            }
        }

        public Object getItem(int position){
            if (!playlistResults.isEmpty() && position <= playlistResults.size()){
                if (position == 0) return SearchActivity.this.getResources().getString(R.string.header_playlists);
                else return playlistResults.get(position - 1);
            }

            //The number of views above the current section. This value is incremented later in the method
            int leadingViewCount = (playlistResults.isEmpty()? 0 : playlistResults.size() + 1);
            if (!songResults.isEmpty() && position <= songResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return SearchActivity.this.getResources().getString(R.string.header_songs);
                else return songResults.get(position - 1 - leadingViewCount);
            }

            leadingViewCount += (songResults.isEmpty()? 0 : songResults.size() + 1);
            if (!albumResults.isEmpty() && position <= albumResults.size() + leadingViewCount) {
                if (position == leadingViewCount) return SearchActivity.this.getResources().getString(R.string.header_albums);
                else return albumResults.get(position - 1 - leadingViewCount);
            }

            leadingViewCount += (albumResults.isEmpty()? 0 : albumResults.size() + 1);
            if (!artistResults.isEmpty() && position <= artistResults.size() + leadingViewCount){
                if (position == leadingViewCount) return SearchActivity.this.getResources().getString(R.string.header_artists);
                else return artistResults.get(position - 1 - leadingViewCount);
            }

            leadingViewCount += (artistResults.isEmpty()? 0 : artistResults.size() + 1);
            if (!genreResults.isEmpty() && position <= genreResults.size() + leadingViewCount){
                if (position == leadingViewCount) return SearchActivity.this.getResources().getString(R.string.header_genres);
                else return genreResults.get(position - 1 - leadingViewCount);
            }

            return null;
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
            Object item = getItem(position);

            if (item instanceof String) return HEADER_VIEW;
            if (item instanceof Playlist) return PLAYLIST_VIEW;
            if (item instanceof Song) return SONG_VIEW;
            if (item instanceof Album) return ALBUM_VIEW;
            if (item instanceof Artist) return ARTIST_VIEW;
            if (item instanceof Genre) return GENRE_VIEW;

            return -1;
        }

        @Override
        public void onPlaylistDelete(RecyclerView.ViewHolder viewHolder, final Playlist removed) {

            final ArrayList<Song> contents = Library.getPlaylistEntries(SearchActivity.this, removed);
            final int index = playlistResults.indexOf(removed);
            final int playlistCount = playlistResults.size();

            Snackbar snackbar = Snackbar.make(
                    findViewById(R.id.list),
                    String.format(getResources().getString(R.string.message_removed_playlist), removed),
                    Snackbar.LENGTH_LONG);

            snackbar.setAction("Undo", new View.OnClickListener() { // TODO String Resource
                @Override
                public void onClick(View v) {
                    Playlist recreated = Library.createPlaylist(SearchActivity.this, removed.playlistName, contents);
                    playlistResults.add(recreated);
                    Library.sortPlaylistList(playlistResults);

                    if (playlistCount == 1) notifyItemInserted(0); // Add the playlist header
                    notifyItemInserted(index);
                }
            });

            Library.removePlaylist(SearchActivity.this, removed);
            notifyItemRemoved(viewHolder.getAdapterPosition());
            snackbar.show();

            playlistResults.remove(index);
            notifyItemRemoved(index);
            if (playlistCount == 1) notifyItemRemoved(0); // Remove the playlist header
        }
    }

}
