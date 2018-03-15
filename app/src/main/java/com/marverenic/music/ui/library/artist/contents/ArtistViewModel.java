package com.marverenic.music.ui.library.artist.contents;

import android.content.Context;
import android.databinding.Bindable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.util.DisplayMetrics;

import com.android.databinding.library.baseAdapters.BR;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.lastfm.model.Image;
import com.marverenic.music.lastfm.model.LfmArtist;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Artist;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.HeaderSection;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.album.AlbumSection;
import com.marverenic.music.ui.library.song.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import timber.log.Timber;

public class ArtistViewModel extends BaseViewModel {

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;
    private ThemeStore mThemeStore;

    private HeterogeneousAdapter mAdapter;
    private int mColumnCount;
    private int mAlbumColumnCount;
    private int mRelatedColumnCount;

    private LoadingSingleton mLoadingSection;
    private ArtistBioSingleton mBioSection;
    private RelatedArtistSection mRelatedArtistSection;
    private AlbumSection mAlbumSection;
    private ShuffleAllSection mShuffleAllSection;
    private SongSection mSongSection;

    private Artist mReference;
    private LfmArtist mLfmReference;

    public ArtistViewModel(Context context, FragmentManager fragmentManager, Artist artist,
                           PlayerController playerController, MusicStore musicStore,
                           PlaylistStore playlistStore, PreferenceStore prefStore,
                           ThemeStore themeStore, @Nullable OnSongSelectedListener songSelectedListener) {
        super(context);
        mReference = artist;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
        mThemeStore = themeStore;

        createAdapter(fragmentManager, prefStore, songSelectedListener);
    }

    private void createAdapter(FragmentManager fragmentManager, PreferenceStore prefStore,
                               @Nullable OnSongSelectedListener songSelectedListener) {
        mAdapter = new HeterogeneousAdapter();
        mAdapter.setEmptyState(new LibraryEmptyState(getContext(), mMusicStore, mPlaylistStore) {
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

        mBioSection = new ArtistBioSingleton(null, false);
        mRelatedArtistSection = new RelatedArtistSection(mMusicStore, Collections.emptyList());
        mAlbumSection = new AlbumSection(Collections.emptyList(), getContext(),
                mMusicStore, mPlaylistStore, mPlayerController, fragmentManager);
        mShuffleAllSection = new ShuffleAllSection(Collections.emptyList(), prefStore,
                mPlayerController, songSelectedListener);
        mSongSection = new SongSection(Collections.emptyList(), getContext(),
                mPlayerController, mMusicStore, mPlaylistStore,
                fragmentManager, songSelectedListener);

        mAdapter.addSection(mBioSection)
                .addSection(mRelatedArtistSection)
                .addSection(new HeaderSection(getString(R.string.header_albums)))
                .addSection(mAlbumSection)
                .addSection(new HeaderSection(getString(R.string.header_songs)))
                .addSection(mShuffleAllSection)
                .addSection(mSongSection);
    }

    public void setCurrentSong(Song nowPlaying) {
        mSongSection.setCurrentSong(nowPlaying);
    }

    public void setArtistSongs(List<Song> songs) {
        mSongSection.setData(songs);
        mShuffleAllSection.setData(songs);
        mAdapter.notifyDataSetChanged();
    }

    public void setArtistAlbums(List<Album> albums) {
        mAlbumSection.setData(albums);
        mAdapter.notifyDataSetChanged();
    }

    public void setLastFmData(LfmArtist lfmArtist) {
        hideLoadingSpinner();
        mLfmReference = lfmArtist;
        mBioSection.setData(lfmArtist);

        notifyPropertyChanged(BR.heroImage);

        if (lfmArtist == null) {
            return;
        }

        Observable.from(lfmArtist.getSimilarArtists())
                .flatMap(relatedArtist -> {
                    // Filter out artists that aren't in the library
                    return mMusicStore.findArtistByName(relatedArtist.getName())
                            .flatMap(found -> {
                                if (found == null) {
                                    return Observable.empty();
                                } else {
                                    return Observable.just(relatedArtist);
                                }
                            });
                })
                .toList()
                .subscribe(relatedArtists -> {
                    mRelatedArtistSection.setData(relatedArtists);
                    mBioSection.setHasRelatedArtists(!relatedArtists.isEmpty());
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e("Failed to update similar artists");
                });
    }

    public void setLoadingLastFmData(boolean loading) {
        if (loading) {
            showLoadingSpinner();
        } else {
            hideLoadingSpinner();
        }
    }

    private void showLoadingSpinner() {
        if (mLoadingSection == null) {
            int[] colors = {mThemeStore.getPrimaryColor(), mThemeStore.getAccentColor()};
            mLoadingSection = new LoadingSingleton(colors);
            mAdapter.addSection(mLoadingSection, 0);
        }
    }

    private void hideLoadingSpinner() {
        if (mLoadingSection != null) {
            mAdapter.removeSection(0);
            mLoadingSection = null;
        }
    }

    @Bindable
    public int getHeroImageHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // prefer a 3:2 aspect ratio
        int preferredHeight = screenWidth * 2 / 3;
        int maxHeight = screenHeight / 2;

        return Math.min(preferredHeight, maxHeight);
    }

    @Bindable
    public GenericRequestBuilder getHeroImage() {
        if (mLfmReference != null) {
            Image hero = mLfmReference.getImageBySize(Image.Size.MEGA);

            if (hero != null) {
                return Glide.with(getContext())
                        .load(hero.getUrl())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .animate(android.R.anim.fade_in);
            }
        }
        return null;
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public LayoutManager getLayoutManager() {
        mAlbumColumnCount = ViewUtils.getNumberOfGridColumns(getContext(), R.dimen.grid_width);
        mRelatedColumnCount = ViewUtils.getNumberOfGridColumns(getContext(), R.dimen.large_grid_width);
        mColumnCount = mAlbumColumnCount * mRelatedColumnCount;

        // Setup the GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), mColumnCount);
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

        spanSizeLookup.setSpanIndexCacheEnabled(true);
        layoutManager.setSpanSizeLookup(spanSizeLookup);

        return layoutManager;
    }

    @Bindable
    public ItemDecoration[] getItemDecorations() {
        return new ItemDecoration[] {
                new BackgroundDecoration(R.id.loading_frame, R.id.artist_bio_card, R.id.relatedCard),
                new DividerDecoration(getContext(), R.id.artist_bio_card, R.id.album_view,
                        R.id.subheader_frame, R.id.relatedCard, R.id.empty_layout),
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.grid_margin),
                        mAlbumColumnCount, mAlbumSection.getTypeId()),
                new GridSpacingDecoration(
                        (int) getResources().getDimension(R.dimen.card_margin),
                        mRelatedColumnCount, mRelatedArtistSection.getTypeId())
        };
    }

}
