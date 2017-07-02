package com.marverenic.music.ui.nowplaying;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.ui.library.GenreSection;
import com.marverenic.music.view.HeterogeneousFastScrollAdapter;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Genre;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class GenreFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;

    private FastScrollRecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private GenreSection mGenreSection;
    private List<Genre> mGenres;
    private Boolean mLibraryHasSongs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mMusicStore.getSongs()
                .compose(bindToLifecycle())
                .map(List::isEmpty)
                .subscribe(
                        isLibraryEmpty -> {
                            mLibraryHasSongs = !isLibraryEmpty;
                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to check if MusicStore has songs");
                        }
                );

        mMusicStore.getGenres()
                .compose(bindToLifecycle())
                .subscribe(
                        genres -> {
                            mGenres = genres;
                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get all genres from MusicStore");
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_library_page, container, false);
        mRecyclerView = (FastScrollRecyclerView) view.findViewById(R.id.library_page_list);
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(getContext(), R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        if (mAdapter == null) {
            setupAdapter();
        } else {
            mRecyclerView.setAdapter(mAdapter);
        }

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mAdapter = null;
        mGenreSection = null;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mGenres == null || mLibraryHasSongs == null) {
            return;
        }

        if (mGenreSection != null) {
            mGenreSection.setData(mGenres);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousFastScrollAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mGenreSection = new GenreSection(this, mGenres);
            mAdapter.addSection(mGenreSection);

            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()) {
                @Override
                public String getEmptyMessage() {
                    if (mLibraryHasSongs) {
                        return getString(R.string.empty_genres);
                    } else {
                        return super.getEmptyMessage();
                    }
                }

                @Override
                public String getEmptyMessageDetail() {
                    if (mLibraryHasSongs) {
                        return getString(R.string.empty_genres_detail);
                    } else {
                        return super.getEmptyMessageDetail();
                    }
                }
            });
        }
    }
}
