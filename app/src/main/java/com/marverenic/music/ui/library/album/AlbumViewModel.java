package com.marverenic.music.ui.library.album;

import android.databinding.Bindable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Album;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.BasicEmptyState;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.Collections;

import timber.log.Timber;

public class AlbumViewModel extends BaseViewModel {

    // TODO refactor the list view models so we don't need to hold on to a fragment
    private BaseFragment mFragment;
    private Album mAlbum;

    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;
    private ShuffleAllSection mShuffleAllSection;

    public AlbumViewModel(BaseFragment fragment, Album album, MusicStore musicStore) {
        super(fragment);
        mFragment = fragment;
        mAlbum = album;

        createAdapter();
        musicStore.getSongs(album)
                .compose(bindToLifecycle())
                .subscribe(songs -> {
                    mSongSection.setData(songs);
                    mShuffleAllSection.setData(songs);
                }, throwable -> {
                    Timber.e(throwable, "Failed to get songs in album");
                });
    }

    private void createAdapter() {
        mSongSection = new SongSection(mFragment, Collections.emptyList());
        mShuffleAllSection = new ShuffleAllSection(mFragment, Collections.emptyList());

        mAdapter = new HeterogeneousAdapter();
        mAdapter.addSection(mShuffleAllSection);
        mAdapter.addSection(mSongSection);

        mAdapter.setEmptyState(new BasicEmptyState() {
            @Override
            public String getMessage() {
                return getString(R.string.empty);
            }
        });
    }

    @Bindable
    public int getHeroImageHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        int maxHeight = screenHeight / 2;

        // prefer a 1:1 aspect ratio
        return Math.min(screenWidth, maxHeight);
    }

    @Bindable
    public GenericRequestBuilder getHeroImage() {
        return Glide.with(getContext()).load(mAlbum.getArtUri()).centerCrop();
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Bindable
    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext(), R.id.empty_layout)
        };
    }

}
