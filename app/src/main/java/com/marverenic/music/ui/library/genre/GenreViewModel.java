package com.marverenic.music.ui.library.genre;

import android.databinding.Bindable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutManager;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Song;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.RxViewModel;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.List;

import timber.log.Timber;

public class GenreViewModel extends RxViewModel {

    // TODO remove this field after refactoring LibraryEmptyState
    private BaseFragment mFragment;

    private HeterogeneousAdapter mAdapter;
    private ShuffleAllSection mShuffleAllSection;
    private SongSection mSongSection;

    public GenreViewModel(BaseFragment fragment, MusicStore musicStore, Genre genre) {
        super(fragment);
        mFragment = fragment;
        createAdapter();

        musicStore.getSongs(genre)
                .compose(bindToLifecycle())
                .subscribe(this::updateAdapter, throwable -> {
                            Timber.e(throwable, "Failed to get song contents");
                        });
    }

    private void createAdapter() {
        mAdapter = new HeterogeneousAdapter();
        mAdapter.setEmptyState(new LibraryEmptyState(mFragment.getActivity()) {
            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });
    }

    private void updateAdapter(List<Song> genreContents) {
        if (mSongSection != null && mShuffleAllSection != null) {
            mSongSection.setData(genreContents);
            mShuffleAllSection.setData(genreContents);
            mAdapter.notifyDataSetChanged();
        } else {
            mSongSection = new SongSection(mFragment, genreContents);
            mShuffleAllSection = new ShuffleAllSection(mFragment, genreContents);
            mAdapter.addSection(mShuffleAllSection);
            mAdapter.addSection(mSongSection);
        }
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Bindable
    public ItemDecoration[] getItemDecorations() {
        return new ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext(), R.id.empty_layout)
        };
    }

}
