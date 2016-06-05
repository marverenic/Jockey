package com.marverenic.music.activity.instance;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.section.AlbumSection;
import com.marverenic.music.instances.section.ArtistBioSingleton;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.instances.section.LoadingSingleton;
import com.marverenic.music.instances.section.RelatedArtistSection;
import com.marverenic.music.instances.section.SongSection;
import com.marverenic.music.lastfm2.data.store.LastFmStore;
import com.marverenic.music.lastfm2.model.LfmArtist;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ArtistActivity extends BaseActivity {

    public static final String ARTIST_EXTRA = "artist";

    @Inject LastFmStore mLfmStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private LoadingSingleton mLoadingSection;
    private ArtistBioSingleton mBioSection;
    private RelatedArtistSection mRelatedArtistSection;
    private AlbumSection mAlbumSection;
    private SongSection mSongSection;

    private Artist mReference;
    private LfmArtist mLfmReference;
    private List<Song> mSongs;
    private List<Album> mAlbums;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_artwork);

        JockeyApplication.getComponent(this).inject(this);
        mReference = getIntent().getParcelableExtra(ARTIST_EXTRA);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mReference.getArtistName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        mAlbums = Library.getArtistAlbumEntries(mReference);
        mSongs = Library.getArtistSongEntries(mReference);

        // Sort the album list chronologically if all albums have years,
        // otherwise sort alphabetically
        if (allEntriesHaveYears()) {
            Collections.sort(mAlbums, (a1, a2) -> a1.getYear() - a2.getYear());
        } else {
            Collections.sort(mAlbums);
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        setupAdapter();
    }

    private boolean allEntriesHaveYears() {
        for (int i = 0; i < mAlbums.size(); i++) {
            if (mAlbums.get(i).getYear() == 0) {
                return false;
            }
        }
        return true;
    }

    private void setupAdapter() {
        if (mRecyclerView == null) {
            return;
        }

        if (mAdapter == null) {
            setupRecyclerView();

            mAdapter = new HeterogeneousAdapter();
            mAdapter.setEmptyState(new LibraryEmptyState(this) {
                @Override
                public String getEmptyMessage() {
                    if (mReference == null) {
                        return getString(R.string.empty_error_artist);
                    } else {
                        return super.getEmptyMessage();
                    }
                }

                @Override
                public String getEmptyMessageDetail() {
                    if (mReference == null) {
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

            mRecyclerView.setAdapter(mAdapter);
        }

        setupNetworkAdapter();
        setupAlbumAdapter();
        setupSongAdapter();

        mAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        int numColumns = ViewUtils.getNumberOfGridColumns(this);

        // Setup the GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, numColumns);
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Albums & related artists fill one column,
                // all other view types fill the available width
                if (mAdapter.getItemViewType(position) == AlbumSection.ID
                        || mAdapter.getItemViewType(position) == RelatedArtistSection.ID) {
                    return 1;
                } else {
                    return numColumns;
                }
            }
        };

        spanSizeLookup.setSpanIndexCacheEnabled(true); // For performance
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        mRecyclerView.setLayoutManager(layoutManager);

        setupListDecorations(numColumns);
    }

    private void setupListDecorations(int numColumns) {
        mRecyclerView.addItemDecoration(
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.grid_margin),
                        numColumns,
                        AlbumSection.ID));
        mRecyclerView.addItemDecoration(
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.card_margin),
                        numColumns,
                        RelatedArtistSection.ID));
        mRecyclerView.addItemDecoration(
                new BackgroundDecoration(Themes.getBackgroundElevated(), R.id.loadingView,
                        R.id.infoCard, R.id.relatedCard));
        mRecyclerView.addItemDecoration(
                new DividerDecoration(this, R.id.infoCard, R.id.albumInstance, R.id.subheaderFrame,
                        R.id.relatedCard, R.id.empty_layout));
    }

    private void setupNetworkAdapter() {

    }

    private void setupSongAdapter() {
        if (mSongs == null) {
            mSongs = Collections.emptyList();
        }

        if (mSongSection == null) {
            mSongSection = new SongSection(mSongs);
            mAdapter.addSection(mSongSection);
        } else {
            mSongSection.setData(mSongs);
        }
    }

    private void setupAlbumAdapter() {
        if (mAlbums == null) {
            mAlbums = Collections.emptyList();
        }

        if (mAlbumSection == null) {
            mAlbumSection = new AlbumSection(mAlbums);
            mAdapter.addSection(mAlbumSection);
        } else {
            mAlbumSection.setData(mAlbums);
        }
    }
}
