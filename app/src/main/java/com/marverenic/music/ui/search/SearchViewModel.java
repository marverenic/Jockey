package com.marverenic.music.ui.search;

import android.databinding.Bindable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.RxViewModel;
import com.marverenic.music.ui.common.BasicEmptyState;
import com.marverenic.music.ui.common.HeaderSection;
import com.marverenic.music.ui.library.AlbumSection;
import com.marverenic.music.ui.library.ArtistSection;
import com.marverenic.music.ui.library.GenreSection;
import com.marverenic.music.ui.library.PlaylistSection;
import com.marverenic.music.ui.library.SongSection;
import com.marverenic.music.utils.StringUtils;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.Collections;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

import static android.R.attr.numColumns;

public class SearchViewModel extends RxViewModel {

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;

    private BehaviorSubject<String> mQuerySubject;

    private HeterogeneousAdapter mAdapter;
    private int mColumnCount;

    private PlaylistSection mPlaylistSection;
    private SongSection mSongSection;
    private AlbumSection mAlbumSection;
    private ArtistSection mArtistSection;
    private GenreSection mGenreSection;

    public SearchViewModel(BaseFragment fragment, MusicStore musicStore,
                           PlaylistStore playlistStore) {

        super(fragment);
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;

        mQuerySubject = BehaviorSubject.create("");

        createAdapter(fragment);
        observeSearchQuery();
    }

    private void createAdapter(BaseFragment fragment) {
        mPlaylistSection = new PlaylistSection(Collections.emptyList());
        mSongSection = new SongSection(fragment, Collections.emptyList());
        mAlbumSection = new AlbumSection(fragment, Collections.emptyList());
        mArtistSection = new ArtistSection(fragment, Collections.emptyList());
        mGenreSection = new GenreSection(fragment, Collections.emptyList());

        mAdapter = new HeterogeneousAdapter()
                .addSection(new HeaderSection(getString(R.string.header_playlists)))
                .addSection(mPlaylistSection)
                .addSection(new HeaderSection(getString(R.string.header_songs)))
                .addSection(mSongSection)
                .addSection(new HeaderSection(getString(R.string.header_albums)))
                .addSection(mAlbumSection)
                .addSection(new HeaderSection(getString(R.string.header_artists)))
                .addSection(mArtistSection)
                .addSection(new HeaderSection(getString(R.string.header_genres)))
                .addSection(mGenreSection);

        mAdapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                if (StringUtils.isEmpty(getSearchQuery())) {
                    return null;
                } else {
                    return getString(R.string.empty_search);
                }
            }
        });
    }

    private Observable<String> getQueryObservable() {
        return mQuerySubject.distinctUntilChanged().compose(bindToLifecycle());
    }

    private void observeSearchQuery() {
        getQueryObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(query -> mPlaylistStore.searchForPlaylists(query))
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlists -> {
                    mPlaylistSection.setData(playlists);
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e(throwable, "Failed to search for playlists");
                });

        getQueryObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(query -> mMusicStore.searchForSongs(query))
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mSongSection.setData(songs);
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e(throwable, "Failed to search for songs");
                });

        getQueryObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(query -> mMusicStore.searchForAlbums(query))
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(albums -> {
                    mAlbumSection.setData(albums);
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e(throwable, "Failed to search for albums");
                });

        getQueryObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(query -> mMusicStore.searchForArtists(query))
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(artists -> {
                    mArtistSection.setData(artists);
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e(throwable, "Failed to search for artists");
                });

        getQueryObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(query -> mMusicStore.searchForGenres(query))
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(genres -> {
                    mGenreSection.setData(genres);
                    mAdapter.notifyDataSetChanged();
                }, throwable -> {
                    Timber.e(throwable, "Failed to search for genres");
                });
    }

    public void setSearchQuery(String query) {
        mQuerySubject.onNext(query);
    }

    public String getSearchQuery() {
        return mQuerySubject.getValue();
    }

    @Bindable
    public RecyclerView.LayoutManager getLayoutManager() {
        mColumnCount = ViewUtils.getNumberOfGridColumns(getContext(), R.dimen.grid_width);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), mColumnCount);

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (getAdapter().getItemViewType(position) == mAlbumSection.getTypeId()) {
                    return 1;
                } else {
                    return mColumnCount;
                }
            }
        });

        return layoutManager;
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new GridSpacingDecoration(getDimensionPixelSize(R.dimen.grid_margin),
                        numColumns, mAlbumSection.getTypeId()),
                new BackgroundDecoration(R.id.subheader_frame),
                new DividerDecoration(getContext(),
                        R.id.album_view, R.id.subheader_frame, R.id.empty_layout),
        };
    }

}
