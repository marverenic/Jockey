package com.marverenic.music.activity.instance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.crashlytics.android.Crashlytics;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.AlbumViewHolder;
import com.marverenic.music.instances.viewholder.HeaderViewHolder;
import com.marverenic.music.instances.viewholder.SongViewHolder;
import com.marverenic.music.lastfm.ImageList;
import com.marverenic.music.lastfm.LArtist;
import com.marverenic.music.lastfm.Query;
import com.marverenic.music.lastfm.Tag;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.MaterialProgressDrawable;
import com.marverenic.music.view.ViewUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.ParserConfigurationException;

public class ArtistActivity extends BaseActivity {

    public static final String ARTIST_EXTRA = "artist";
    private Artist reference;
    private ArrayList<Album> albums;
    private ArrayList<Song> songs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_artwork);

        reference = getIntent().getParcelableExtra(ARTIST_EXTRA);
        albums = Library.getArtistAlbumEntries(reference);
        songs = Library.getArtistSongEntries(reference);

        // Sort the album list chronologically if all albums have years,
        // otherwise sort alphabetically
        boolean allEntriesHaveYears = true;
        int i = 0;
        while (i < albums.size() && allEntriesHaveYears) {
            if (albums.get(i).getYear() == 0) allEntriesHaveYears = false;
            i++;
        }

        if (allEntriesHaveYears) {
            Collections.sort(albums, new Comparator<Album>() {
                @Override
                public int compare(Album a1, Album a2) {
                    return a1.getYear() - a2.getYear();
                }
            });
        } else {
            Collections.sort(albums);
        }

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(reference.getArtistName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        final Adapter adapter = new Adapter();
        list.setAdapter(adapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(this);

        // Setup the GridLayoutManager
        final GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Albums & related artists fill one column,
                // all other view types fill the available width
                if (adapter.getItemViewType(position) == Adapter.ALBUM_INSTANCE) {
                    return 1;
                } else if (adapter.getItemViewType(position) == Adapter.RELATED_ARTIST) {
                    return 1;
                } else {
                    return numColumns;
                }
            }
        };

        spanSizeLookup.setSpanIndexCacheEnabled(true); // For performance

        // Attach the GridLayoutManager
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        list.setLayoutManager(layoutManager);

        // Add decorations
        list.addItemDecoration(
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.grid_margin),
                        numColumns,
                        Adapter.ALBUM_INSTANCE));
        list.addItemDecoration(
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.card_margin),
                        numColumns,
                        Adapter.RELATED_ARTIST));
        list.addItemDecoration(
                new BackgroundDecoration(Themes.getBackgroundElevated(), R.id.loadingView,
                        R.id.infoCard, R.id.relatedCard));
        list.addItemDecoration(
                new DividerDecoration(this, R.id.infoCard, R.id.albumInstance, R.id.subheaderFrame,
                        R.id.relatedCard));
    }

    /**
     * Adapter class for the Artist RecyclerView
     */
    public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private boolean hasBio = true;
        private ArrayList<LArtist> relatedArtists = new ArrayList<>();
        private ArrayList<Artist> localRelatedArtists = new ArrayList<>();
        private LArtist artist;

        public static final int LOADING_BIO_VIEW = 0;
        public static final int BIO_VIEW = 1;
        public static final int RELATED_ARTIST = 2;
        public static final int HEADER_VIEW = 3;
        public static final int ALBUM_INSTANCE = 4;
        public static final int SONG_INSTANCE = 5;

        public Adapter() {
            // Fetch the Artist Bio
            new AsyncTask<Artist, Void, Void>() {
                @Override
                protected Void doInBackground(Artist... params) {
                    try {
                        artist = Query.getArtist(ArtistActivity.this, params[0]);
                    } catch (IOException | ParserConfigurationException | SAXException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                        artist = null;
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                    updateLastFmData();
                }
            }.execute(reference);
        }

        private void updateLastFmData() {
            if (artist == null) {
                hasBio = false;
                notifyDataSetChanged();
            } else {
                // Only show related artists if they exist in the library
                for (LArtist a : artist.getRelatedArtists()) {
                    Artist localReference = Library.findArtistByName(a.getName());
                    if (localReference != null) {
                        relatedArtists.add(a);
                        localRelatedArtists.add(localReference);
                    }
                }

                notifyItemChanged(0);
                notifyItemRangeInserted((hasBio) ? 1 : 0, relatedArtists.size());

                // Set header image
                String url = artist.getImageURL(ImageList.SIZE_MEGA);

                if (url.trim().length() != 0) {
                    Glide.with(ArtistActivity.this).load(url)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .centerCrop()
                            .animate(android.R.anim.fade_in)
                            .into((ImageView) findViewById(R.id.backdrop));
                }
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case LOADING_BIO_VIEW:
                    return new LoadingViewHolder(LayoutInflater.from(ArtistActivity.this)
                            .inflate(R.layout.instance_loading, parent, false));
                case BIO_VIEW:
                    return new BioViewHolder(LayoutInflater.from(ArtistActivity.this)
                            .inflate(R.layout.instance_artist_bio, parent, false), this, artist);
                case RELATED_ARTIST:
                    return new SuggestedArtistHolder(LayoutInflater.from(ArtistActivity.this)
                            .inflate(R.layout.instance_artist_suggested, parent, false));
                case HEADER_VIEW:
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.subheader, parent, false));
                case ALBUM_INSTANCE:
                    return new AlbumViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.instance_album, parent, false));
                case SONG_INSTANCE:
                    return new SongViewHolder(
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.instance_song, parent, false),
                            songs);
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                // Don't update anything in the bio view. Since there's only one instance, the data
                // will never become invalid
                case LOADING_BIO_VIEW:
                    ((LoadingViewHolder) holder).update();
                    break;
                case RELATED_ARTIST:
                    ((SuggestedArtistHolder) holder).update(
                            relatedArtists.get(position - (hasBio ? 1 : 0)),
                            localRelatedArtists.get(position - (hasBio ? 1 : 0)));
                    break;
                case HEADER_VIEW:
                    ((HeaderViewHolder) holder).update(
                            (position == (hasBio ? relatedArtists.size() + 1 : 0))
                                    ? getResources().getString(R.string.header_albums)
                                    : getResources().getString(R.string.header_songs));
                    break;
                case ALBUM_INSTANCE:
                    ((AlbumViewHolder) holder).update(
                            albums.get(position - (hasBio ? 2 : 1) - relatedArtists.size()));
                    break;
                case SONG_INSTANCE:
                    int songIndex =
                            position - albums.size() - (hasBio ? 3 : 2) - relatedArtists.size();
                    ((SongViewHolder) holder).update(songs.get(songIndex), songIndex);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return (hasBio ? 1 : 0) + relatedArtists.size() + 2 + albums.size() + songs.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (hasBio && position == 0) {
                return ((artist == null) ? LOADING_BIO_VIEW : BIO_VIEW);
            } else if (position < relatedArtists.size() + (hasBio ? 1 : 0)) {
                return RELATED_ARTIST;
            } else if (position == relatedArtists.size() + (hasBio ? 1 : 0)) {
                return HEADER_VIEW;
            } else if (position <= albums.size() + relatedArtists.size() + (hasBio ? 1 : 0)) {
                return ALBUM_INSTANCE;
            } else if (position == albums.size() + relatedArtists.size() + (hasBio ? 2 : 1)) {
                return HEADER_VIEW;
            }
            return SONG_INSTANCE;
        }
    }

    /**
     * Temporary ViewHolder for loading an artist bio
     */
    public static class LoadingViewHolder extends RecyclerView.ViewHolder {

        private MaterialProgressDrawable spinner;

        public LoadingViewHolder(View itemView) {
            super(itemView);

            ImageView spinnerView = (ImageView) itemView.findViewById(R.id.loadingDrawable);
            spinner = new MaterialProgressDrawable(itemView.getContext(), spinnerView);
            spinner.setColorSchemeColors(Themes.getAccent(), Themes.getPrimary());
            spinner.updateSizes(MaterialProgressDrawable.LARGE);
            spinner.setAlpha(255);
            spinnerView.setImageDrawable(spinner);
            spinner.start();
        }

        public void update() {
            spinner.stop();
            spinner.start();
        }
    }

    /**
     * ViewHolder class for artist bio
     */
    public static class BioViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private CardView cardView;
        private TextView bioText;
        private FrameLayout lfmButton;
        private String artistURL;

        public BioViewHolder(View itemView, Adapter adapter, LArtist artist) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.infoCard);
            bioText = (TextView) itemView.findViewById(R.id.infoText);
            lfmButton = (FrameLayout) itemView.findViewById(R.id.openLFMButton);
            lfmButton.setOnClickListener(this);

            cardView.setCardBackgroundColor(Themes.getBackgroundElevated());
            if (adapter.relatedArtists.size() != 0) {
                ((GridLayoutManager.LayoutParams) itemView.getLayoutParams()).bottomMargin = 0;
            }

            setData(artist);
        }

        @SuppressLint("SetTextI18n")
        public void setData(LArtist artist) {
            Tag[] tags = artist.getTags();
            String[] tagNames = new String[tags.length];

            for (int i = 0; i < tags.length; i++) {
                tagNames[i] = tags[i].getName();
            }

            String tagList = ""; // A list of tags to display in the card

            if (tags.length > 0) {
                // Capitalize the first letter of the tag
                tagList = tagNames[0].substring(0, 1).toUpperCase() + tagNames[0].substring(1);
                // Add up to 4 more tags (separated by commas)
                int tagCount = (tags.length < 5) ? tags.length : 5;
                for (int i = 1; i < tagCount; i++) {
                    tagList += ", " + tagNames[i].substring(0, 1).toUpperCase()
                            + tagNames[i].substring(1);
                }
            }

            String summary = artist.getBio().getSummary();
            if (summary.length() > 0) {
                // Trim the "read more" attribution since there's already a button
                // linking to Last.fm
                summary = summary.substring(0, summary.lastIndexOf("<a href=\""));
            }

            bioText.setText(tagList
                    + ((tagList.trim().length() > 0 && summary.trim().length() > 0) ? "\n\n" : "")
                    + summary);

            artistURL = artist.getUrl();
        }

        @Override
        public void onClick(View v) {
            if (v.equals(lfmButton)) {
                Intent openLFMIntent = new Intent(Intent.ACTION_VIEW);
                if (artistURL == null) {
                    openLFMIntent.setData(Uri.parse("http://www.last.fm/home"));
                } else {
                    openLFMIntent.setData(Uri.parse(artistURL));
                }
                itemView.getContext().startActivity(openLFMIntent);
            }
        }
    }

    /**
     * ViewHolder class for related artists
     */
    public static class SuggestedArtistHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private Artist localReference;

        private Context context;
        private ImageView artwork;
        private TextView artistName;

        public SuggestedArtistHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();

            ((CardView) itemView).setCardBackgroundColor(Themes.getBackgroundElevated());
            itemView.setOnClickListener(this);

            artwork = (ImageView) itemView.findViewById(R.id.imageArtwork);
            artistName = (TextView) itemView.findViewById(R.id.textArtistName);
        }

        public void update(LArtist artist, Artist localArtist) {
            localReference = localArtist;

            final String artURL = artist.getImageURL(ImageList.SIZE_MEDIUM);

            Glide.with(context)
                    .load(artURL)
                    .asBitmap()
                    .error(R.drawable.art_default)
                    .into(new BitmapImageViewTarget(artwork) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(
                                            context.getResources(),
                                            resource);
                            circularBitmapDrawable.setCircular(true);
                            artwork.setImageDrawable(circularBitmapDrawable);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(
                                            context.getResources(),
                                            ((BitmapDrawable) errorDrawable).getBitmap());
                            circularBitmapDrawable.setCircular(true);
                            artwork.setImageDrawable(circularBitmapDrawable);
                        }
                    });

            artistName.setText(artist.getName());
        }

        @Override
        public void onClick(View v) {
            if (localReference != null) {
                Navigate.to(context, ArtistActivity.class, ARTIST_EXTRA, localReference);
            }
        }

    }

}
