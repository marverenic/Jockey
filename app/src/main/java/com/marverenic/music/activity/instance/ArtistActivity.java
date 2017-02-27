package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseLibraryActivity;
import com.marverenic.music.adapter.AlbumSection;
import com.marverenic.music.adapter.ArtistBioSingleton;
import com.marverenic.music.adapter.HeaderSection;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.LoadingSingleton;
import com.marverenic.music.adapter.RelatedArtistSection;
import com.marverenic.music.adapter.SongSection;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.lastfm.data.store.LastFmStore;
import com.marverenic.music.lastfm.model.Image;
import com.marverenic.music.lastfm.model.LfmArtist;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class ArtistActivity extends BaseLibraryActivity {

    private static final String ARTIST_EXTRA = "ArtistActivity.ARTIST";

    @Inject MusicStore mMusicStore;
    @Inject LastFmStore mLfmStore;
    @Inject PreferenceStore mPrefStore;
    @Inject ThemeStore mThemeStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private int mColumnCount;
    private int mAlbumColumnCount;
    private int mRelatedColumnCount;

    private LoadingSingleton mLoadingSection;
    private ArtistBioSingleton mBioSection;
    private RelatedArtistSection mRelatedArtistSection;
    private AlbumSection mAlbumSection;
    private SongSection mSongSection;

    private Artist mReference;
    private LfmArtist mLfmReference;
    private List<LfmArtist> mRelatedArtists;
    private List<Song> mSongs;
    private List<Album> mAlbums;

    public static Intent newIntent(Context context, Artist artist) {
        Intent intent = new Intent(context, ArtistActivity.class);
        intent.putExtra(ARTIST_EXTRA, artist);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mReference = getIntent().getParcelableExtra(ARTIST_EXTRA);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mReference.getArtistName());

        ImageView artistImage = (ImageView) findViewById(R.id.activity_backdrop);
        artistImage.getLayoutParams().height = calculateHeroHeight();

        mMusicStore.getSongs(mReference)
                .compose(bindToLifecycle())
                .subscribe(
                        songs -> {
                            mSongs = songs;
                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get song contents");
                        });

        mMusicStore.getAlbums(mReference)
                .compose(bindToLifecycle())
                .subscribe(
                        albums -> {
                            mAlbums = albums;

                            // Sort the album list chronologically if all albums have years,
                            // otherwise sort alphabetically
                            if (allEntriesHaveYears()) {
                                Collections.sort(mAlbums, (a1, a2) -> a1.getYear() - a2.getYear());
                            } else {
                                Collections.sort(mAlbums);
                            }

                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get album contents");
                        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        setupAdapter();

        if (Util.canAccessInternet(this, mPrefStore.useMobileNetwork())) {
            setupLoadingAdapter();

            mLfmStore.getArtistInfo(mReference.getArtistName())
                    .compose(bindToLifecycle())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::setLastFmReference,
                            throwable -> {
                                Timber.e(throwable, "Failed to get Last.fm artist info");
                                hideLoadingSpinner();
                            });
        }
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.activity_instance_artwork;
    }

    @Override
    public boolean isToolbarCollapsing() {
        return true;
    }

    private int calculateHeroHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // prefer a 3:2 aspect ratio
        int preferredHeight = screenWidth * 2 / 3;
        int maxHeight = screenHeight / 2;

        return Math.min(preferredHeight, maxHeight);
    }

    private boolean allEntriesHaveYears() {
        for (int i = 0; i < mAlbums.size(); i++) {
            if (mAlbums.get(i).getYear() == 0) {
                return false;
            }
        }
        return true;
    }

    private void setLastFmReference(LfmArtist lfmArtist) {
        if (lfmArtist == null) {
            hideLoadingSpinner();
            return;
        }

        mLfmReference = lfmArtist;
        mRelatedArtists = new ArrayList<>();

        for (LfmArtist relatedArtist : lfmArtist.getSimilarArtists()) {
            mMusicStore.findArtistByName(relatedArtist.getName())
                    .subscribe(
                            found -> {
                                if (found != null) {
                                    mRelatedArtists.add(relatedArtist);
                                    setupAdapter();
                                }
                            },
                            throwable -> {
                                Timber.e(throwable, "Failed to find artist");
                            });
        }
        setupAdapter();

        Image hero = mLfmReference.getImageBySize(Image.Size.MEGA);

        if (hero != null) {
            Glide.with(this)
                    .load(hero.getUrl())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .centerCrop()
                    .animate(android.R.anim.fade_in)
                    .into((ImageView) findViewById(R.id.activity_backdrop));
        }
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

        setupLastFmAdapter();
        setupAlbumAdapter();
        setupSongAdapter();

        mAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        mAlbumColumnCount = ViewUtils.getNumberOfGridColumns(this, R.dimen.grid_width);
        mRelatedColumnCount = ViewUtils.getNumberOfGridColumns(this, R.dimen.large_grid_width);
        mColumnCount = mAlbumColumnCount * mRelatedColumnCount;

        // Setup the GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, mColumnCount);
        GridLayoutManager.SpanSizeLookup spanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Albums & related artists fill one column,
                // all other view types fill the available width
                boolean isArtist = mAlbumSection != null
                        && mAdapter.getItemViewType(position) == mAlbumSection.getTypeId();
                boolean isRelatedArtist = mRelatedArtistSection != null
                        && mAdapter.getItemViewType(position) == mRelatedArtistSection.getTypeId();

                if (isArtist) {
                    return mRelatedColumnCount;
                } else if (isRelatedArtist) {
                    return mAlbumColumnCount;
                } else {
                    return mColumnCount;
                }
            }
        };

        spanSizeLookup.setSpanIndexCacheEnabled(true); // For performance
        layoutManager.setSpanSizeLookup(spanSizeLookup);
        mRecyclerView.setLayoutManager(layoutManager);

        setupListDecorations();
    }

    private void setupListDecorations() {
        mRecyclerView.addItemDecoration(
                new BackgroundDecoration(R.id.loading_frame, R.id.artist_bio_card, R.id.relatedCard));
        mRecyclerView.addItemDecoration(
                new DividerDecoration(this, R.id.artist_bio_card, R.id.album_view, R.id.subheader_frame,
                        R.id.relatedCard, R.id.empty_layout));
    }

    private void setupLoadingAdapter() {
        if (mLoadingSection == null && mLfmReference == null) {
            int[] colors = {mThemeStore.getPrimaryColor(), mThemeStore.getAccentColor()};
            mLoadingSection = new LoadingSingleton(colors);
            mAdapter.addSection(mLoadingSection, 0);
        }
    }

    private void setupLastFmAdapter() {
        if (mLfmReference == null) {
            return;
        }

        hideLoadingSpinner();

        if (mBioSection == null) {
            mBioSection = new ArtistBioSingleton(mLfmReference, !mRelatedArtists.isEmpty());
            mAdapter.addSection(mBioSection, 0);
        }

        if (mRelatedArtistSection == null) {
            mRelatedArtistSection = new RelatedArtistSection(mMusicStore, mRelatedArtists);
            mAdapter.addSection(mRelatedArtistSection, 1);
            mRecyclerView.addItemDecoration(
                    new GridSpacingDecoration(
                            (int) getResources().getDimension(R.dimen.card_margin),
                            mRelatedColumnCount, mRelatedArtistSection.getTypeId()));
        }
    }

    private void hideLoadingSpinner() {
        if (mLoadingSection != null) {
            mAdapter.removeSection(0);
            mLoadingSection = null;
        }
    }

    private void setupSongAdapter() {
        if (mSongs == null) {
            mSongs = Collections.emptyList();
        }

        if (mSongSection == null) {
            mSongSection = new SongSection(this, mSongs);

            mAdapter
                    .addSection(new HeaderSection(getString(R.string.header_songs)))
                    .addSection(mSongSection);
        } else {
            mSongSection.setData(mSongs);
        }
    }

    private void setupAlbumAdapter() {
        if (mAlbums == null) {
            mAlbums = Collections.emptyList();
        }

        if (mAlbumSection == null) {
            mAlbumSection = new AlbumSection(this, mAlbums);
            mAdapter
                    .addSection(new HeaderSection(getString(R.string.header_albums)))
                    .addSection(mAlbumSection);

            mRecyclerView.addItemDecoration(
                    new GridSpacingDecoration(
                            (int) getResources().getDimension(R.dimen.grid_margin),
                            mAlbumColumnCount, mAlbumSection.getTypeId()));
        } else {
            mAlbumSection.setData(mAlbums);
        }
    }
}
