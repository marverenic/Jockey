package com.marverenic.music.activity.instance;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.AlbumSection;
import com.marverenic.music.instances.section.ArtistBioSingleton;
import com.marverenic.music.instances.section.HeaderSection;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.LoadingSingleton;
import com.marverenic.music.instances.section.RelatedArtistSection;
import com.marverenic.music.instances.section.SongSection;
import com.marverenic.music.lastfm.ImageList;
import com.marverenic.music.lastfm.LArtist;
import com.marverenic.music.lastfm.Query;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class ArtistActivity extends BaseActivity {

    public static final String ARTIST_EXTRA = "artist";

    private HeterogeneousAdapter adapter;
    private Worker lfmLoader;
    private Artist reference;
    private LArtist lfmReference;
    private List<LArtist> relatedArtists;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_artwork);

        reference = getIntent().getParcelableExtra(ARTIST_EXTRA);
        List<Album> albums = Library.getArtistAlbumEntries(reference);
        List<Song> songs = Library.getArtistSongEntries(reference);
        relatedArtists = new ArrayList<>();

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

        adapter = new HeterogeneousAdapter();
        adapter
                .addSection(new LoadingSingleton(null))
                .addSection(new RelatedArtistSection(relatedArtists))
                .addSection(new HeaderSection(getString(R.string.header_albums), AlbumSection.ID))
                .addSection(new AlbumSection(albums))
                .addSection(new HeaderSection(getString(R.string.header_songs), SongSection.ID))
                .addSection(new SongSection(songs));
        adapter.setEmptyState(new LibraryEmptyState(this) {
            @Override
            public String getEmptyMessage() {
                if (reference == null) {
                    return getString(R.string.empty_error_artist);
                } else {
                    return super.getEmptyMessage();
                }
            }

            @Override
            public String getEmptyMessageDetail() {
                if (reference == null) {
                    return "";
                } else {
                    return super.getEmptyMessageDetail();
                }
            }

            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);

        final int numColumns = ViewUtils.getNumberOfGridColumns(this);

        // Setup the GridLayoutManager
        final GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Albums & related artists fill one column,
                // all other view types fill the available width
                if (adapter.getItemViewType(position) == AlbumSection.ID
                        || adapter.getItemViewType(position) == RelatedArtistSection.ID) {
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
                        AlbumSection.ID));
        list.addItemDecoration(
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.card_margin),
                        numColumns,
                        RelatedArtistSection.ID));
        list.addItemDecoration(
                new BackgroundDecoration(Themes.getBackgroundElevated(), R.id.loadingView,
                        R.id.infoCard, R.id.relatedCard));
        list.addItemDecoration(
                new DividerDecoration(this, R.id.infoCard, R.id.albumInstance, R.id.subheaderFrame,
                        R.id.relatedCard, R.id.empty_layout));

        lfmLoader = new Worker();
        lfmLoader.execute(reference);
    }

    @Override
    protected void onStop() {
        super.onStop();
        lfmLoader.cancel(true);
    }

    private class Worker extends AsyncTask<Artist, Void, Void> {

        @Override
        protected Void doInBackground(Artist... params) {
            try {
                lfmReference = Query.getArtist(ArtistActivity.this, params[0]);
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
                lfmReference = null;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            adapter.removeSectionById(LoadingSingleton.ID);
            if (lfmReference == null) {
                adapter.notifyItemRemoved(0);
            } else {
                // Only show related artists if they exist in the library
                for (LArtist a : lfmReference.getRelatedArtists()) {
                    Artist localReference = Library.findArtistByName(a.getName());
                    if (localReference != null) {
                        relatedArtists.add(a);
                    }
                }

                adapter.addSection(
                        new ArtistBioSingleton(lfmReference, !relatedArtists.isEmpty()), 0);

                adapter.notifyItemChanged(0);
                adapter.notifyItemRangeInserted(1, relatedArtists.size());

                // Set header image
                String url = lfmReference.getImageURL(ImageList.SIZE_MEGA);

                if (url.trim().length() != 0) {
                    Glide.with(ArtistActivity.this).load(url)
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .centerCrop()
                            .animate(android.R.anim.fade_in)
                            .into((ImageView) findViewById(R.id.backdrop));
                }
            }
        }
    }
}
