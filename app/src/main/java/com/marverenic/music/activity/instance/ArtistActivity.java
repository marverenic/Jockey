package com.marverenic.music.activity.instance;

import android.content.Context;
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
import com.marverenic.music.view.MaterialProgressDrawable;
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
                // Albums & related artists fill one column, all other view types fill the available width
                if (adapter.getItemViewType(position) == Adapter.ALBUM_INSTANCE) return 1;
                else if (adapter.getItemViewType(position) == Adapter.RELATED_ARTIST) return 1;
                else return numColumns;
            }
        };

        spanSizeLookup.setSpanIndexCacheEnabled(true); // For performance

        // Attach the GridLayoutManager
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        recyclerView.setLayoutManager(layoutManager);

        // Add decorations
        recyclerView.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.grid_margin), numColumns, Adapter.ALBUM_INSTANCE));
        recyclerView.addItemDecoration(new GridSpacingDecoration((int) getResources().getDimension(R.dimen.card_margin), numColumns, Adapter.RELATED_ARTIST));
        recyclerView.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated(), new int[]{R.id.loadingView, R.id.infoCard, R.id.relatedCard}));
        recyclerView.addItemDecoration(new DividerDecoration(this, new int[]{R.id.infoCard, R.id.albumInstance, R.id.subheaderFrame, R.id.relatedCard}));
    }

    /**
     * Adapter class for the Artist RecyclerView
     */
    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private boolean hasBio = true;
        private de.umass.lastfm.Artist[] relatedArtists = new de.umass.lastfm.Artist[0];
        private de.umass.lastfm.Artist artist;

        public static final int LOADING_BIO_VIEW = 0;
        public static final int BIO_VIEW = 1;
        public static final int RELATED_ARTIST = 2;
        public static final int HEADER_VIEW = 3;
        public static final int ALBUM_INSTANCE = 4;
        public static final int SONG_INSTANCE = 5;

        public Adapter(){
            // Fetch the Artist Bio
            new AsyncTask<Artist, Void, Void>() {
                @Override
                protected Void doInBackground(Artist... params) {
                    artist = Fetch.fetchArtistBio(ArtistActivity.this, params[0].artistName);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);

                    if (artist == null) {
                        hasBio = false;
                        notifyDataSetChanged();
                    }
                    else{
                        // Get related artists
                        relatedArtists = new de.umass.lastfm.Artist[artist.getSimilar().size()];
                        artist.getSimilar().toArray(relatedArtists);

                        notifyItemChanged(0);
                        notifyItemRangeInserted((hasBio) ? 1 : 0, relatedArtists.length);

                        // Set header image
                        String URL = artist.getImageURL(ImageSize.MEGA);

                        if (URL.trim().length() != 0) {
                            Picasso.with(ArtistActivity.this).load(URL)
                                    .placeholder(R.drawable.art_default_xl)
                                    .error(R.drawable.art_default_xl)
                                    .into((ImageView) findViewById(R.id.backdrop));
                        }
                    }
                }
            }.execute(reference);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType){
                case LOADING_BIO_VIEW:
                    return new LoadingViewHolder(LayoutInflater.from(ArtistActivity.this).inflate(R.layout.instance_loading, parent, false), this);
                case BIO_VIEW:
                    return new BioViewHolder(LayoutInflater.from(ArtistActivity.this).inflate(R.layout.instance_artist_bio, parent, false), this, artist);
                case RELATED_ARTIST:
                    return new SuggestedArtistHolder(LayoutInflater.from(ArtistActivity.this).inflate(R.layout.instance_artist_suggested, parent, false));
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
                // Don't update anything in the bio view or its loading view. Since there's only
                // one instance, the data will never become invalid
                case RELATED_ARTIST:
                    ((SuggestedArtistHolder) holder).update(relatedArtists[position - (hasBio? 1 : 0)]);
                    break;
                case HEADER_VIEW:
                    ((HeaderViewHolder) holder).update(
                            (position == (hasBio? relatedArtists.length + 1 : 0))
                                    ? getResources().getString(R.string.header_albums)
                                    : getResources().getString(R.string.header_songs));
                    break;
                case ALBUM_INSTANCE:
                    ((AlbumViewHolder) holder).update(albums.get(position - (hasBio ? 2 : 1) - relatedArtists.length));
                    break;
                case SONG_INSTANCE:
                    ((SongViewHolder) holder).update(songs.get(position - albums.size() - (hasBio? 3 : 2) - relatedArtists.length));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return (hasBio? 1 : 0) + relatedArtists.length + 2 + albums.size() + songs.size();
        }

        @Override
        public int getItemViewType(int position){
            if (hasBio && position == 0) return ((artist == null) ? LOADING_BIO_VIEW : BIO_VIEW);
            else if (position < relatedArtists.length + (hasBio? 1 : 0)) return RELATED_ARTIST;
            else if (position == relatedArtists.length + (hasBio? 1 : 0)) return HEADER_VIEW;
            else if (position <= albums.size() + relatedArtists.length + (hasBio? 1 : 0)) return ALBUM_INSTANCE;
            else if (position == albums.size() + relatedArtists.length + (hasBio? 2 : 1)) return HEADER_VIEW;
            else return SONG_INSTANCE;
        }
    }

    /**
     * Temporary ViewHolder for loading an artist bio
     */
    public class LoadingViewHolder extends RecyclerView.ViewHolder {

        public LoadingViewHolder(View itemView, final Adapter adapter) {
            super(itemView);

            ImageView spinnerView = (ImageView) itemView.findViewById(R.id.loadingDrawable);
            MaterialProgressDrawable spinner = new MaterialProgressDrawable(itemView.getContext(), spinnerView);
            spinner.setColorSchemeColors(Themes.getAccent());
            spinner.updateSizes(MaterialProgressDrawable.LARGE);
            spinner.setAlpha(255);
            spinnerView.setImageDrawable(spinner);
            spinner.start();
        }
    }

    /**
     * ViewHolder class for artist bio
     */
    public class BioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Adapter adapter;

        private CardView cardView;
        private TextView bioText;
        private View itemView;
        private FrameLayout lfmButton;
        private String artistURL;

        public BioViewHolder(View itemView, Adapter adapter, de.umass.lastfm.Artist artist) {
            super(itemView);
            this.itemView = itemView;
            this.adapter = adapter;

            cardView = (CardView) itemView.findViewById(R.id.infoCard);
            bioText = (TextView) itemView.findViewById(R.id.infoText);
            lfmButton = (FrameLayout) itemView.findViewById(R.id.openLFMButton);
            lfmButton.setOnClickListener(this);

            cardView.setCardBackgroundColor(Themes.getBackgroundElevated());
            if (adapter.relatedArtists.length != 0){
                ((GridLayoutManager.LayoutParams) itemView.getLayoutParams()).bottomMargin = 0;
            }

            setData(artist);
        }

        public void setData(de.umass.lastfm.Artist artist) {
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

    /**
     * ViewHolder class for related artists
     */
    public static class SuggestedArtistHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private de.umass.lastfm.Artist reference;

        private Context context;
        private ImageView artwork;
        private TextView artistName;

        public SuggestedArtistHolder(View itemView){
            super(itemView);
            context = itemView.getContext();

            ((CardView) itemView).setCardBackgroundColor(Themes.getBackgroundElevated());
            itemView.setOnClickListener(this);

            artwork = (ImageView) itemView.findViewById(R.id.imageArtwork);
            artistName = (TextView) itemView.findViewById(R.id.textArtistName);
        }

        public void update(de.umass.lastfm.Artist artist){
            reference = artist;

            final String artURL = artist.getImageURL(ImageSize.MEDIUM);
            if (artURL != null && !artURL.equals(""))
                Picasso.with(context).load(artURL).error(R.drawable.art_default).into(artwork);

            artistName.setText(artist.getName());
        }

        @Override
        public void onClick(View v){
            // TODO deal with this
        }

    }

}
