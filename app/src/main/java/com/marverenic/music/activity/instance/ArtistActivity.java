package com.marverenic.music.activity.instance;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.Library;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.AlbumViewHolder;
import com.marverenic.music.instances.viewholder.HeaderViewHolder;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.utils.Fetch;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.umass.lastfm.ImageSize;

public class ArtistActivity extends BaseActivity {

    public static final String ARTIST_EXTRA = "artist";
    private Artist reference;
    private ArrayList<Album> albums;
    private ArrayList<Song> songs;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_artwork);

        reference = getIntent().getParcelableExtra(ARTIST_EXTRA);
        albums = Library.getArtistAlbumEntries(reference);
        songs = Library.getArtistSongEntries(reference);

        // Sort the album list chronologically if all albums have years, otherwise sort alphabetically
        boolean allEntriesHaveYears = true;
        int i = 0;
        while (i < albums.size() && allEntriesHaveYears){
            if (albums.get(i).year == null || albums.get(i).year.equals(""))
                allEntriesHaveYears = false;
            i++;
        }

        if (allEntriesHaveYears) {
            Collections.sort(albums, new Comparator<Album>() {
                @Override
                public int compare(Album a1, Album a2) {
                    return a2.year.compareTo(a1.year);
                }
            });
        }
        else {
            Library.sortAlbumList(albums);
        }

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(reference.artistName);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        final Adapter adapter = new Adapter();
        recyclerView.setAdapter(adapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(this);

        // Setup the GridLayoutManager
        final GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Albums fill one column, all other view types fill the available width
                if (adapter.getItemViewType(position) == Adapter.ALBUM_INSTANCE) return 1;
                else return numColumns;
            }
        };

        spanSizeLookup.setSpanIndexCacheEnabled(true); // For performance

        // Attach the GridLayoutManager
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(layoutManager);

        // Add decorations
        recyclerView.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns, Adapter.ALBUM_INSTANCE));
        recyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated(), new int[]{R.id.infoCard}));
        recyclerView.addItemDecoration(new DividerDecoration(this, new int[]{R.id.infoCard, R.id.albumInstance, R.id.subheaderFrame}));
    }

    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private boolean hasBio = true;

        public static final int BIO_VIEW = 0;
        public static final int HEADER_VIEW = 1;
        public static final int ALBUM_INSTANCE = 2;
        public static final int SONG_INSTANCE = 3;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case BIO_VIEW:
                    return new BioViewHolder(LayoutInflater.from(ArtistActivity.this).inflate(R.layout.instance_artist_bio, parent, false), this);
                case HEADER_VIEW:
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.subheader, parent, false));
                case ALBUM_INSTANCE:
                    return new AlbumViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_album, parent, false));
                case SONG_INSTANCE:
                    SongViewHolder viewHolder = new SongViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.instance_song, parent, false));
                    viewHolder.setSongList(songs);
                    return viewHolder;
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case BIO_VIEW:
                    // Don't update anything in this view. Since there's only one instance, the
                    // view will never be recycled
                    break;
                case HEADER_VIEW:
                    ((HeaderViewHolder) holder).update(
                            (position == (hasBio? 1 : 0))
                                    ? getResources().getString(R.string.header_albums)
                                    : getResources().getString(R.string.header_songs));
                    break;
                case ALBUM_INSTANCE:
                    ((AlbumViewHolder) holder).update(albums.get(position - (hasBio? 2 : 1)));
                    break;
                case SONG_INSTANCE:
                    ((SongViewHolder) holder).update(songs.get(position - albums.size() - (hasBio? 3 : 2)));
            }
        }

        @Override
        public int getItemCount() {
            return (hasBio? 1 : 0) + 2 + albums.size() + songs.size();
        }

        @Override
        public int getItemViewType(int position){
            if (hasBio && position == 0) return BIO_VIEW;
            else if (position == (hasBio? 1 : 0)) return HEADER_VIEW;
            else if (position <= albums.size() + (hasBio? 1 : 0)) return ALBUM_INSTANCE;
            else if (position == albums.size() + (hasBio? 2 : 1)) return HEADER_VIEW;
            else return SONG_INSTANCE;
        }

        public void hideBioCard(){
            hasBio = false;
            notifyItemRemoved(0);
        }
    }

    public class BioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Adapter adapter;

        private CardView cardView;
        private TextView bioText;
        private View itemView;
        private FrameLayout lfmButton;
        private String artistURL;

        public BioViewHolder(View itemView, Adapter adapter) {
            super(itemView);
            this.itemView = itemView;
            this.adapter = adapter;

            cardView = (CardView) itemView.findViewById(R.id.infoCard);
            bioText = (TextView) itemView.findViewById(R.id.infoText);
            lfmButton = (FrameLayout) itemView.findViewById(R.id.openLFMButton);
            lfmButton.setOnClickListener(this);

            // Fetch the Artist Bio
            //TODO get suggested artists
            new AsyncTask<Artist, Void, de.umass.lastfm.Artist>() {
                @Override
                protected de.umass.lastfm.Artist doInBackground(Artist... params) {
                    return Fetch.fetchArtistBio(ArtistActivity.this, params[0].artistName);
                }

                @Override
                protected void onPostExecute(de.umass.lastfm.Artist artist) {
                    super.onPostExecute(artist);
                    setData(artist);

                    if (artist != null) {
                        // Set header image
                        String URL = artist.getImageURL(ImageSize.MEGA);

                        if (URL.trim().length() != 0) {
                            Picasso.with(ArtistActivity.this).load(URL)
                                    .placeholder(R.drawable.art_default_xl)
                                    .error(R.drawable.art_default_xl)
                                    .into((ImageView) ArtistActivity.this.findViewById(R.id.backdrop));
                            return;
                        }
                    }
                    ((ImageView) ArtistActivity.this.findViewById(R.id.backdrop)).setImageResource(R.drawable.art_default_xl);
                }
            }.execute(reference);
        }

        public void setData(de.umass.lastfm.Artist artist) {
            if (artist == null){
                adapter.hideBioCard();
                return;
            }

            String[] tags = new String[artist.getTags().size()];
            artist.getTags().toArray(tags);

            String tagList = ""; // A list of tags to display in the card

            if (tags.length > 0) {
                // Capitalize the first letter of the tag
                tagList = tags[0].substring(0,1).toUpperCase() + tags[0].substring(1);
                // Add up to 4 more tags (separated by commas)
                int tagCount = (tags.length < 5)? tags.length : 5;
                for (int i = 1; i < tagCount; i++){
                    tagList += ", " + tags[i].substring(0,1).toUpperCase() + tags[i].substring(1);
                }
            }

            String summary = Html.fromHtml(artist.getWikiSummary()).toString();
            if (summary.length() > 0) {
                // Trim the "read more" attribution since there's already a button linking to Last.fm
                summary = summary.substring(0, summary.length() - " Read more about  on Last.fm.".length() - artist.getName().length() - 1);
                if (tagList.length() > 0) tagList += " - ";
            }

            bioText.setText(tagList + summary);
            artistURL = artist.getUrl();
        }

        @Override
        public void onClick(View v) {
            if (v.equals(lfmButton)){
                Intent openLFMIntent = new Intent(Intent.ACTION_VIEW);
                if (artistURL == null) openLFMIntent.setData(Uri.parse("http://www.last.fm/home"));
                else openLFMIntent.setData(Uri.parse(artistURL));
                startActivity(openLFMIntent);
            }
        }
    }

}
